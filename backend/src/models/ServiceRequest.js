const mongoose = require('mongoose');

const SERVICE_TYPES = [
  'TOWING',
  'JUMP_START',
  'TIRE_CHANGE',
  'FUEL_DELIVERY',
  'MECHANICAL_FAILURE',
  'OTHER',
];
const STATUSES = ['PENDING', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'];

// Един запис в историята на статусите - вграден (embedded) документ, не отделна колекция
const statusHistorySchema = new mongoose.Schema(
  {
    status: { type: String, enum: STATUSES, required: true },
    changedAt: { type: Date, default: Date.now },
  },
  { _id: false }
);

const serviceRequestSchema = new mongoose.Schema(
  {
    client: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    // Попълва се, когато доставчик приеме заявката
    provider: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      default: null,
    },
    // По избор - кой автомобил на клиента е свързан със заявката
    vehicle: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'Vehicle',
      default: null,
    },
    serviceType: {
      type: String,
      enum: SERVICE_TYPES,
      required: [true, 'Типът услуга е задължителен'],
    },
    // GeoJSON точка с координатите, откъдето е подадена заявката
    location: {
      type: {
        type: String,
        enum: ['Point'],
        default: 'Point',
      },
      coordinates: {
        // [longitude, latitude]
        type: [Number],
        required: true,
      },
    },
    // Човекочетим адрес (по избор), получен чрез Geocoding API от клиента
    address: {
      type: String,
      trim: true,
      default: '',
    },
    description: {
      type: String,
      trim: true,
      default: '',
    },
    status: {
      type: String,
      enum: STATUSES,
      default: 'PENDING',
    },
    // Хронология на статусите - попълва се автоматично от pre('save') хука по-долу
    statusHistory: {
      type: [statusHistorySchema],
      default: [],
    },
  },
  { timestamps: true }
);

serviceRequestSchema.index({ location: '2dsphere' });

// Записва запис в историята при създаване и при всяка следваща промяна на status,
// без контролерите да трябва да го правят ръчно на всяко място, където сменят статус.
serviceRequestSchema.pre('save', function appendStatusHistory() {
  if (this.isNew || this.isModified('status')) {
    this.statusHistory.push({ status: this.status, changedAt: new Date() });
  }
});

module.exports = mongoose.model('ServiceRequest', serviceRequestSchema);
module.exports.SERVICE_TYPES = SERVICE_TYPES;
module.exports.STATUSES = STATUSES;
