-- V5: связка аккаунта платформы с Telegram-ботом (глубокие ссылки из бота).

create table bot_link_tokens (
    token       varchar(64) primary key,
    telegram_id bigint      not null,
    created_at  timestamptz not null default now(),
    expires_at  timestamptz not null,
    used_at     timestamptz
);

create index idx_bot_link_tokens_tg on bot_link_tokens (telegram_id);

comment on table bot_link_tokens is 'Одноразовые токены привязки веб-аккаунта к Telegram (генерирует бот при /start, погашает веб после входа). user_id не хранится: владелец определяется в момент погашения токена.';
