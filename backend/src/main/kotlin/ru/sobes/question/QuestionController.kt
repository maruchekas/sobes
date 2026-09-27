package ru.sobes.question

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class QuestionController(private val service: QuestionService) {

    @GetMapping("/questions")
    fun questions(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) difficulty: Difficulty?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<QuestionSummary> = service.list(category, difficulty, pageable)

    @GetMapping("/questions/{id}")
    fun question(@PathVariable id: Long): ResponseEntity<QuestionDetails> {
        val details = service.byId(id)
        return if (details != null) ResponseEntity.ok(details) else ResponseEntity.notFound().build()
    }

    @GetMapping("/categories")
    fun categories(): List<CategoryView> = service.categories()
}
