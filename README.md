# Базовая лотерея API (Java)

Короткая версия документации.  
Полная документация: `docs/README_DETAILED.md`.

## Что это

REST API лотереи на чистом Java (без веб-фреймворков) с:
- JWT-аутентификацией;
- ролями `ADMIN` и `USER`;
- PostgreSQL;
- Docker-запуском.

## Быстрый старт (Docker)

```bash
docker compose up --build -d
```

Проверка:

```bash
curl http://localhost:3000/health
```

Ожидаемый ответ:

```json
{"status":"ok"}
```

## Основные эндпоинты

- `POST /auth/register`
- `POST /auth/login`
- `POST /draws` (admin)
- `GET /draws`
- `DELETE /draws?id={drawId}` (admin)
- `POST /tickets`
- `POST /draws/generate-result` (admin)
- `GET /tickets/check?ticketId={ticketId}`
- `GET /health`

