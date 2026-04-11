package com.sanele.quiztrivia.dto;

import com.sanele.quiztrivia.enums.SessionStatus;

import java.time.LocalDateTime;

public record StartSessionResponse(
        Long sessionId,
        Long quizId,
        String quizTitle,
        String playerName,
        int totalQuestions,
        LocalDateTime startedAt,
        SessionStatus status
) {}
