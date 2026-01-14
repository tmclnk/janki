package com.prairiegrade.janki.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * ReviewRecord entity - stores SM-2 algorithm state for each card.
 * One-to-one relationship with Card entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("review_records")
public class ReviewRecord {

    @Id
    private Long id;

    private Long cardId;  // Foreign key to Card (unique)

    @Builder.Default
    private Double easeFactor = 2.5;

    @Builder.Default
    private Integer repetitions = 0;

    @Builder.Default
    private Integer intervalDays = 0;

    private LocalDateTime nextReviewDate;

    private LocalDateTime lastReviewedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
