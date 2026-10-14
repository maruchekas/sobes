package ru.sobes.admin

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.sobes.question.Category
import ru.sobes.question.CategoryRepository
import ru.sobes.question.Difficulty
import ru.sobes.question.Question
import ru.sobes.question.QuestionRepository

data class QuestionAdminDto(
    val id: Long,
    val categorySlug: String,
    val categoryTitle: String,
    val difficulty: String,
    val body: String,
    val answer: String,
    val followup: String?,
)

data class QuestionUpsertRequest(
    val id: Long? = null,
    val categorySlug: String,
    val difficulty: String,
    val body: String,
    val answer: String,
    val followup: String? = null,
)

data class CategoryDto(
    val slug: String,
    val title: String,
    val sortOrder: Int,
)

@Service
class AdminService(
    private val questions: QuestionRepository,
    private val categories: CategoryRepository,
) {
    @Transactional(readOnly = true)
    fun listQuestions(page: Int, size: Int, search: String?): Page<QuestionAdminDto> {
        val pageable: Pageable = PageRequest.of(page.coerceIn(0, 10_000), size.coerceIn(1, 100))
        val raw: Page<Question> = if (search.isNullOrBlank()) {
            questions.findAll(pageable)
        } else {
            questions.findByBodyContainingIgnoreCase(search, pageable)
        }
        return raw.map { q ->
            QuestionAdminDto(
                id = q.id!!,
                categorySlug = q.category.slug,
                categoryTitle = q.category.title,
                difficulty = q.difficulty.name,
                body = q.body,
                answer = q.answer,
                followup = q.followup,
            )
        }
    }

    @Transactional
    fun upsertQuestion(req: QuestionUpsertRequest): QuestionAdminDto {
        val cat: Category = categories.findBySlug(req.categorySlug)
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Категория '${req.categorySlug}' не найдена")
        val difficulty = runCatching { Difficulty.valueOf(req.difficulty) }
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Сложность JUNIOR/MIDDLE/SENIOR") }
        if (req.body.isBlank() || req.answer.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "body и answer обязательны")
        }
        val entity: Question = req.id
            ?.let { questions.findById(it).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Вопрос ${it} не найден") } }
            ?: Question(category = cat, difficulty = difficulty, body = "", answer = "")
        entity.category = cat
        entity.difficulty = difficulty
        entity.body = req.body.trim()
        entity.answer = req.answer.trim()
        entity.followup = req.followup?.trim()?.ifBlank { null }
        val saved = questions.save(entity)
        return QuestionAdminDto(
            id = saved.id!!,
            categorySlug = cat.slug,
            categoryTitle = cat.title,
            difficulty = saved.difficulty.name,
            body = saved.body,
            answer = saved.answer,
            followup = saved.followup,
        )
    }

    @Transactional
    fun deleteQuestion(id: Long) {
        if (!questions.existsById(id)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Вопрос $id не найден")
        }
        questions.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun listCategories(): List<CategoryDto> =
        categories.findAllByOrderBySortOrderAscTitleAsc().map { CategoryDto(it.slug, it.title, it.sortOrder) }

    @Transactional
    fun upsertCategory(dto: CategoryDto): CategoryDto {
        if (dto.slug.isBlank() || dto.title.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "slug и title обязательны")
        }
        val entity = categories.findBySlug(dto.slug) ?: Category(slug = dto.slug, title = dto.title)
        entity.title = dto.title.trim()
        entity.sortOrder = dto.sortOrder
        val saved = categories.save(entity)
        return CategoryDto(saved.slug, saved.title, saved.sortOrder)
    }
}
