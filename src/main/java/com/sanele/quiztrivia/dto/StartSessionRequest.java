package com.sanele.quiztrivia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StartSessionRequest(

        @NotNull(message = "Quiz ID is required")
        Long quizId,

        @NotBlank(message = "Player name is required")
        @Size(max = 100, message = "Player name must not exceed 100 characters")
        String playerName
) {}
