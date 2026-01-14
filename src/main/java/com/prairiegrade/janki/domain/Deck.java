package com.prairiegrade.janki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Deck entity - represents a collection of flashcards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("decks")
public class Deck {

    @Id
    private Long id;

    private String name;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Builder.Default
    private Integer newCardsPerDay = 20;

    @Builder.Default
    private Integer reviewCardsPerDay = 200;
}
