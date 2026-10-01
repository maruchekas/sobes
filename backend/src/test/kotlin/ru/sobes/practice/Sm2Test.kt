package ru.sobes.practice

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Юнит-тесты SM-2: чистая функция, проверяем математику алгоритма
 * по формулам из оригинальной статьи SuperMemo 2 и таблицам Anki.
 */
class Sm2Test {

    private val t0 = Instant.parse("2026-10-01T12:00:00Z")

    @Test
    fun `первый GOOD начинает серию с интервала 1 день`() {
        val r = sm2(ReviewState(), Rating.GOOD, t0)
        assertEquals(2.5, r.easeFactor, 1e-9) // q=4: EF + 0 = без изменений
        assertEquals(1, r.intervalDays)
        assertEquals(1, r.repetitions)
        assertEquals(t0.plusSeconds(86_400), r.dueAt)
    }

    @Test
    fun `второй GOOD дает 6 дней`() {
        val r = sm2(ReviewState(intervalDays = 1, repetitions = 1), Rating.GOOD, t0)
        assertEquals(6, r.intervalDays)
        assertEquals(2, r.repetitions)
    }

    @Test
    fun `третий GOOD умножает интервал на ease factor`() {
        val r = sm2(ReviewState(intervalDays = 6, repetitions = 2), Rating.GOOD, t0)
        assertEquals(15, r.intervalDays) // 6 * 2.5 = 15
        assertEquals(3, r.repetitions)
    }

    @Test
    fun `EASY повышает ease factor`() {
        val r = sm2(ReviewState(intervalDays = 6, repetitions = 2), Rating.EASY, t0)
        assertEquals(2.6, r.easeFactor, 1e-9) // EF + 0.1
        assertEquals(16, r.intervalDays)     // round(6 * 2.6) = 15.6 -> 16
    }

    @Test
    fun `HARD понижает ease factor`() {
        val r = sm2(ReviewState(), Rating.HARD, t0)
        assertEquals(2.36, r.easeFactor, 1e-9) // EF - 0.14
        assertEquals(1, r.intervalDays)
    }

    @Test
    fun `AGAIN сбрасывает серию и интервал на 1 день`() {
        val r = sm2(ReviewState(easeFactor = 2.2, intervalDays = 30, repetitions = 5), Rating.AGAIN, t0)
        assertEquals(0, r.repetitions)
        assertEquals(1, r.intervalDays)
        assertEquals(1.86, r.easeFactor, 1e-9) // 2.2 - 0.56 + 0.1... : EF + (0.1 - 3*(0.08+3*0.02)) = 2.2 - 0.44
        assertEquals(t0.plusSeconds(86_400), r.dueAt)
    }

    @Test
    fun `ease factor никогда не падает ниже 130`() {
        var state = ReviewState(easeFactor = 1.4)
        repeat(10) {
            state = ReviewState(easeFactor = sm2(state, Rating.AGAIN, t0).easeFactor)
        }
        assertTrue(state.easeFactor >= 1.3, "EF ниже 1.3: ${state.easeFactor}")
    }

    @Test
    fun `после AGAIN серия начинается заново с 1 дня`() {
        val failed = sm2(ReviewState(easeFactor = 2.5, intervalDays = 30, repetitions = 5), Rating.AGAIN, t0)
        val recovered = sm2(
            ReviewState(failed.easeFactor, failed.intervalDays, failed.repetitions),
            Rating.GOOD, t0
        )
        assertEquals(1, recovered.intervalDays)
        assertEquals(1, recovered.repetitions)
    }
}
