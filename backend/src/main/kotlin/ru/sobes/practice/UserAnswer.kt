package ru.sobes.practice

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "user_answers")
class UserAnswer(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "question_id", nullable = false)
    val questionId: Long,

    @Column(name = "self_rating", nullable = false, length = 16)
    val selfRating: String,

    @Column(name = "answered_at", nullable = false)
    val answeredAt: Instant = Instant.now(),
)
