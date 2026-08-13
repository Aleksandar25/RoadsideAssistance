const express = require('express');
const {
  createRequest,
  getNearbyRequests,
  updateRequestStatus,
  getMyRequests,
  getRequestById,
} = require('../controllers/requestController');
const { protect, authorize } = require('../middleware/auth');

const router = express.Router();

// Всички route-ове изискват логнат потребител
router.use(protect);

router.post('/', authorize('CLIENT'), createRequest);
router.get('/nearby', authorize('PROVIDER'), getNearbyRequests);
router.get('/my', getMyRequests);
router.patch('/:id/status', updateRequestStatus);
router.get('/:id', getRequestById);

module.exports = router;
