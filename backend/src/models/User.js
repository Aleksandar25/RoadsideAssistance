const mongoose = require('mongoose');
const bcrypt = require('bcryptjs');

const userSchema = new mongoose.Schema(
  {
    name: {
      type: String,
      required: [true, 'Името е задължително'],
      trim: true,
    },
    email: {
      type: String,
      required: [true, 'Имейлът е задължителен'],
      unique: true,
      lowercase: true,
      trim: true,
      match: [/^\S+@\S+\.\S+$/, 'Невалиден формат на имейл'],
    },
    password: {
      type: String,
      required: [true, 'Паролата е задължителна'],
      minlength: 6,
      select: false, // никога не се връща по подразбиране в заявки
    },
    phone: {
      type: String,
      required: [true, 'Телефонният номер е задължителен'],
      trim: true,
    },
    role: {
      type: String,
      enum: ['CLIENT', 'PROVIDER'],
      default: 'CLIENT',
      required: true,
    },
    // Текуща GPS локация на потребителя.
    // За PROVIDER се използва при търсене на заявки наблизо (geo query).
    location: {
      type: {
        type: String,
        enum: ['Point'],
        default: 'Point',
      },
      coordinates: {
        // [longitude, latitude] - реда е важен за GeoJSON
        type: [Number],
        default: [0, 0],
      },
    },
    // Само за PROVIDER - дали в момента приема нови заявки
    isAvailable: {
      type: Boolean,
      default: true,
    },
  },
  { timestamps: true }
);

// 2dsphere индекс, нужен за геопространствени заявки ($near, $geoWithin)
userSchema.index({ location: '2dsphere' });

// Хеширане на паролата преди запис, само ако е променена
// Забележка: при async pre-хук Mongoose очаква връщане на Promise, а не извикване на next()
userSchema.pre('save', async function hashPassword() {
  if (!this.isModified('password')) return;

  const salt = await bcrypt.genSalt(10);
  this.password = await bcrypt.hash(this.password, salt);
});

// Инстанс метод за сравняване на паролата при login
userSchema.methods.comparePassword = function comparePassword(candidatePassword) {
  return bcrypt.compare(candidatePassword, this.password);
};

module.exports = mongoose.model('User', userSchema);
