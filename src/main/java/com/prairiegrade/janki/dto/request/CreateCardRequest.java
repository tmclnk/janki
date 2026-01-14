package com.prairiegrade.janki.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new card.
 */
public record CreateCardRequest(
    @NotBlank(message = "Card front is required")
    @Size(min = 1, max = 1000, message = "Card front must be between 1 and 1000 characters")
    String front,

    @NotBlank(message = "Card back is required")
    @Size(min = 1, max = 1000, message = "Card back must be between 1 and 1000 characters")
    String back
) {}
