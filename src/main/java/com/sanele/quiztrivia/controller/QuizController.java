package com.sanele.quiztrivia.controller;

import com.sanele.quiztrivia.dto.*;
import com.sanele.quiztrivia.service.QuizService;
import com.sanele.quiztrivia.service.QuizSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final QuizSessionService quizSessionService;

    @PostMapping
    public ResponseEntity<QuizResponse> createQuiz(@Valid @RequestBody QuizRequest request) {
        QuizResponse response = quizService.createQuiz(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<QuizResponse>> getAllQuizzes() {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponse> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PostMapping("/{id}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request
    ) {
        QuestionResponse response = quizService.addQuestion(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{quizId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long quizId,
            @PathVariable Long questionId
    ) {
        quizService.deleteQuestion(quizId, questionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(@PathVariable Long id) {
        return ResponseEntity.ok(quizSessionService.getLeaderboard(id));
    }
}
