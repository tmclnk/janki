package com.prairiegrade.janki.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ReviewRecord entity - stores SM-2 algorithm state for each card.
 * One-to-one relationship with Card entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "review_records")
public class ReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id", unique = true)
    private Long cardId;  // Foreign key to Card (unique)

    @Builder.Default
    @Column(name = "ease_factor")
    private Double easeFactor = 2.5;

    @Builder.Default
    private Integer repetitions = 0;

    @Builder.Default
    @Column(name = "interval_days")
    private Integer intervalDays = 0;

    @Column(name = "next_review_date")
    private LocalDateTime nextReviewDate;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
