const ServiceRequest = require('../models/ServiceRequest');
const { SERVICE_TYPES } = require('../models/ServiceRequest');
const User = require('../models/User');
const Vehicle = require('../models/Vehicle');

// Използва се навсякъде, където връщаме заявка(и) в отговор, за да имат
// всички /api/requests ендпойнти еднаква, консистентна форма на отговора
const populateRequest = (query) =>
  query
    .populate('client', 'name phone')
    .populate('provider', 'name phone')
    .populate('vehicle', 'make model licensePlate year');

// @route   POST /api/requests
// @desc    Клиент подава нова заявка за пътна помощ със своите GPS координати
// @access  Private (CLIENT)
const createRequest = async (req, res, next) => {
  try {
    const { serviceType, lat, lng, address, description, vehicleId } = req.body;

    if (!serviceType || !SERVICE_TYPES.includes(serviceType)) {
      return res.status(400).json({
        message: `Невалиден тип услуга. Позволени стойности: ${SERVICE_TYPES.join(', ')}`,
      });
    }

    if (lat === undefined || lng === undefined) {
      return res.status(400).json({ message: 'Изисква се текуща локация (lat, lng)' });
    }

    let vehicle = null;
    if (vehicleId) {
      vehicle = await Vehicle.findOne({ _id: vehicleId, owner: req.user._id });
      if (!vehicle) {
        return res.status(400).json({ message: 'Автомобилът не е намерен' });
      }
    }

    const request = await ServiceRequest.create({
      client: req.user._id,
      vehicle: vehicle ? vehicle._id : null,
      serviceType,
      location: {
        type: 'Point',
        coordinates: [Number(lng), Number(lat)], // GeoJSON: [longitude, latitude]
      },
      address,
      description,
    });

    // Populate-ваме за консистентна форма на отговора спрямо другите /requests route-ове.
    // Document.populate() (за разлика от Query.populate()) не се чейнва флуентно -
    // всяко извикване трябва да се изчака отделно (виж бележката в updateRequestStatus)
    await request.populate('client', 'name phone');
    await request.populate('provider', 'name phone');
    await request.populate('vehicle', 'make model licensePlate year');

    res.status(201).json({ request });
  } catch (error) {
    next(error);
  }
};

// @route   GET /api/requests/nearby?lat=..&lng=..&maxDistance=..
// @desc    Доставчик вижда чакащите (PENDING) заявки в радиус около текущата си локация
// @access  Private (PROVIDER)
const getNearbyRequests = async (req, res, next) => {
  try {
    const { lat, lng, maxDistance } = req.query;

    if (lat === undefined || lng === undefined) {
      return res.status(400).json({ message: 'Изисква се текуща локация (lat, lng)' });
    }

    // По подразбиране радиус от 15 км, ако не е подаден
    const radiusInMeters = maxDistance ? Number(maxDistance) : 15000;

    const requests = await populateRequest(
      ServiceRequest.find({
        status: 'PENDING',
        location: {
          $near: {
            $geometry: {
              type: 'Point',
              coordinates: [Number(lng), Number(lat)],
            },
            $maxDistance: radiusInMeters,
          },
        },
      })
    );

    // Обновяваме и последната позната локация на доставчика, за бъдещи заявки
    await User.findByIdAndUpdate(req.user._id, {
      location: { type: 'Point', coordinates: [Number(lng), Number(lat)] },
    });

    res.status(200).json({ count: requests.length, requests });
  } catch (error) {
    next(error);
  }
};

// Позволени преходи между статуси, по роля
const ALLOWED_TRANSITIONS = {
  ACCEPTED: { from: 'PENDING', by: 'PROVIDER', assignsProvider: true },
  IN_PROGRESS: { from: 'ACCEPTED', by: 'PROVIDER', requiresAssignedProvider: true },
  COMPLETED: { from: 'IN_PROGRESS', by: 'PROVIDER', requiresAssignedProvider: true },
  CANCELLED: { from: ['PENDING', 'ACCEPTED'], by: ['CLIENT', 'PROVIDER'] },
};

// @route   PATCH /api/requests/:id/status
// @desc    Приемане/отказване/напредък/приключване на заявка
// @access  Private (CLIENT, PROVIDER)
const updateRequestStatus = async (req, res, next) => {
  try {
    const { status } = req.body;
    const transition = ALLOWED_TRANSITIONS[status];

    if (!transition) {
      return res.status(400).json({ message: 'Невалиден статус' });
    }

    const request = await ServiceRequest.findById(req.params.id);
    if (!request) {
      return res.status(404).json({ message: 'Заявката не е намерена' });
    }

    const allowedRoles = Array.isArray(transition.by) ? transition.by : [transition.by];
    if (!allowedRoles.includes(req.user.role)) {
      return res.status(403).json({ message: 'Нямате права за тази промяна на статус' });
    }

    const allowedFromStatuses = Array.isArray(transition.from) ? transition.from : [transition.from];
    if (!allowedFromStatuses.includes(request.status)) {
      return res.status(400).json({
        message: `Не може да се премине от '${request.status}' към '${status}'`,
      });
    }

    // Само собственикът клиент или вече назначеният доставчик могат да променят заявката
    const isOwner = request.client.toString() === req.user._id.toString();
    const isAssignedProvider =
      request.provider && request.provider.toString() === req.user._id.toString();

    if (req.user.role === 'CLIENT' && !isOwner) {
      return res.status(403).json({ message: 'Това не е Ваша заявка' });
    }

    if (transition.requiresAssignedProvider && !isAssignedProvider) {
      return res.status(403).json({ message: 'Не сте назначеният доставчик за тази заявка' });
    }

    if (transition.assignsProvider) {
      request.provider = req.user._id;
    }

    request.status = status;
    await request.save();

    // Populate-ваме за консистентна форма на отговора спрямо другите /requests route-ове.
    // Забележка: на Document инстанция (за разлика от Query) populate() се чейнва
    // само през последователни await-и, не флуентно едно след друго
    await request.populate('client', 'name phone');
    await request.populate('provider', 'name phone');
    await request.populate('vehicle', 'make model licensePlate year');

    res.status(200).json({ request });
  } catch (error) {
    next(error);
  }
};

// @route   GET /api/requests/my
// @desc    Списък със заявките на текущия потребител (клиент или доставчик)
// @access  Private
const getMyRequests = async (req, res, next) => {
  try {
    const filter =
      req.user.role === 'CLIENT' ? { client: req.user._id } : { provider: req.user._id };

    const requests = await populateRequest(ServiceRequest.find(filter)).sort({ createdAt: -1 });

    res.status(200).json({ count: requests.length, requests });
  } catch (error) {
    next(error);
  }
};

// @route   GET /api/requests/:id
// @desc    Детайли за конкретна заявка (за преглед на статус в реално време)
// @access  Private
const getRequestById = async (req, res, next) => {
  try {
    const request = await populateRequest(ServiceRequest.findById(req.params.id));

    if (!request) {
      return res.status(404).json({ message: 'Заявката не е намерена' });
    }

    res.status(200).json({ request });
  } catch (error) {
    next(error);
  }
};

module.exports = {
  createRequest,
  getNearbyRequests,
  updateRequestStatus,
  getMyRequests,
  getRequestById,
};
