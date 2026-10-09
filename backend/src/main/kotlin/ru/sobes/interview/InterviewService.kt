package ru.sobes.interview

import jakarta.persistence.EntityManager
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import ru.sobes.practice.PracticeService
import ru.sobes.practice.Rating
import ru.sobes.question.QuestionService
import java.time.Instant

data class StartRequest(val categories: List<String> = emptyList(), val total: Int = 10)

data class QuestionDto(
    val questionId: Long,
    val body: String,
    val category: String,
    val categorySlug: String,
    val difficulty: String,
    val index: Int,      // 1-based
    val total: Int,
    val secondsLimit: Int,
)

data class AnswerRequest(
    val questionId: Long,
    val userText: String = "",
    val rating: Rating,
    val secondsSpent: Int = 0,
)

data class SessionStateDto(
    val sessionId: Long,
    val question: QuestionDto?,
    val answeredCount: Int,
    val total: Int,
)

data class ReportItemDto(
    val questionId: Long,
    val body: String,
    val category: String,
    val difficulty: String,
    val rating: String,
    val secondsSpent: Int,
    val userTextLength: Int,
    val referenceAnswer: String,
    val followup: String?,
)

data class ReportDto(
    val sessionId: Long,
    val total: Int,
    val answeredCount: Int,
    val totalSeconds: Int,
    val byRating: Map<String, Int>,
    val weakCategories: List<String>,
    val items: List<ReportItemDto>,
)

/**
 * Мок-интервью (SOBES-27): сессия из N вопросов по категориям,
 * таймер на вопрос, открытый ответ + самооценка; рейтинг пишется в SM-2.
 */
@Service
class InterviewService(
    private val em: EntityManager,
    private val sessions: InterviewSessionRepository,
    private val answers: InterviewAnswerRepository,
    private val questions: QuestionService,
    private val practice: PracticeService,
) {
    companion object {
        const val SECONDS_PER_QUESTION = 180
        val ALLOWED_TOTALS = listOf(5, 10, 15)
    }

    @Transactional
    fun start(userId: Long, req: StartRequest): SessionStateDto {
        // Одна активная сессия на юзера: незакрытую завершаем автоматически.
        sessions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, "IN_PROGRESS")?.let {
            finishInternal(it)
        }
        val total = if (req.total in ALLOWED_TOTALS) req.total else 10
        val cats = req.categories.filter { it.isNotBlank() }.distinct().joinToString(",")
        val session = sessions.save(
            InterviewSession(userId = userId, categories = cats, total = total)
        )
        return state(session)
    }

    @Transactional
    fun state(sessionId: Long, userId: Long): SessionStateDto {
        val session = owned(sessionId, userId)
        return state(session)
    }

    /** Текущий вопрос = первый по порядку без ответа. */
    private fun state(session: InterviewSession): SessionStateDto {
        val ids = pickQuestionIds(session)
        val done = answers.findBySessionIdOrderByAnsweredAtAsc(session.id).map { it.questionId }.toSet()
        val next = ids.firstOrNull { it !in done }
        val question = next?.let { qid ->
            val q = questions.byId(qid)!!
            QuestionDto(
                questionId = q.id,
                body = q.body,
                category = q.category,
                categorySlug = q.categorySlug,
                difficulty = q.difficulty.name,
                index = ids.indexOf(qid) + 1,
                total = ids.size,
                secondsLimit = SECONDS_PER_QUESTION,
            )
        }
        return SessionStateDto(
            sessionId = session.id,
            question = question,
            answeredCount = done.size,
            total = ids.size,
        )
    }

    /** Детерминированный порядок вопросов сессии: по id из выборки на момент старта. */
    private fun pickQuestionIds(session: InterviewSession): List<Long> {
        val cats = session.categories.split(",").filter { it.isNotBlank() }
        val sql = buildString {
            append("select q.id from questions q join categories c on c.id = q.category_id ")
            if (cats.isNotEmpty()) {
                append("where c.slug in (")
                append(cats.joinToString(",") { ":cat${cats.indexOf(it)}" })
                append(") ")
            }
            append("order by q.id ")
        }
        var query = em.createNativeQuery(sql)
        cats.forEachIndexed { i, slug -> query = query.setParameter("cat$i", slug) }
        @Suppress("UNCHECKED_CAST")
        val all = (query.resultList as List<Number>).map { it.toLong() }
        // Стабильная псевдослучайная перестановка от id сессии: порядок фиксируется на старте,
        // но вопросы не идут подряд по id.
        val seed = session.id * 2654435761L
        return all.sortedBy { (it * seed) and 0x7fffffff }.take(session.total)
    }

    @Transactional
    fun answer(sessionId: Long, userId: Long, req: AnswerRequest): SessionStateDto {
        val session = owned(sessionId, userId)
        if (session.status != "IN_PROGRESS") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Сессия уже завершена")
        }
        val ids = pickQuestionIds(session)
        if (req.questionId !in ids) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Вопрос не входит в сессию")
        }
        if (answers.findBySessionIdAndQuestionId(session.id, req.questionId) != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ответ на вопрос уже принят")
        }
        val answeredInOrder = answers.findBySessionIdOrderByAnsweredAtAsc(session.id).map { it.questionId }
        val expected = ids.firstOrNull { it !in answeredInOrder.toSet() }
        if (req.questionId != expected) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ответ вне порядка сессии")
        }

        answers.save(
            InterviewAnswer(
                sessionId = session.id,
                questionId = req.questionId,
                userText = req.userText.take(10_000),
                selfRating = req.rating.name,
                secondsSpent = req.secondsSpent.coerceIn(0, SECONDS_PER_QUESTION + 60),
            )
        )
        // Рейтинг интервью пишется в SM-2 — интервью двигает интервалы повторений.
        practice.answer(userId, req.questionId, req.rating)

        val done = answers.countBySessionId(session.id)
        if (done >= ids.size) finishInternal(session)
        return state(session)
    }

    @Transactional
    fun finish(sessionId: Long, userId: Long): ReportDto {
        val session = owned(sessionId, userId)
        if (session.status != "FINISHED") finishInternal(session)
        return report(session)
    }

    private fun finishInternal(session: InterviewSession) {
        session.status = "FINISHED"
        session.finishedAt = Instant.now()
        sessions.save(session)
    }

    private fun report(session: InterviewSession): ReportDto {
        val rows = answers.findBySessionIdOrderByAnsweredAtAsc(session.id)
        val items = rows.map { a ->
            val q = questions.byId(a.questionId)!!
            ReportItemDto(
                questionId = a.questionId,
                body = q.body,
                category = q.category,
                difficulty = q.difficulty.name,
                rating = a.selfRating ?: "SKIPPED",
                secondsSpent = a.secondsSpent ?: 0,
                userTextLength = a.userText.length,
                referenceAnswer = q.answer,
                followup = q.followup,
            )
        }
        val byRating = items.groupingBy { it.rating }.eachCount()
        val weak = items.filter { it.rating in listOf("AGAIN", "HARD") }
            .groupBy { it.category }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .map { it.first }
        return ReportDto(
            sessionId = session.id,
            total = session.total,
            answeredCount = items.size,
            totalSeconds = items.sumOf { it.secondsSpent },
            byRating = byRating,
            weakCategories = weak,
            items = items,
        )
    }

    private fun owned(sessionId: Long, userId: Long): InterviewSession =
        sessions.findById(sessionId).orElse(null)?.takeIf { it.userId == userId }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Сессия не найдена")
}
