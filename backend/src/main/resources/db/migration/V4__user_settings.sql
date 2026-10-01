-- V4: настройки пользователя для личного кабинета и будущих напоминаний бота.

create table user_settings (
    user_id           bigint primary key references users (id) on delete cascade,
    timezone          varchar(64)  not null default 'Europe/Moscow',
    reminder_time     time         not null default '10:00',
    reminders_enabled boolean      not null default true,
    created_at        timestamptz  not null default now(),
    updated_at        timestamptz  not null default now()
);

comment on table user_settings is 'Настройки личного кабинета: таймзона и время напоминаний о повторениях.';
