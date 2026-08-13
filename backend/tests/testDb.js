const mongoose = require('mongoose');
const { MongoMemoryServer } = require('mongodb-memory-server');

// Тестовете зареждат app.js директно (без server.js), затова .env никога не се
// чете от dotenv - задаваме тестов JWT_SECRET ръчно, независимо от реалния .env
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-jwt-secret-not-for-production';
process.env.JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d';

// Тестовете използват in-memory MongoDB инстанция, за да не пипат
// реалната dev база (roadside_assistance) и да не зависят от външна услуга.
let mongod;

const connect = async () => {
  mongod = await MongoMemoryServer.create();
  await mongoose.connect(mongod.getUri());
};

const clearDatabase = async () => {
  const collections = mongoose.connection.collections;
  for (const key in collections) {
    await collections[key].deleteMany({});
  }
};

const closeDatabase = async () => {
  await mongoose.connection.dropDatabase();
  await mongoose.connection.close();
  await mongod.stop();
};

module.exports = { connect, clearDatabase, closeDatabase };
