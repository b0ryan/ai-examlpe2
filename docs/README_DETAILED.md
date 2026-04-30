# Базовая лотерея API (подробно)

## 1) Назначение

Проект реализует базовую лотерею с REST API:
- создание/удаление тиражей;
- список активных тиражей;
- создание билетов;
- генерация результата тиража;
- проверка результата билета;
- роли `ADMIN` и `USER`;
- JWT-аутентификация.

## 2) Стек

- Java 17+
- Maven
- `com.sun.net.httpserver.HttpServer`
- PostgreSQL
- JWT (`java-jwt`)
- BCrypt
- Jackson
- Docker / Docker Compose

## 3) Структура проекта

`src/main/java/lottery`:
- `Main` — bootstrap приложения
- `config` — env-конфигурация
- `db` — подключение и миграции
- `auth` — JWT и user context
- `handler` — HTTP handlers
- `service` — бизнес-логика
- `util` — вспомогательные утилиты

## 4) Роли

### USER
- регистрируется и логинится;
- получает список активных тиражей;
- покупает билет;
- проверяет только свои билеты.

### ADMIN
- все возможности USER;
- создает/удаляет тиражи;
- генерирует результат тиража.

## 5) Модель данных

Таблицы:
- `users`
- `draws`
- `draw_results`
- `tickets`
- `payments` (заготовка)

Ключевые статусы:
- draw: `ACTIVE`, `COMPLETED`
- ticket: `PENDING`, `WIN`, `LOSE`

### Структура таблиц PostgreSQL

#### `users`
- `id` `bigserial` PK
- `email` `text` NOT NULL UNIQUE
- `password_hash` `text` NOT NULL
- `role` `text` NOT NULL CHECK (`ADMIN` / `USER`)
- `created_at` `timestamp` DEFAULT `now()`

#### `draws`
- `id` `bigserial` PK
- `title` `text` NOT NULL
- `status` `text` NOT NULL CHECK (`ACTIVE` / `COMPLETED`)
- `created_by` `bigint` NOT NULL FK -> `users(id)` (`on delete restrict`)
- `created_at` `timestamp` DEFAULT `now()`

#### `draw_results`
- `id` `bigserial` PK
- `draw_id` `bigint` NOT NULL UNIQUE FK -> `draws(id)` (`on delete cascade`)
- `winning_numbers` `text` NOT NULL
- `created_at` `timestamp` DEFAULT `now()`

#### `tickets`
- `id` `bigserial` PK
- `user_id` `bigint` NOT NULL FK -> `users(id)` (`on delete cascade`)
- `draw_id` `bigint` NOT NULL FK -> `draws(id)` (`on delete cascade`)
- `numbers` `text` NOT NULL
- `status` `text` NOT NULL CHECK (`PENDING` / `WIN` / `LOSE`)
- `created_at` `timestamp` DEFAULT `now()`

#### `payments` (подготовлено, пока не используется в API)
- `id` `bigserial` PK
- `ticket_id` `bigint` UNIQUE FK -> `tickets(id)` (`on delete cascade`)
- `amount` `numeric(10,2)` NOT NULL
- `status` `text` NOT NULL CHECK (`PAID` / `FAILED`)
- `created_at` `timestamp` DEFAULT `now()`

### Связи между таблицами
- один `user` может создать много `draws` и иметь много `tickets`;
- у одного `draw` может быть много `tickets`, но только один `draw_results`;
- у одного `ticket` может быть максимум один `payment`.

## 6) Идемпотентность генерации результата

`POST /draws/generate-result` реализован идемпотентно:
- при первом вызове создается результат, закрывается тираж и пересчитываются билеты;
- при повторном вызове для того же `drawId` возвращается уже существующая
  выигрышная комбинация без повторной обработки.

Это защищает от дублей при ретраях клиента/сети.

## 7) Переменные окружения

Пример (`.env.example`):

```env
PORT=3000
DATABASE_URL=jdbc:postgresql://db:5432/lottery?user=lotteryowner&password=admin
JWT_SECRET=super-secret-key
ADMIN_EMAIL=admin@example.com
ADMIN_PASSWORD=admin123
```

## 8) Локальный запуск

Требования:
- Java 17+
- Maven 3.9+
- PostgreSQL

Сборка:

```bash
mvn -DskipTests package
```

Запуск:

```bash
java -jar target/basic-lottery-1.0.0-jar-with-dependencies.jar
```

## 9) Docker запуск

```bash
docker compose up --build -d
```

Проверка:

```bash
curl http://localhost:3000/health
```

## 10) Эндпоинты

### Auth
- `POST /auth/register`
- `POST /auth/login`

### Draws
- `POST /draws` (admin)
- `GET /draws`
- `DELETE /draws?id={drawId}` (admin)
- `POST /draws/generate-result` (admin)

### Tickets
- `POST /tickets`
- `GET /tickets/check?ticketId={ticketId}`

### System
- `GET /health`

## 11) Пример e2e smoke flow

1. Логин admin  
2. Регистрация user  
3. Логин user  
4. Создание тиража (admin)  
5. Создание билета (user)  
6. Генерация результата (admin)  
7. Проверка билета (user)

## 12) Коды ответов

- `200` — success
- `201` — created
- `400` — validation error
- `401` — unauthorized
- `403` — forbidden
- `404` — not found
- `405` — method not allowed
- `409` — state conflict
- `500` — internal/db error
