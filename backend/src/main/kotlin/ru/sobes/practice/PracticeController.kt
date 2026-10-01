package ru.sobes.practice

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.NotNull
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.sobes.auth.requireUserId
import ru.sobes.question.QuestionService

/** Практика на карточках: очередь и самооценки. */
@RestController
@RequestMapping("/api/v1/practice")
class PracticeController(
    private val practice: PracticeService,
    private val questionService: QuestionService,
) {

    /** Очередь на сессию: due-карточки + новые. limit по умолчанию 20. */
    @GetMapping("/queue")
    fun queue(request: HttpServletRequest, @RequestParam(defaultValue = "20") limit: Int): List<QueueCard> =
        practice.nextQueue(request.requireUserId(), limit.coerceIn(1, 50)).map { item ->
            val details = questionService.byId(item.questionId)
                ?: throw IllegalArgumentException("Вопрос ${item.questionId} исчез из каталога")
            QueueCard(
                questionId = item.questionId,
                state = item.state,
                dueAt = item.dueAt,
                category = details.category,
                difficulty = details.difficulty.name,
                body = details.body,
            )
        }

    /** Самооценка ответа: AGAIN/HARD/GOOD/EASY. */
    @PostMapping("/answer")
    fun answer(request: HttpServletRequest, @RequestBody body: AnswerRequest): AnswerResult {
        require(body.rating != null) { "rating обязателен" }
        return practice.answer(request.requireUserId(), body.questionId, body.rating)
    }

    /** Сводка: к повторению / новых / всего ответов. */
    @GetMapping("/summary")
    fun summary(request: HttpServletRequest): PracticeSummary = practice.summary(request.requireUserId())
}

data class AnswerRequest(
    val questionId: Long,
    @field:NotNull val rating: Rating?,
)

data class QueueCard(
    val questionId: Long,
    val state: QueueState,
    val dueAt: java.time.Instant?,
    val category: String,
    val difficulty: String,
    val body: String,
)
