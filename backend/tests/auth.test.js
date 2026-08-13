const request = require('supertest');
const app = require('../src/app');
const { connect, clearDatabase, closeDatabase } = require('./testDb');

beforeAll(async () => connect());
afterEach(async () => clearDatabase());
afterAll(async () => closeDatabase());

const clientPayload = {
  name: 'Ivan Ivanov',
  email: 'ivan@test.com',
  password: 'test123',
  phone: '0888111222',
  role: 'CLIENT',
};

describe('POST /api/auth/register', () => {
  it('регистрира нов CLIENT и връща token + user', async () => {
    const res = await request(app).post('/api/auth/register').send(clientPayload);

    expect(res.status).toBe(201);
    expect(res.body.token).toBeDefined();
    expect(res.body.user.email).toBe(clientPayload.email);
    expect(res.body.user.role).toBe('CLIENT');
    expect(res.body.user.password).toBeUndefined();
  });

  it('регистрира PROVIDER, когато role=PROVIDER е подадена', async () => {
    const res = await request(app)
      .post('/api/auth/register')
      .send({ ...clientPayload, email: 'provider@test.com', role: 'PROVIDER' });

    expect(res.status).toBe(201);
    expect(res.body.user.role).toBe('PROVIDER');
  });

  it('връща 400 при липсващо задължително поле', async () => {
    const { password, ...withoutPassword } = clientPayload;

    const res = await request(app).post('/api/auth/register').send(withoutPassword);

    expect(res.status).toBe(400);
  });

  it('връща 409 при вече регистриран имейл', async () => {
    await request(app).post('/api/auth/register').send(clientPayload);
    const res = await request(app).post('/api/auth/register').send(clientPayload);

    expect(res.status).toBe(409);
  });
});

describe('POST /api/auth/login', () => {
  beforeEach(async () => {
    await request(app).post('/api/auth/register').send(clientPayload);
  });

  it('връща token при валидни данни', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: clientPayload.email, password: clientPayload.password });

    expect(res.status).toBe(200);
    expect(res.body.token).toBeDefined();
  });

  it('връща 401 при грешна парола', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: clientPayload.email, password: 'wrongpassword' });

    expect(res.status).toBe(401);
  });

  it('връща 401 за несъществуващ имейл', async () => {
    const res = await request(app)
      .post('/api/auth/login')
      .send({ email: 'nobody@test.com', password: 'test123' });

    expect(res.status).toBe(401);
  });
});
