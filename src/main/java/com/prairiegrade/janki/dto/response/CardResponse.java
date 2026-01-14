package com.prairiegrade.janki.dto.response;

import com.prairiegrade.janki.domain.Card;

import java.time.LocalDateTime;

/**
 * Response DTO for Card entity.
 * Prevents exposure of internal JPA implementation details.
 */
public record CardResponse(
    Long id,
    Long deckId,
    String front,
    String back,
    String state,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Factory method to create CardResponse from Card entity.
     */
    public static CardResponse from(Card card) {
        return new CardResponse(
            card.getId(),
            card.getDeckId(),
            card.getFront(),
            card.getBack(),
            card.getState(),
            card.getCreatedAt(),
            card.getUpdatedAt()
        );
    }
}
