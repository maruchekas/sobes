package ru.sobes.dashboard

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.sobes.practice.ReviewScheduleRepository
import ru.sobes.practice.UserAnswerRepository
import ru.sobes.question.QuestionRepository
import ru.sobes.settings.UserSettingsRepository
import java.time.LocalDate

/** Дашборд: слабые темы, streak, готовность к собеседованию. */
@Service
class DashboardService(
    private val queries: DashboardQueryRepository,
    private val settingsRepository: UserSettingsRepository,
    private val answersRepository: UserAnswerRepository,
    private val schedules: ReviewScheduleRepository,
    private val questions: QuestionRepository,
) {

    @Transactional(readOnly = true)
    fun dashboard(userId: Long): DashboardDto {
        val tz = settingsRepository.findById(userId).orElse(null)?.timezone ?: "Europe/Moscow"

        val categories = queries.progressByCategory(userId).map {
            CategoryProgress(
                slug = it.slug,
                title = it.title,
                answered = it.answered,
                confidencePercent = (it.confidence * 100).toInt().coerceIn(0, 100),
            )
        }

        val streakDays = countStreak(queries.answersByDay(userId, tz).map { it.day }.toSet())
        val answeredTotal = answersRepository.countByUserId(userId)
        val started = schedules.countStarted(userId)
        val totalQuestions = questions.count()

        return DashboardDto(
            answeredTotal = answeredTotal,
            streakDays = streakDays,
            weakestTopics = categories.filter { it.confidencePercent < 70 }.take(5),
            categoryProgress = categories,
            readinessPercent = readiness(categories, started, totalQuestions),
        )
    }

    /**
     * Streak: дни подряд с ответами до сегодня/вчера.
     * Сегодня не обязательно отвечать — streak не рвётся до конца дня.
     */
    private fun countStreak(days: Set<LocalDate>): Int {
        if (days.isEmpty()) return 0
        val today = LocalDate.now()
        var cursor = if (days.contains(today)) today else today.minusDays(1)
        var streak = 0
        while (days.contains(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    /**
     * Готовность к собеседованию: покрытие банка (вес 60%) * средняя уверенность (вес 40%).
     * Честная композитная метрика: не растёт от пустых ответов.
     */
    private fun readiness(categories: List<CategoryProgress>, started: Long, total: Long): Int {
        if (total == 0L || categories.isEmpty()) return 0
        val coverage = started.toDouble() / total
        val avgConfidence = categories.map { it.confidencePercent }.average() / 100.0
        return ((coverage * 0.6 + avgConfidence * 0.4) * 100).toInt().coerceIn(0, 100)
    }
}

data class DashboardDto(
    val answeredTotal: Long,
    val streakDays: Int,
    val weakestTopics: List<CategoryProgress>,
    val categoryProgress: List<CategoryProgress>,
    val readinessPercent: Int,
)

data class CategoryProgress(
    val slug: String,
    val title: String,
    val answered: Long,
    val confidencePercent: Int,
)
