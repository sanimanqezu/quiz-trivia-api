package com.sanele.quiztrivia.dto;

import java.time.LocalDateTime;

public record LeaderboardEntry(
        int rank,
        String playerName,
        int score,
        int totalQuestions,
        double percentage,
        LocalDateTime completedAt
) {}
