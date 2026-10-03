> **Статус проекта:** MVP в разработке. Неделя 1 из 8 — каркас, CI и стартовый банк вопросов.

# Sobes — платформа подготовки к техническим собеседованиям

Веб-платформа, где разработчик системно готовится к собеседованиям и поддерживает знания: каталог вопросов с эталонными ответами, интервальные повторения, мок-интервью, аналитика слабых тем и Telegram-бот для ежедневной практики.

Ключевая идея — не «ещё один список вопросов», а тренажёр с расписанием повторений: платформа сама решает, что показать сегодня, исходя из ваших слабых тем и забывания.

## Что уже есть

- Каталог вопросов с фильтрами по темам и уровню сложности (Junior / Middle / Senior).
- Стартовый банк: **112 вопросов** по Java, Kotlin, Spring, Kafka, PostgreSQL, Docker и смежным темам.
- REST API с документацией OpenAPI.
- Схема БД под пользователей, историю ответов и интервальные повторения (SM-2).

## Что в планах

- [ ] Вход через Telegram и личный кабинет
- [ ] Практика на карточках с самооценкой и алгоритмом повторений
- [ ] Дашборд: прогресс по темам, слабые места, серия дней
- [ ] Telegram-бот: вопрос дня и напоминания
- [ ] Проверка развёрнутых ответов через LLM
- [ ] Режим мок-интервью с таймером и итоговым отчётом
- [ ] Админка для наполнения банка вопросов
- [ ] Публичные страницы вопросов с серверным рендерингом для поиска

## Стек

**Бэкенд:** Kotlin 2.4, Spring Boot 4.1, Java 21, PostgreSQL 16, Flyway, springdoc-openapi
**Фронтенд:** TypeScript, Next.js 16, React 19, Tailwind CSS 4
**Инфраструктура:** Docker Compose, Caddy (автоматический HTTPS), GitHub Actions
**Наблюдаемость:** Spring Boot Actuator, Micrometer, Prometheus

## Архитектура

```mermaid
flowchart LR
    U[Пользователь] -->|HTTPS| C[Caddy<br/>reverse-proxy]
    B[Telegram-бот] -->|REST| A
    C -->|/api/*| A[Backend<br/>Kotlin + Spring Boot]
    C -->|/*| F[Frontend<br/>Next.js, SSR]
    A --> P[(PostgreSQL)]
    A -->|метрики| M[Prometheus]
```

Подробнее — в [docs/architecture.md](docs/architecture.md).

## Быстрый старт

Нужны JDK 21, Node.js 22+ и Docker.

```bash
# 1. Локальная конфигурация (один раз; оба файла игнорируются Git)
cp .env.example .env.local
cp frontend/.env.example frontend/.env.local
# Заполните TELEGRAM_BOT_TOKEN, JWT_SECRET и публичные настройки фронтенда.

# 2. База данных для разработки
docker compose -f infra/docker-compose.dev.yml up -d

# 3. Бэкенд (миграции применяются автоматически, .env.local загрузится сам)
cd backend && ./gradlew bootRun
# API:    http://localhost:8080/api/v1/questions
# Swagger: http://localhost:8080/swagger-ui.html

# 4. Фронтенд
cd frontend && npm install && npm run dev
# http://localhost:3000
```

## Запуск в продакшене

```bash
cd infra
cp .env.example .env
# Заполните .env: POSTGRES_PASSWORD, TELEGRAM_BOT_TOKEN, JWT_SECRET и DOMAIN.
docker compose up -d --build
```

Caddy сам получит сертификат Let's Encrypt. Файл `infra/.env` игнорируется Git и передаёт
секреты контейнерам через Docker Compose; коммитить его не нужно.

## Как мы работаем (ветки и PR)

Флоу — упрощённый GitFlow:

- `main` — стабильная ветка, всегда деплоябельное состояние. Мерж только через PR.
- `develop` — интеграционная ветка. Здесь собираются фичи между вехами.
- `feature/SOBES-N-краткое-имя` — ветка на задачу из Jira (номер обязателен в имени).
  По готовности — PR в `develop` (шаблон заполнить), ревью, зелёный CI, merge (squash).
- `hotfix/SOBES-N-...` — срочные фиксы: PR в `main` **и** в `develop` сразу (бэкпорт обязателен).
- `develop → main` — PR-пакетами к вехам (не после каждой фичи), мерж сопровождается тегом версии (`v0.1.0`, `v0.2.0`…).

Ветки `main` и `develop` защищены: обязательны зелёные чеки CI, force-push запрещён.

## Структура репозитория

```
backend/    Kotlin + Spring Boot: API, бизнес-логика, миграции Flyway
frontend/   Next.js: каталог вопросов, личный кабинет, лендинг
bot/        Telegram-бот (в разработке)
infra/      Docker Compose, конфигурация Caddy
docs/       Архитектура и проектные решения
```

## Тесты

```bash
cd backend && ./gradlew test     # интеграционные тесты на Testcontainers
cd frontend && npm run typecheck # проверка типов
```

## Лицензия

MIT — см. [LICENSE](LICENSE).
