# Базовая лотерея API (Java)

REST API для базовой лотереи, реализованный на чистом Java **без веб-фреймворков**.

Проект покрывает ключевые требования:
- создание и удаление тиражей;
- получение списка активных тиражей;
- создание билетов;
- генерация выигрышной комбинации;
- проверка результатов билетов;
- статусы билетов `WIN` / `LOSE` / `PENDING`;
- роли пользователей `ADMIN` и `USER`;
- аутентификация через JWT;
- запуск в Docker.

---

## 1. Технологии

- Язык: `Java 17+`
- Сборка: `Maven`
- HTTP-сервер: `com.sun.net.httpserver.HttpServer`
- База данных: `PostgreSQL`
- Аутентификация: `JWT` (`java-jwt`)
- Хеширование паролей: `BCrypt`
- Сериализация JSON: `Jackson`
- Контейнеризация: `Docker`, `docker compose`

---

## 2. Архитектура проекта

Исходный код расположен в `src/main/java/lottery`.

Пакеты:
- `lottery` — точка входа (`Main`)
- `lottery.config` — конфигурация окружения
- `lottery.db` — подключение к БД, миграции, сидирование админа
- `lottery.auth` — JWT-сервис и контекст пользователя
- `lottery.handler` — HTTP-обработчики эндпоинтов
- `lottery.service` — бизнес-логика
- `lottery.util` — вспомогательные классы (JSON, парсинг, работа с числами)

Такое разделение упрощает поддержку, тестирование и дальнейшее расширение проекта.

---

## 3. Роли и права доступа

### USER
- регистрация и вход;
- просмотр активных тиражей;
- покупка (создание) билета;
- проверка своего билета.

### ADMIN
- все права `USER`;
- создание тиражей;
- удаление тиражей;
- генерация результата тиража.

---

## 4. Модель данных

В PostgreSQL создаются таблицы:

- `users`
  - `id`
  - `email` (unique)
  - `password_hash`
  - `role` (`ADMIN` / `USER`)
  - `created_at`

- `draws`
  - `id`
  - `title`
  - `status` (`ACTIVE` / `COMPLETED`)
  - `created_by` (FK -> `users.id`)
  - `created_at`

- `draw_results`
  - `id`
  - `draw_id` (unique, FK -> `draws.id`)
  - `winning_numbers` (CSV-строка вида `1,5,12,31,48`)
  - `created_at`

- `tickets`
  - `id`
  - `user_id` (FK -> `users.id`)
  - `draw_id` (FK -> `draws.id`)
  - `numbers` (CSV-строка)
  - `status` (`PENDING` / `WIN` / `LOSE`)
  - `created_at`

- `payments` (опциональная сущность по ТЗ, в текущей версии таблица подготовлена)
  - `id`
  - `ticket_id` (FK -> `tickets.id`)
  - `amount`
  - `status` (`PAID` / `FAILED`)
  - `created_at`

---

## 5. Бизнес-логика статусов

### Статус тиража
- При создании тираж получает статус `ACTIVE`.
- После генерации результата тираж переводится в `COMPLETED`.

### Статус билета
- При создании билет получает статус `PENDING`.
- После генерации результата тиража:
  - если комбинация билета совпадает с выигрышной — `WIN`;
  - иначе — `LOSE`.

Проверка билета показывает текущий статус билета и статус связанного тиража.

---

## 6. Переменные окружения

Пример: `.env.example`

```env
PORT=3000
DATABASE_URL=jdbc:postgresql://db:5432/lottery
JWT_SECRET=super-secret-key
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
```

Пояснения:
- `PORT` — порт HTTP API;
- `DATABASE_URL` — JDBC URL PostgreSQL;
- `JWT_SECRET` — секрет для подписи JWT;
- `ADMIN_EMAIL`, `ADMIN_PASSWORD` — учетные данные администратора, который создается автоматически при старте.

---

## 7. Запуск проекта локально

### Требования
- Java 17+;
- Maven 3.9+;
- PostgreSQL 14+.

### Шаги
1. Создать БД `lottery` в PostgreSQL.
2. Задать переменные окружения (или использовать значения по умолчанию).
3. Собрать проект:

```bash
mvn -DskipTests package
```

4. Запустить приложение:

```bash
java -jar target/basic-lottery-1.0.0.jar
```

API будет доступен на `http://localhost:3000`.

---

## 8. Запуск в Docker

В корне проекта:

```bash
docker compose up --build
```

Что произойдет:
- поднимется контейнер `PostgreSQL`;
- соберется и запустится Java-приложение;
- автоматически выполнятся миграции и сидирование администратора.

После старта API доступен по адресу:
- `http://localhost:3000`

Проверка:

```bash
curl http://localhost:3000/health
```

---

## 9. API эндпоинты

Базовый URL: `http://localhost:3000`

### 9.1 Health

#### `GET /health`
Проверка доступности сервиса.

Ответ `200`:
```json
{ "status": "ok" }
```

---

### 9.2 Аутентификация

#### `POST /auth/register`
Регистрация обычного пользователя (`USER`).

Тело:
```json
{
  "email": "user@example.com",
  "password": "qwerty123"
}
```

Ответ `201`:
```json
{
  "id": 2,
  "email": "user@example.com",
  "role": "USER"
}
```

---

#### `POST /auth/login`
Вход пользователя, получение JWT.

Тело:
```json
{
  "email": "user@example.com",
  "password": "qwerty123"
}
```

Ответ `200`:
```json
{
  "token": "<JWT>",
  "role": "USER"
}
```

---

### 9.3 Тиражи

#### `POST /draws` (только `ADMIN`)
Создание тиража.

Заголовок:
- `Authorization: Bearer <ADMIN_TOKEN>`

Тело:
```json
{
  "title": "Тираж #1"
}
```

Ответ `201`:
```json
{
  "id": 1,
  "title": "Тираж #1",
  "status": "ACTIVE"
}
```

---

#### `GET /draws` (авторизованный пользователь)
Список активных тиражей.

Заголовок:
- `Authorization: Bearer <TOKEN>`

Ответ `200`:
```json
{
  "items": [
    {
      "id": 1,
      "title": "Тираж #1",
      "status": "ACTIVE",
      "createdAt": "2026-04-27T00:00:00Z"
    }
  ]
}
```

---

#### `DELETE /draws?id={drawId}` (только `ADMIN`)
Удаление тиража.

Заголовок:
- `Authorization: Bearer <ADMIN_TOKEN>`

Ответ `200`:
```json
{ "status": "deleted" }
```

---

### 9.4 Билеты

#### `POST /tickets`
Создание билета в активном тираже.

Заголовок:
- `Authorization: Bearer <TOKEN>`

Тело:
```json
{
  "drawId": 1,
  "numbers": [1, 5, 12, 31, 48]
}
```

Правила валидации:
- ровно 5 чисел;
- числа в диапазоне `1..50`;
- числа должны быть уникальными;
- тираж должен существовать и быть `ACTIVE`.

Ответ `201`:
```json
{
  "id": 10,
  "status": "PENDING"
}
```

---

#### `GET /tickets/check?ticketId={ticketId}`
Проверка результата билета.

Заголовок:
- `Authorization: Bearer <TOKEN>`

Ограничения доступа:
- `USER` может смотреть только свои билеты;
- `ADMIN` может смотреть любой билет.

Ответ `200`:
```json
{
  "ticketId": 10,
  "drawId": 1,
  "numbers": [1, 5, 12, 31, 48],
  "ticketStatus": "WIN",
  "drawStatus": "COMPLETED"
}
```

---

### 9.5 Результаты тиража

#### `POST /draws/generate-result` (только `ADMIN`)
Генерация выигрышной комбинации, закрытие тиража и пересчет статусов билетов.

Заголовок:
- `Authorization: Bearer <ADMIN_TOKEN>`

Тело:
```json
{
  "drawId": 1
}
```

Ответ `200`:
```json
{
  "drawId": 1,
  "winningNumbers": [1, 5, 12, 31, 48],
  "drawStatus": "COMPLETED"
}
```

---

## 10. Полный пример сценария через curl

Ниже типовой путь: регистрация -> логин -> создание тиража -> билет -> генерация результата -> проверка.

### 10.1 Логин администратора
```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"
```

Скопируйте `token` в переменную `ADMIN_TOKEN`.

### 10.2 Регистрация пользователя
```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"user@example.com\",\"password\":\"qwerty123\"}"
```

### 10.3 Логин пользователя
```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"user@example.com\",\"password\":\"qwerty123\"}"
```

Скопируйте `token` в `USER_TOKEN`.

### 10.4 Создание тиража (ADMIN)
```bash
curl -X POST http://localhost:3000/draws \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"Тираж #1\"}"
```

Скопируйте `id` тиража в `DRAW_ID`.

### 10.5 Покупка билета (USER)
```bash
curl -X POST http://localhost:3000/tickets \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"drawId\":1,\"numbers\":[1,5,12,31,48]}"
```

Скопируйте `id` билета в `TICKET_ID`.

### 10.6 Генерация результата (ADMIN)
```bash
curl -X POST http://localhost:3000/draws/generate-result \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d "{\"drawId\":1}"
```

### 10.7 Проверка билета
```bash
curl "http://localhost:3000/tickets/check?ticketId=1" \
  -H "Authorization: Bearer <USER_TOKEN>"
```

---

## 11. Коды ответов (общие)

- `200` — успешная операция
- `201` — ресурс создан
- `400` — ошибка валидации входных данных
- `401` — невалидный/отсутствующий токен
- `403` — недостаточно прав
- `404` — ресурс не найден
- `405` — HTTP-метод не поддерживается
- `409` — конфликт состояния (например, тираж не `ACTIVE`)
- `500` — внутренняя ошибка сервера/БД

---

## 12. Что можно улучшить дальше

- добавить полноценный слой `repository`;
- вынести SQL в отдельные классы/файлы;
- добавить unit/integration тесты;
- добавить OpenAPI/Swagger документацию;
- реализовать полноценные операции `Payment/Invoice`;
- добавить rate limit и аудит событий администратора.
