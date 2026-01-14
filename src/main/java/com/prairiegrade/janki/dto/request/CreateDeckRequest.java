package com.prairiegrade.janki.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new deck.
 */
public record CreateDeckRequest(
    @NotBlank(message = "Deck name is required")
    @Size(min = 1, max = 100, message = "Deck name must be between 1 and 100 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description
) {}
