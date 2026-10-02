package ru.sobes.daily

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.time.LocalDate

data class DailyQuestionId(
    val userId: Long = 0,
    val forDate: LocalDate? = null,
) : Serializable

@Entity
@Table(name = "daily_questions")
@IdClass(DailyQuestionId::class)
class DailyQuestion(
    @Id
    @Column(name = "user_id")
    val userId: Long = 0,

    @Id
    @Column(name = "for_date")
    val forDate: LocalDate? = null,

    @Column(name = "question_id", nullable = false)
    val questionId: Long = 0,

    @Column(name = "sent_at", nullable = false)
    val sentAt: Instant = Instant.now(),
)
