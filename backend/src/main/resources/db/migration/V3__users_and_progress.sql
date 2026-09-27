-- V3: пользователи, привязка Telegram и прогресс повторений.

create table users (
    id            bigserial primary key,
    display_name  varchar(120) not null,
    created_at    timestamptz  not null default now(),
    last_seen_at  timestamptz
);

create table telegram_accounts (
    id           bigserial primary key,
    user_id      bigint      not null references users (id) on delete cascade,
    telegram_id  bigint      not null unique,
    username     varchar(64),
    language_code varchar(8),
    linked_at    timestamptz not null default now()
);

-- История ответов пользователя.
create table user_answers (
    id           bigserial primary key,
    user_id      bigint      not null references users (id) on delete cascade,
    question_id  bigint      not null references questions (id) on delete cascade,
    self_rating  varchar(16) not null,
    answered_at  timestamptz not null default now(),
    constraint user_answers_rating_check check (self_rating in ('AGAIN', 'HARD', 'GOOD', 'EASY'))
);

create index idx_user_answers_user on user_answers (user_id, answered_at desc);

-- Состояние алгоритма интервальных повторений (SM-2) по паре пользователь/вопрос.
create table review_schedule (
    user_id        bigint      not null references users (id) on delete cascade,
    question_id    bigint      not null references questions (id) on delete cascade,
    ease_factor    numeric(4, 2) not null default 2.50,
    interval_days  integer     not null default 0,
    repetitions    integer     not null default 0,
    due_at         timestamptz not null default now(),
    last_reviewed_at timestamptz,
    primary key (user_id, question_id)
);

create index idx_review_schedule_due on review_schedule (user_id, due_at);
