-- V6: вопрос дня — что и когда доставлено ботом (защита от дублей + история для /stats).

create table daily_questions (
    user_id     bigint      not null references users (id) on delete cascade,
    for_date    date        not null,
    question_id bigint      not null references questions (id),
    sent_at     timestamptz not null default now(),
    primary key (user_id, for_date)
);

create index idx_daily_questions_date on daily_questions (for_date);

comment on table daily_questions is 'Вопрос дня: ровно один на пользователя в его локальный день; отправлено ботом.';
