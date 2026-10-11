package ru.sobes.interview

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "interview_sessions")
class InterviewSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long = 0,

    @Column(name = "status", nullable = false, length = 16)
    var status: String = "IN_PROGRESS",

    /** slug'и категорий через запятую; пусто = все. */
    @Column(name = "categories", nullable = false)
    var categories: String = "",

    @Column(name = "total", nullable = false)
    var total: Int = 0,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "finished_at")
    var finishedAt: Instant? = null,
)

@Entity
@Table(name = "interview_answers")
class InterviewAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "session_id", nullable = false)
    val sessionId: Long = 0,

    @Column(name = "question_id", nullable = false)
    val questionId: Long = 0,

    @Column(name = "user_text", nullable = false)
    var userText: String = "",

    @Column(name = "self_rating", length = 8)
    var selfRating: String? = null,

    @Column(name = "seconds_spent")
    var secondsSpent: Int? = null,

    @Column(name = "answered_at", nullable = false)
    val answeredAt: Instant = Instant.now(),
)
