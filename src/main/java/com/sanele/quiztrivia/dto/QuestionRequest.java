package com.sanele.quiztrivia.dto;

import com.sanele.quiztrivia.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record QuestionRequest(

        @NotBlank(message = "Question text is required")
        String questionText,

        @NotNull(message = "Question type is required")
        QuestionType questionType,

        @NotBlank(message = "Correct answer is required")
        String correctAnswer,

        @Min(value = 1, message = "Points must be at least 1")
        @Max(value = 100, message = "Points must not exceed 100")
        int points,

        @Valid
        List<AnswerOptionDto> answerOptions
) {}
