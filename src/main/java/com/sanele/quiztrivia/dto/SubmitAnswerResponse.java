package com.sanele.quiztrivia.dto;

public record SubmitAnswerResponse(
        Long questionId,
        String submittedAnswer,
        boolean correct,
        String correctAnswer,
        int pointsEarned,
        int currentScore,
        int questionsAnswered,
        int totalQuestions
) {}
