package com.sanele.quiztrivia.dto;

import com.sanele.quiztrivia.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SessionDetailsResponse(
        Long sessionId,
        Long quizId,
        String quizTitle,
        String playerName,
        int score,
        int totalQuestions,
        int answeredQuestions,
        SessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<SessionAnswerDto> answers
) {}
