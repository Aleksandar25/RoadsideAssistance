const express = require('express');
const { getMyVehicles, createVehicle } = require('../controllers/vehicleController');
const { protect, authorize } = require('../middleware/auth');

const router = express.Router();

// Автомобилите принадлежат само на CLIENT потребители - те подават заявки за пътна помощ
router.use(protect, authorize('CLIENT'));

router.get('/', getMyVehicles);
router.post('/', createVehicle);

module.exports = router;
