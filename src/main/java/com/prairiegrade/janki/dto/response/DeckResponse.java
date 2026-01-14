package com.prairiegrade.janki.dto.response;

import com.prairiegrade.janki.domain.Deck;

import java.time.LocalDateTime;

/**
 * Response DTO for Deck entity.
 * Prevents exposure of internal JPA implementation details.
 */
public record DeckResponse(
    Long id,
    String name,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Factory method to create DeckResponse from Deck entity.
     */
    public static DeckResponse from(Deck deck) {
        return new DeckResponse(
            deck.getId(),
            deck.getName(),
            deck.getDescription(),
            deck.getCreatedAt(),
            deck.getUpdatedAt()
        );
    }
}
