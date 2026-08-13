const mongoose = require('mongoose');

const vehicleSchema = new mongoose.Schema(
  {
    owner: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'User',
      required: true,
    },
    make: {
      type: String,
      required: [true, 'Марката е задължителна'],
      trim: true,
    },
    model: {
      type: String,
      required: [true, 'Моделът е задължителен'],
      trim: true,
    },
    licensePlate: {
      type: String,
      required: [true, 'Регистрационният номер е задължителен'],
      trim: true,
      uppercase: true,
    },
    year: {
      type: Number,
      required: [true, 'Годината е задължителна'],
      min: 1950,
      max: new Date().getFullYear() + 1,
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model('Vehicle', vehicleSchema);
