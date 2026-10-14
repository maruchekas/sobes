package ru.sobes.interview.llm

/**
 * Результат LLM-разбора ответа на вопрос интервью.
 */
data class Evaluation(
    val score: Int,            // 0..100 — покрытие эталона
    val covered: String,       // что ответ покрывает (кратко, по-русски)
    val missing: String,       // что important пропущено
    val verdict: String,       // одна фраза-вердикт
)

/** LLM-оценщик открытых ответов (SOBES-28). Реализации: DeepSeekEvaluator, NoopEvaluator. */
interface AnswerEvaluator {
    fun evaluate(questionBody: String, referenceAnswer: String, userAnswer: String): Evaluation?
}
