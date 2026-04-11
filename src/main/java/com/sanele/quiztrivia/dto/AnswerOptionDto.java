package com.sanele.quiztrivia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AnswerOptionDto(

        Long id,

        @NotBlank(message = "Option text is required")
        String optionText,

        @NotBlank(message = "Option label is required")
        @Pattern(regexp = "^[A-D]$", message = "Option label must be A, B, C, or D")
        String optionLabel
) {}
