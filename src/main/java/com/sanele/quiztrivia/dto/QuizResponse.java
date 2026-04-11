package com.sanele.quiztrivia.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizResponse(
        Long id,
        String title,
        String description,
        LocalDateTime createdAt,
        int questionCount,
        List<QuestionResponse> questions
) {}
