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
