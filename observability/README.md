# Наблюдаемость (SOBES-33)

Backend отдаёт метрики в формате Prometheus на `/actuator/prometheus`.

## Бизнес-метрики

| Метрика | Что означает |
|---|---|
| `sobes_registrations_total` | новые пользователи (вход через Telegram) |
| `sobes_answers_total` + `sobes_answer_seconds` | ответы в практике SM-2 |
| `sobes_interviews_total` | начатые мок-интервью |
| `sobes_llm_evaluations_total` / `sobes_llm_failures_total` / `sobes_llm_seconds` | LLM-оценки: счёт, сбои, p50/p95 |
| `sobes_bot_daily_delivered_total` / `sobes_bot_reminders_total` | доставки вопроса дня и напоминаний ботом |

Плюс стандартные: `http_server_requests_seconds` (RPS/латентность/ошибки по URI), `jvm_*`, `hikaricp_*`.

## Стек на сервере

```bash
cd observability
SOBES_BACKEND_TARGET=172.17.0.1:8080 docker compose up -d
```

- **Prometheus** — `127.0.0.1:9090`, retention 30 дней, скрапит backend раз в 15с
- **Grafana** — `127.0.0.1:3001`, datasource и дашборд «Sobes — обзор» провижинятся автоматически
  (12 панелей: регистрации/ответы/интервью/LLM, RPS и p95 по URI, 5xx, heap, HikariCP, доставки бота)

Порты проброшены только на localhost — снаружи Grafana открывается через reverse-proxy (Caddy) с авторизацией, см. SOBES-35.

`SOBES_BACKEND_TARGET` — адрес backend-хоста, как его видит контейнер Prometheus:
`host.docker.internal:8080` (Docker Desktop), `172.17.0.1:8080` (docker0 на Linux-сервере).

Пароль Grafana задаётся `GRAFANA_ADMIN_PASSWORD` (дефолт `sobes-admin` — сменить на проде).
