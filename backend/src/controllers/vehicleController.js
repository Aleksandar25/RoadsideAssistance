const Vehicle = require('../models/Vehicle');

// @route   GET /api/vehicles
// @desc    Списък с автомобилите на текущия потребител
// @access  Private (CLIENT)
const getMyVehicles = async (req, res, next) => {
  try {
    const vehicles = await Vehicle.find({ owner: req.user._id }).sort({ createdAt: -1 });
    res.status(200).json({ count: vehicles.length, vehicles });
  } catch (error) {
    next(error);
  }
};

// @route   POST /api/vehicles
// @desc    Добавяне на нов автомобил от текущия потребител
// @access  Private (CLIENT)
const createVehicle = async (req, res, next) => {
  try {
    const { make, model, licensePlate, year } = req.body;

    if (!make || !model || !licensePlate || !year) {
      return res.status(400).json({ message: 'Моля, попълнете всички полета' });
    }

    const vehicle = await Vehicle.create({
      owner: req.user._id,
      make,
      model,
      licensePlate,
      year,
    });

    res.status(201).json({ vehicle });
  } catch (error) {
    next(error);
  }
};

module.exports = { getMyVehicles, createVehicle };
