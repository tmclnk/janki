package com.prairiegrade.janki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Card entity - represents an individual flashcard with front and back.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("cards")
public class Card {

    @Id
    private Long id;

    private Long deckId;

    private String front;

    private String back;

    @Builder.Default
    private String state = "NEW";  // NEW, LEARNING, REVIEW

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
