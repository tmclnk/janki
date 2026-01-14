package com.prairiegrade.janki.dto.request;

import com.prairiegrade.janki.service.SM2Service;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for rating a card during study session.
 */
public record RateCardRequest(
    @NotNull(message = "Rating is required")
    SM2Service.UserRating rating
) {}
