package com.sanele.quiztrivia.dto;

import com.sanele.quiztrivia.enums.SessionStatus;

import java.time.LocalDateTime;

public record CompleteSessionResponse(
        Long sessionId,
        String playerName,
        String quizTitle,
        int finalScore,
        int totalQuestions,
        int correctAnswers,
        double percentage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        SessionStatus status
) {}
