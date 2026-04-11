package com.sanele.quiztrivia.controller;

import com.sanele.quiztrivia.dto.*;
import com.sanele.quiztrivia.service.QuizSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizSessionService quizSessionService;

    @PostMapping("/start")
    public ResponseEntity<StartSessionResponse> startSession(
            @Valid @RequestBody StartSessionRequest request
    ) {
        StartSessionResponse response = quizSessionService.startSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{sessionId}/answer")
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        SubmitAnswerResponse response = quizSessionService.submitAnswer(sessionId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<CompleteSessionResponse> completeSession(@PathVariable Long sessionId) {
        CompleteSessionResponse response = quizSessionService.completeSession(sessionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionDetailsResponse> getSessionDetails(@PathVariable Long sessionId) {
        return ResponseEntity.ok(quizSessionService.getSessionDetails(sessionId));
    }
}
