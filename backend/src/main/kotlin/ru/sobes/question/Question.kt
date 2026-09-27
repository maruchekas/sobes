package ru.sobes.question

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/** Уровень сложности вопроса. */
enum class Difficulty {
    JUNIOR,
    MIDDLE,
    SENIOR,
}

/** Тематика вопроса: Java Core, Kotlin, Spring, базы данных и так далее. */
@Entity
@Table(name = "categories")
class Category(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 64)
    var slug: String,

    @Column(nullable = false, length = 128)
    var title: String,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
)

/** Вопрос собеседования с эталонным ответом и уточняющим вопросом. */
@Entity
@Table(name = "questions")
class Question(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var difficulty: Difficulty,

    @Column(nullable = false, columnDefinition = "text")
    var body: String,

    @Column(nullable = false, columnDefinition = "text")
    var answer: String,

    @Column(columnDefinition = "text")
    var followup: String? = null,
)
