package ru.sobes.practice

import java.time.Duration
import java.time.Instant

/**
 * SM-2 (SuperMemo 2) — классический алгоритм интервальных повторений,
 * адаптированный под 4-балльную самооценку Anki-стиля.
 *
 * Отображение оценок в q (0..5) оригинального SM-2:
 * AGAIN -> 2 (провал: сброс серии), HARD -> 3, GOOD -> 4, EASY -> 5.
 */
enum class Rating(val q: Int) {
    AGAIN(2), HARD(3), GOOD(4), EASY(5)
}

/** Входное состояние пары пользователь/вопрос (из review_schedule). */
data class ReviewState(
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
)

/** Результат пересчёта: новое состояние + момент следующего показа. */
data class ScheduledReview(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Instant,
)

/** Чистая функция — без зависимостей, детерминирована (время передаём снаружи). */
fun sm2(state: ReviewState, rating: Rating, now: Instant = Instant.now()): ScheduledReview {
    val q = rating.q
    require(q in 0..5) { "q должен быть 0..5" }

    // EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02)), минимум 1.3
    val rawEase = state.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
    val ease = (rawEase).coerceAtLeast(1.3)

    return if (q < 3) {
        // Провал: серия в ноль, интервал сбрасывается на 1 день.
        ScheduledReview(ease, intervalDays = 1, repetitions = 0, dueAt = now + Duration.ofDays(1))
    } else {
        val repetitions = state.repetitions + 1
        val interval = when (repetitions) {
            1 -> 1
            2 -> 6
            else -> Math.round(state.intervalDays * ease).toInt().coerceAtLeast(1)
        }
        ScheduledReview(ease, interval, repetitions, dueAt = now + Duration.ofDays(interval.toLong()))
    }
}
