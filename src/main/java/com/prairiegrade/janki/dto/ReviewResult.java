package com.prairiegrade.janki.dto;

import java.time.LocalDateTime;

/**
 * Result of a card review operation.
 * Contains the updated state and scheduling information after rating a card.
 */
public record ReviewResult(
    Long cardId,
    String newState,
    int intervalDays,
    LocalDateTime nextReviewDate,
    double easeFactor
) {}
