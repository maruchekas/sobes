package ru.sobes.admin

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/** Управление банком вопросов. Только ADMIN (env ADMIN_TELEGRAM_IDS). */
@RestController
@RequestMapping("/api/v1/admin")
class AdminController(
    private val guard: AdminGuard,
    private val service: AdminService,
) {
    @GetMapping("/questions")
    fun questions(
        request: HttpServletRequest,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?,
    ): Map<String, Any> {
        guard.requireAdmin(request)
        val result = service.listQuestions(page, size, search)
        return mapOf(
            "content" to result.content,
            "totalElements" to result.totalElements,
            "totalPages" to result.totalPages,
        )
    }

    @PostMapping("/questions")
    fun upsertQuestion(request: HttpServletRequest, @RequestBody body: QuestionUpsertRequest): QuestionAdminDto {
        guard.requireAdmin(request)
        return service.upsertQuestion(body)
    }

    @DeleteMapping("/questions/{id}")
    fun deleteQuestion(request: HttpServletRequest, @PathVariable id: Long): Map<String, String> {
        guard.requireAdmin(request)
        service.deleteQuestion(id)
        return mapOf("status" to "deleted")
    }

    @GetMapping("/categories")
    fun categories(request: HttpServletRequest): List<CategoryDto> {
        guard.requireAdmin(request)
        return service.listCategories()
    }

    @PostMapping("/categories")
    fun upsertCategory(request: HttpServletRequest, @RequestBody body: CategoryDto): CategoryDto {
        guard.requireAdmin(request)
        return service.upsertCategory(body)
    }
}
