package com.sanele.quiztrivia.dto;

public record SessionAnswerDto(
        Long questionId,
        String questionText,
        String submittedAnswer,
        String correctAnswer,
        boolean correct,
        int pointsEarned
) {}
