package com.prairiegrade.janki.dto.response;

import com.prairiegrade.janki.service.StudyService;

import java.time.LocalDateTime;

/**
 * Response DTO for StudyCard (combines Card + ReviewRecord for study sessions).
 */
public record StudyCardResponse(
    Long cardId,
    Long deckId,
    String front,
    String back,
    String state,
    Double easeFactor,
    Integer repetitions,
    Integer intervalDays,
    LocalDateTime nextReviewDate,
    LocalDateTime lastReviewedAt
) {
    /**
     * Factory method to create StudyCardResponse from StudyCard.
     */
    public static StudyCardResponse from(StudyService.StudyCard studyCard) {
        return new StudyCardResponse(
            studyCard.card().getId(),
            studyCard.card().getDeckId(),
            studyCard.card().getFront(),
            studyCard.card().getBack(),
            studyCard.card().getState(),
            studyCard.reviewRecord().getEaseFactor(),
            studyCard.reviewRecord().getRepetitions(),
            studyCard.reviewRecord().getIntervalDays(),
            studyCard.reviewRecord().getNextReviewDate(),
            studyCard.reviewRecord().getLastReviewedAt()
        );
    }
}
