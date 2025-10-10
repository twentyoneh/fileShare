FileShare — простое файловое хранилище (Backend + Frontend)

Лёгкий сервис для загрузки и раздачи файлов по токену.
Содержит автоматическое удаление устаревших файлов: если файл не скачивали более RETENTION_DAYS дней (по умолчанию — 30), он удаляется как из БД, так и с диска.

Backend: http://localhost:8080
Frontend: http://localhost:5500

API эндпоинты:
/api/health
/api/files (upload)
/d/{token} (download)

Backend (Javalin/Java 21) — REST API: загрузка, скачивание, health, авто-очистка старых файлов.
PostgreSQL — метаданные (имя, токен, размеры, даты скачивания и т.д.).
Storage — реальные файлы на диске (том Docker / volume).
Frontend — простой UI, использует API бэкенда.

Быстрый старт (Docker Compose)

# 1) Запуск

```bash
docker compose up -d --build
```

# 2) Проверка состояния

```bash
docker compose ps
```

# 3) Логи бэкенда

```bash
docker compose logs -f backend
```

### Запуск без Docker

В каждой подсистеме есть инструкции по локальному запуску без контейнеров:

/backend/README.md — запуск сервера (Javalin/Java 21) с подключением к вашей PostgreSQL(настройки PostgeSQL лежат в docker-compose).
/frontend/README.md — запуск фронтенда (локально через dev-сервер).
P.s. если планируется локальный запуск, легче поднять базу данных через docker, а всё остальное локально

### API эндпоинты

GET /api/health

Пример:
curl -s http://localhost:8080/api/health
Ответ 200:
{ "status": "ok" }

POST /api/files
Загрузка файла (multipart form). Поле: file.
Запрос:
POST /api/files
Content-Type: multipart/form-data
form-data: file=<ваш_файл>

Пример:
curl -F "file=@/path/to/photo.png" http://localhost:8080/api/files
Успешный ответ 200:
{
"id": "1a2b3c4d-...-9f",
"token": "AbCdEfGh...",
"downloadUrl": "http://localhost:8080/d/AbCdEfGh...",
"originalName": "photo.png",
"sizeBytes": 123456
}

GET /d/{token}
Скачивание файла по токену.

Пример:
curl -OJ http://localhost:8080/d/AbCdEfGh...
Заголовки ответа:
Content-Disposition: attachment; filename="..." — корректное имя файла.
Content-Type: application/octet-stream
Cache-Control: no-store

### Коды ошибок

400 Bad Request — неверный файл / формат.
404 Not Found - запрошенный ресурс не найден
413 Payload Too Large — превышен MAX_UPLOAD_MB.
500 Internal Server Error — внутренняя ошибка.

Формат ошибок
Все ошибки возвращаются как JSON:
{ "error": "сообщение_ошибки" }
Примеры:

400 → { "error": "..." }
404 → { "error": "not_found" }
413 → { "error": "..." }
500 → { "error": "internal_error" }
