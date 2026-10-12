-- V10: LLM-разбор ответов мок-интервью (SOBES-28).

alter table interview_answers
    add column llm_score   int,
    add column llm_covered text,
    add column llm_missing text,
    add column llm_verdict text;

comment on column interview_answers.llm_score is '0-100 покрытие эталона по мнению LLM; null — оценка недоступна или ответ пустой.';
