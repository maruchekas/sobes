package ru.sobes.practice

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant

/** Составной ключ пары пользователь/вопрос. */
data class ReviewScheduleId(
    val userId: Long = 0,
    val questionId: Long = 0,
) : Serializable

@Entity
@IdClass(ReviewScheduleId::class)
@Table(name = "review_schedule")
class ReviewSchedule(
    @Id
    @Column(name = "user_id")
    val userId: Long = 0,

    @Id
    @Column(name = "question_id")
    val questionId: Long = 0,

    @Column(name = "ease_factor", nullable = false, columnDefinition = "numeric")
    var easeFactor: Double = 2.5,

    @Column(name = "interval_days", nullable = false)
    var intervalDays: Int = 0,

    @Column(name = "repetitions", nullable = false)
    var repetitions: Int = 0,

    @Column(name = "due_at", nullable = false)
    var dueAt: Instant = Instant.now(),

    @Column(name = "last_reviewed_at")
    var lastReviewedAt: Instant? = null,
)
