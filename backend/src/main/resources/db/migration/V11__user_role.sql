-- V11: роль пользователя для админки (SOBES-30).

alter table users add column role varchar(16) not null default 'USER';

comment on column users.role is 'USER / ADMIN. Админы управляют банком вопросов через /admin; выдаётся через env ADMIN_TELEGRAM_IDS (телефрам-id через запятую) при входе.';
