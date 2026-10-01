package ru.sobes.practice

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sobes.question.QuestionRepository
import java.time.Instant

/** Сессия практики: очередь due+новых и приём самооценок. */
@Service
class PracticeService(
    private val schedules: ReviewScheduleRepository,
    private val answers: UserAnswerRepository,
    private val questions: QuestionRepository,
) {

    /** Порядок: сначала просроченные (старейшие), потом новые. Лимит — на сессию. */
    @Transactional(readOnly = true)
    fun nextQueue(userId: Long, limit: Int = 20): List<QueueItem> {
        val now = Instant.now()
        val due = schedules.findDue(userId, now, Pageable.ofSize(limit))
            .map { QueueItem(questionId = it.questionId, state = QueueState.DUE, dueAt = it.dueAt) }
        val newLimit = (limit - due.size).coerceAtLeast(0)
        val fresh = if (newLimit > 0) {
            schedules.findNewQuestionIds(userId, Pageable.ofSize(newLimit))
                .map { QueueItem(questionId = it, state = QueueState.NEW, dueAt = null) }
        } else emptyList()
        return due + fresh
    }

    /** Записывает самооценку: история + пересчёт SM-2 + возвращение следующего показа. */
    @Transactional
    fun answer(userId: Long, questionId: Long, rating: Rating): AnswerResult {
        val now = Instant.now()
        val state = schedules.findById(ReviewScheduleId(userId, questionId))
            .orElseGet { ReviewSchedule(userId = userId, questionId = questionId) }
        val current = ReviewState(easeFactor = state.easeFactor, intervalDays = state.intervalDays, repetitions = state.repetitions)
        val scheduled = sm2(current, rating, now)

        state.easeFactor = scheduled.easeFactor
        state.intervalDays = scheduled.intervalDays
        state.repetitions = scheduled.repetitions
        state.dueAt = scheduled.dueAt
        state.lastReviewedAt = now
        schedules.save(state)

        answers.save(
            UserAnswer(userId = userId, questionId = questionId, selfRating = rating.name)
        )

        return AnswerResult(
            questionId = questionId,
            rating = rating,
            intervalDays = scheduled.intervalDays,
            dueAt = scheduled.dueAt,
        )
    }

    /** Сводка для UI: сколько всего, сколько к повторению, сколько новых. */
    @Transactional(readOnly = true)
    fun summary(userId: Long): PracticeSummary {
        val now = Instant.now()
        val dueCount = schedules.countDue(userId, now)
        val started = schedules.countStarted(userId)
        val total = questions.count()
        return PracticeSummary(
            dueCount = dueCount,
            newCount = (total - started).coerceAtLeast(0),
            answeredTotal = answers.countByUserId(userId),
        )
    }
}

enum class QueueState { DUE, NEW }

data class QueueItem(
    val questionId: Long,
    val state: QueueState,
    val dueAt: Instant?,
)

data class AnswerResult(
    val questionId: Long,
    val rating: Rating,
    val intervalDays: Int,
    val dueAt: Instant,
)

data class PracticeSummary(
    val dueCount: Int,
    val newCount: Long,
    val answeredTotal: Long,
)
