// Централизиран error handler - хваща грешки, хвърлени/подадени с next(error)
// или (в Express 5) излезли от отхвърлен Promise в async route handler
const errorHandler = (err, req, res, next) => {
  console.error(err.stack);

  // Невалиден ObjectId, подаден в URL параметър (напр. /api/requests/123)
  if (err.name === 'CastError') {
    return res.status(400).json({ message: 'Невалиден идентификатор' });
  }

  // Грешки от Mongoose валидация на схема
  if (err.name === 'ValidationError') {
    const messages = Object.values(err.errors).map((e) => e.message);
    return res.status(400).json({ message: messages.join(', ') });
  }

  // Дублиран уникален ключ (напр. email вече съществува)
  if (err.code === 11000) {
    return res.status(409).json({ message: 'Имейлът вече е регистриран' });
  }

  const statusCode = res.statusCode !== 200 ? res.statusCode : 500;
  res.status(statusCode).json({ message: err.message || 'Вътрешна сървърна грешка' });
};

// Хваща заявки към несъществуващ route
const notFound = (req, res, next) => {
  res.status(404).json({ message: `Route ${req.originalUrl} не е намерен` });
};

module.exports = { errorHandler, notFound };
