# Базовая лотерея API (Java)

## Что это

REST API первого сценария лотереи на чистом Java (без веб-фреймворков) с:
- JWT-аутентификацией;
- ролями `ADMIN` и `USER`;
- PostgreSQL;
- Docker-запуском.

## Документация

- Полная документация: [здесь](docs/README.md);
- Архитектура отказоустойчивого High Availability кластера СУБД PostgreSQL под управлением Patroni, развернутая в процессе работы над приложением (Подстречный А.В.): [здесь](docs/README_DBA.md)

Если СУБД развернута отдельно от приложения, воспользуйтесь конфигурацией из файла: `docker-compose-app-only.yml`. 

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

## Структура проекта

```
lottery
├── Dockerfile
├── README.md
├── docker-compose-example.yml      # запуск только приложения. Необходимо задать строку подключения к БД.
├── docker-compose.yml              # запуск приложения и СУБД PostgreSQL в Docker Compose
├── docs
│   ├── README.md
│   └── README_DBA.md
├── pom.xml
└── src
    └── main
        └── java
            └── lottery
                ├── Main.java
                ├── auth
                ├── config
                ├── db
                ├── handler
                ├── service
                └── util
```
