# Roadside Assistance — Backend

REST API за дипломен проект „Мобилно приложение за пътна помощ“.

## Технологичен стек

- **Runtime**: Node.js
- **Framework**: Express 5
- **База данни**: MongoDB + Mongoose ODM
- **Автентикация**: JWT (jsonwebtoken) + bcrypt хеширане на пароли
- **Тестове**: Jest + Supertest + mongodb-memory-server (in-memory MongoDB, не пипа dev базата)

## Стартиране

```bash
npm install
cp .env.example .env   # после попълни MONGO_URI и JWT_SECRET
npm run dev             # nodemon, рестартира при промяна
# или
npm start                # обикновен node процес
```

## Тестове

```bash
npm test
```

16 теста (Jest + Supertest), покриващи регистрация/вход, създаване/приемане/отказ на заявки, ролеви ограничения и прикачване на автомобил към заявка. Изпълняват се срещу in-memory MongoDB — не докосват реалната dev база.

## Postman

Готова колекция в [`postman/RoadsideAssistance.postman_collection.json`](postman/RoadsideAssistance.postman_collection.json) — импортирай я в Postman и пускай заявките отгоре надолу; токените и ID-тата се записват автоматично в collection variables между заявките.

## Модели на данни

### User

| Поле | Тип | Описание |
|---|---|---|
| `name` | String | Име |
| `email` | String | Уникален, използва се за вход |
| `password` | String | bcrypt хеш (никога не се връща в API отговори) |
| `phone` | String | Телефон за контакт |
| `role` | `CLIENT` \| `PROVIDER` | Шофьор или доставчик на пътна помощ |
| `location` | GeoJSON Point | Последна позната локация (само за PROVIDER, обновява се при `/requests/nearby`) |

### Vehicle

| Поле | Тип | Описание |
|---|---|---|
| `owner` | ObjectId → User | Собственик (винаги CLIENT) |
| `make`, `model` | String | Марка, модел |
| `licensePlate` | String | Регистрационен номер |
| `year` | Number | Година на производство |

### ServiceRequest

| Поле | Тип | Описание |
|---|---|---|
| `client` | ObjectId → User | Кой е подал заявката |
| `provider` | ObjectId → User \| null | Кой я е приел |
| `vehicle` | ObjectId → Vehicle \| null | По избор, кой автомобил е засегнат |
| `serviceType` | enum | `TOWING`, `JUMP_START`, `TIRE_CHANGE`, `FUEL_DELIVERY`, `MECHANICAL_FAILURE`, `OTHER` |
| `location` | GeoJSON Point | `[longitude, latitude]`, с `2dsphere` индекс |
| `status` | enum | `PENDING` → `ACCEPTED` → `IN_PROGRESS` → `COMPLETED`, или `CANCELLED` |
| `statusHistory` | [{status, changedAt}] | Автоматична хронология, попълва се от `pre('save')` хук |

## API ендпойнти

Base URL: `http://localhost:5000`

### Auth (`/api/auth`)

| Метод | Път | Достъп | Тяло | Описание |
|---|---|---|---|---|
| POST | `/register` | Public | `{name, email, password, phone, role}` | Регистрация, връща `{token, user}` |
| POST | `/login` | Public | `{email, password}` | Вход, връща `{token, user}` |
| GET | `/me` | Private | — | Данни за текущия потребител |

### Vehicles (`/api/vehicles`) — само CLIENT

| Метод | Път | Тяло | Описание |
|---|---|---|---|
| GET | `/` | — | Списък с автомобилите на текущия потребител |
| POST | `/` | `{make, model, licensePlate, year}` | Добавя нов автомобил |

### Requests (`/api/requests`)

| Метод | Път | Достъп | Тяло / Query | Описание |
|---|---|---|---|---|
| POST | `/` | CLIENT | `{serviceType, lat, lng, address?, description?, vehicleId?}` | Създава заявка |
| GET | `/nearby` | PROVIDER | `?lat=&lng=&maxDistance=` (метри, default 15000) | Чакащи заявки в радиус ($near) |
| GET | `/my` | Private | — | Заявките на текущия потребител (клиент или доставчик) |
| GET | `/:id` | Private | — | Детайли за заявка |
| PATCH | `/:id/status` | Private | `{status}` | Смяна на статус (виж таблицата с преходи по-долу) |

### Позволени преходи на статус

| От | Към | Кой | Забележка |
|---|---|---|---|
| PENDING | ACCEPTED | PROVIDER | Назначава провидъра към заявката |
| ACCEPTED | IN_PROGRESS | PROVIDER (назначеният) | |
| IN_PROGRESS | COMPLETED | PROVIDER (назначеният) | |
| PENDING/ACCEPTED | CANCELLED | CLIENT (собственик) или PROVIDER | |

Всеки друг преход връща `400`/`403` с подходящо съобщение.

## Известни ограничения (съзнателен избор за обхвата на дипломната)

- Няма WebSockets/push известия — Android клиентът обновява статуса чрез periodic polling.
- Няма rate limiting / refresh tokens — JWT е с фиксиран `JWT_EXPIRES_IN` (default 7 дни).
- `CANCELLED` преходът позволява на всеки PROVIDER (не само назначения) да отмени PENDING/ACCEPTED заявка — съзнателно опростяване, а не пропуск.
