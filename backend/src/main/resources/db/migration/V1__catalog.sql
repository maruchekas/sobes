-- V1: базовый каталог вопросов платформы Sobes.

create table categories (
    id         bigserial primary key,
    slug       varchar(64)  not null unique,
    title      varchar(128) not null,
    sort_order integer      not null default 0
);

create table questions (
    id          bigserial primary key,
    category_id bigint      not null references categories (id),
    difficulty  varchar(16) not null,
    body        text        not null,
    answer      text        not null,
    followup    text,
    created_at  timestamptz not null default now(),
    updated_at  timestamptz not null default now(),
    constraint questions_difficulty_check check (difficulty in ('JUNIOR', 'MIDDLE', 'SENIOR'))
);

create index idx_questions_category_difficulty on questions (category_id, difficulty);
