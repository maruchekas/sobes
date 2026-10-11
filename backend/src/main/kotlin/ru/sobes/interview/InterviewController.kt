package ru.sobes.interview

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.sobes.auth.requireUserId

@RestController
@RequestMapping("/api/v1/interview")
class InterviewController(
    private val service: InterviewService,
) {
    @PostMapping("/start")
    fun start(request: HttpServletRequest, @RequestBody body: StartRequest): SessionStateDto =
        service.start(request.requireUserId(), body)

    @GetMapping("/{sessionId}/state")
    fun state(request: HttpServletRequest, @PathVariable sessionId: Long): SessionStateDto =
        service.state(sessionId, request.requireUserId())

    @PostMapping("/{sessionId}/answer")
    fun answer(
        request: HttpServletRequest,
        @PathVariable sessionId: Long,
        @RequestBody body: AnswerRequest,
    ): SessionStateDto = service.answer(sessionId, request.requireUserId(), body)

    @PostMapping("/{sessionId}/finish")
    fun finish(request: HttpServletRequest, @PathVariable sessionId: Long): ReportDto =
        service.finish(sessionId, request.requireUserId())
}
