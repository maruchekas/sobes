package ru.sobes.question

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findAllByOrderBySortOrderAscTitleAsc(): List<Category>
    fun findBySlug(slug: String): Category?
}

interface QuestionRepository : JpaRepository<Question, Long> {
    fun findByCategorySlug(slug: String, pageable: Pageable): Page<Question>
    fun findByDifficulty(difficulty: Difficulty, pageable: Pageable): Page<Question>
    fun findByCategorySlugAndDifficulty(slug: String, difficulty: Difficulty, pageable: Pageable): Page<Question>
    fun countByCategory(category: Category): Long
}
