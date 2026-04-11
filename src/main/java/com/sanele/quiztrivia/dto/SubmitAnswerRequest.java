package com.sanele.quiztrivia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(

        @NotNull(message = "Question ID is required")
        Long questionId,

        @NotBlank(message = "Answer is required")
        String answer
) {}
