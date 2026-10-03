-- V7: журнал напоминаний — антиспам (не чаще 1/24ч на тип напоминания).

create table reminder_log (
    id       bigserial primary key,
    user_id  bigint      not null references users (id) on delete cascade,
    kind     varchar(32) not null,
    sent_at  timestamptz not null default now()
);

create index idx_reminder_log_user_kind_time on reminder_log (user_id, kind, sent_at desc);

comment on table reminder_log is 'Отправленные напоминания;.kind: REVIEW (просроченные повторения), позднее DAILY и др.';
