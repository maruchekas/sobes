package ru.sobes.question

/** Короткое представление вопроса для списков. */
data class QuestionSummary(
    val id: Long,
    val category: String,
    val categorySlug: String,
    val difficulty: Difficulty,
    val body: String,
)

/** Полное представление вопроса вместе с эталонным ответом. */
data class QuestionDetails(
    val id: Long,
    val category: String,
    val categorySlug: String,
    val difficulty: Difficulty,
    val body: String,
    val answer: String,
    val followup: String?,
)

data class CategoryView(
    val slug: String,
    val title: String,
    val questions: Long,
)

fun Question.toSummary() = QuestionSummary(
    id = requireNotNull(id),
    category = category.title,
    categorySlug = category.slug,
    difficulty = difficulty,
    body = body,
)

fun Question.toDetails() = QuestionDetails(
    id = requireNotNull(id),
    category = category.title,
    categorySlug = category.slug,
    difficulty = difficulty,
    body = body,
    answer = answer,
    followup = followup,
)
