const request = require('supertest');
const app = require('../src/app');
const { connect, clearDatabase, closeDatabase } = require('./testDb');

beforeAll(async () => connect());
afterEach(async () => clearDatabase());
afterAll(async () => closeDatabase());

// Регистрира тестов потребител с дадена роля и връща неговия JWT токен
const registerAndLogin = async (role, email) => {
  const res = await request(app).post('/api/auth/register').send({
    name: role === 'CLIENT' ? 'Test Client' : 'Test Provider',
    email,
    password: 'test123',
    phone: '0888000000',
    role,
  });
  return res.body.token;
};

const sofiaCoords = { lat: 42.6977, lng: 23.3219 };

const createPendingRequest = async (clientToken) => {
  const res = await request(app)
    .post('/api/requests')
    .set('Authorization', `Bearer ${clientToken}`)
    .send({ serviceType: 'TIRE_CHANGE', ...sofiaCoords });
  return res.body.request._id;
};

describe('POST /api/requests', () => {
  it('CLIENT създава заявка успешно', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');

    const res = await request(app)
      .post('/api/requests')
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ serviceType: 'TOWING', ...sofiaCoords, description: 'Не пали колата' });

    expect(res.status).toBe(201);
    expect(res.body.request.status).toBe('PENDING');
    expect(res.body.request.location.coordinates).toEqual([sofiaCoords.lng, sofiaCoords.lat]);
    expect(res.body.request.statusHistory).toHaveLength(1);
    expect(res.body.request.client.name).toBe('Test Client');
  });

  it('създава заявка с прикачен автомобил и го populate-ва в отговора', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');

    const vehicleRes = await request(app)
      .post('/api/vehicles')
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ make: 'Toyota', model: 'Corolla', licensePlate: 'CA1234BH', year: 2018 });
    const vehicleId = vehicleRes.body.vehicle._id;

    const res = await request(app)
      .post('/api/requests')
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ serviceType: 'TOWING', ...sofiaCoords, vehicleId });

    expect(res.status).toBe(201);
    expect(res.body.request.vehicle.licensePlate).toBe('CA1234BH');
  });

  it('връща 403, ако PROVIDER се опита да създаде заявка', async () => {
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');

    const res = await request(app)
      .post('/api/requests')
      .set('Authorization', `Bearer ${providerToken}`)
      .send({ serviceType: 'TOWING', ...sofiaCoords });

    expect(res.status).toBe(403);
  });

  it('връща 400 при липсваща локация', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');

    const res = await request(app)
      .post('/api/requests')
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ serviceType: 'TOWING' });

    expect(res.status).toBe(400);
  });

  it('връща 401 без токен', async () => {
    const res = await request(app).post('/api/requests').send({ serviceType: 'TOWING', ...sofiaCoords });

    expect(res.status).toBe(401);
  });
});

describe('GET /api/requests/nearby', () => {
  it('PROVIDER намира чакаща заявка в радиус', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');

    await request(app)
      .post('/api/requests')
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ serviceType: 'FUEL_DELIVERY', ...sofiaCoords });

    const res = await request(app)
      .get('/api/requests/nearby')
      .query({ lat: sofiaCoords.lat, lng: sofiaCoords.lng })
      .set('Authorization', `Bearer ${providerToken}`);

    expect(res.status).toBe(200);
    expect(res.body.count).toBe(1);
    expect(res.body.requests[0].serviceType).toBe('FUEL_DELIVERY');
  });
});

describe('PATCH /api/requests/:id/status', () => {
  it('PROVIDER приема чакаща заявка (PENDING -> ACCEPTED)', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');
    const requestId = await createPendingRequest(clientToken);

    const res = await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${providerToken}`)
      .send({ status: 'ACCEPTED' });

    expect(res.status).toBe(200);
    expect(res.body.request.status).toBe('ACCEPTED');
    expect(res.body.request.provider.name).toBe('Test Provider');
    expect(res.body.request.statusHistory).toHaveLength(2);
  });

  it('връща 403, ако CLIENT се опита директно да завърши заявка (само PROVIDER може)', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const requestId = await createPendingRequest(clientToken);

    const res = await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ status: 'COMPLETED' });

    expect(res.status).toBe(403);
  });

  it('връща 403, ако друг PROVIDER (неназначен) опита да завърши заявката', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');
    const otherProviderToken = await registerAndLogin('PROVIDER', 'other-provider@test.com');
    const requestId = await createPendingRequest(clientToken);

    await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${providerToken}`)
      .send({ status: 'ACCEPTED' });

    const res = await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${otherProviderToken}`)
      .send({ status: 'IN_PROGRESS' });

    expect(res.status).toBe(403);
  });
});

describe('PATCH /api/requests/:id/rating', () => {
  // Прекарва заявка през целия жизнен цикъл до COMPLETED, за да тества rating-а
  const createCompletedRequest = async (clientToken, providerToken) => {
    const createRes = await request(app)
      .post('/api/requests')
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ serviceType: 'JUMP_START', ...sofiaCoords });
    const requestId = createRes.body.request._id;

    await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${providerToken}`)
      .send({ status: 'ACCEPTED' });
    await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${providerToken}`)
      .send({ status: 'IN_PROGRESS' });
    await request(app)
      .patch(`/api/requests/${requestId}/status`)
      .set('Authorization', `Bearer ${providerToken}`)
      .send({ status: 'COMPLETED' });

    return requestId;
  };

  it('CLIENT оценява собствена COMPLETED заявка успешно', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');
    const requestId = await createCompletedRequest(clientToken, providerToken);

    const res = await request(app)
      .patch(`/api/requests/${requestId}/rating`)
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ rating: 5, comment: 'Много бърза реакция' });

    expect(res.status).toBe(200);
    expect(res.body.request.rating).toBe(5);
    expect(res.body.request.ratingComment).toBe('Много бърза реакция');
  });

  it('връща 400 при опит за оценка на все още неприключена заявка', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const requestId = await createPendingRequest(clientToken);

    const res = await request(app)
      .patch(`/api/requests/${requestId}/rating`)
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ rating: 4 });

    expect(res.status).toBe(400);
  });

  it('връща 400 при невалидна оценка извън диапазона 1-5', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');
    const requestId = await createCompletedRequest(clientToken, providerToken);

    const res = await request(app)
      .patch(`/api/requests/${requestId}/rating`)
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ rating: 7 });

    expect(res.status).toBe(400);
  });

  it('връща 403, ако друг клиент (не собственик) опита да оцени заявката', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');
    const otherClientToken = await registerAndLogin('CLIENT', 'other-client@test.com');
    const requestId = await createCompletedRequest(clientToken, providerToken);

    const res = await request(app)
      .patch(`/api/requests/${requestId}/rating`)
      .set('Authorization', `Bearer ${otherClientToken}`)
      .send({ rating: 3 });

    expect(res.status).toBe(403);
  });

  it('връща 400 при повторна оценка на вече оценена заявка', async () => {
    const clientToken = await registerAndLogin('CLIENT', 'client@test.com');
    const providerToken = await registerAndLogin('PROVIDER', 'provider@test.com');
    const requestId = await createCompletedRequest(clientToken, providerToken);

    await request(app)
      .patch(`/api/requests/${requestId}/rating`)
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ rating: 4 });

    const res = await request(app)
      .patch(`/api/requests/${requestId}/rating`)
      .set('Authorization', `Bearer ${clientToken}`)
      .send({ rating: 2 });

    expect(res.status).toBe(400);
  });
});
