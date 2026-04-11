package com.sanele.quiztrivia.dto;

import com.sanele.quiztrivia.enums.QuestionType;

import java.util.List;

public record QuestionResponse(
        Long id,
        String questionText,
        QuestionType questionType,
        int points,
        List<AnswerOptionDto> answerOptions
) {}
