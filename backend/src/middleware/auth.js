const jwt = require('jsonwebtoken');
const User = require('../models/User');

// Проверява JWT от Authorization: Bearer <token> хедъра
// и прикачва пълния (актуален) потребителски документ към req.user
const protect = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ message: 'Няма достъп - липсва токен' });
    }

    const token = authHeader.split(' ')[1];
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const user = await User.findById(decoded.id);
    if (!user) {
      return res.status(401).json({ message: 'Потребителят вече не съществува' });
    }

    req.user = user;
    next();
  } catch (error) {
    return res.status(401).json({ message: 'Невалиден или изтекъл токен' });
  }
};

// Ограничава достъпа само до потребители с определена(и) роля(и)
// Пример: authorize('PROVIDER') или authorize('CLIENT', 'PROVIDER')
const authorize = (...roles) => {
  return (req, res, next) => {
    if (!req.user || !roles.includes(req.user.role)) {
      return res.status(403).json({ message: 'Нямате права за тази операция' });
    }
    next();
  };
};

module.exports = { protect, authorize };
