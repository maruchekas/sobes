package ru.sobes.question

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuestionService(
    private val questions: QuestionRepository,
    private val categories: CategoryRepository,
) {

    fun list(categorySlug: String?, difficulty: Difficulty?, pageable: Pageable): Page<QuestionSummary> =
        when {
            categorySlug != null && difficulty != null ->
                questions.findByCategorySlugAndDifficulty(categorySlug, difficulty, pageable)

            categorySlug != null -> questions.findByCategorySlug(categorySlug, pageable)
            difficulty != null -> questions.findByDifficulty(difficulty, pageable)
            else -> questions.findAll(pageable)
        }.map(Question::toSummary)

    fun byId(id: Long): QuestionDetails? = questions.findById(id).orElse(null)?.toDetails()

    fun categories(): List<CategoryView> =
        categories.findAllByOrderBySortOrderAscTitleAsc().map { category ->
            CategoryView(
                slug = category.slug,
                title = category.title,
                questions = questions.countByCategory(category),
            )
        }
}
