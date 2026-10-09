-- V9: мок-интервью — сессии и ответы.

create table interview_sessions (
    id          bigserial primary key,
    user_id     bigint      not null references users (id) on delete cascade,
    status      varchar(16) not null default 'IN_PROGRESS',  -- IN_PROGRESS / FINISHED
    categories  text        not null default '',             -- slug'и через запятую, '' = все
    total       int         not null,
    created_at  timestamptz not null default now(),
    finished_at timestamptz
);

create table interview_answers (
    id              bigserial primary key,
    session_id      bigint      not null references interview_sessions (id) on delete cascade,
    question_id     bigint      not null references questions (id),
    user_text       text        not null default '',
    self_rating     varchar(8),
    seconds_spent   int,
    answered_at     timestamptz not null default now(),
    unique (session_id, question_id)
);

create index idx_interview_sessions_user on interview_sessions (user_id, created_at desc);

comment on table interview_sessions is 'Мок-интервью: N вопросов по выбранным категориям, таймер на вопрос, отчёт по завершении.';
comment on table interview_answers is 'Ответы в мок-интервью; self_rating пишется и в SM-2 (review_schedule) через PracticeService.';
