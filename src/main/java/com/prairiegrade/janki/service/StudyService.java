package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.ReviewRecord;
import com.prairiegrade.janki.dto.ReviewResult;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.ReviewRecordRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for managing spaced repetition study sessions.
 *
 * <p>This service coordinates card retrieval, review scheduling, and rating operations
 * following the SuperMemo 2 algorithm. It handles both new cards (never studied) and
 * due cards (scheduled for review) within a deck.</p>
 *
 * <h3>Study Workflow:</h3>
 * <ol>
 *   <li>getDueCards() - Retrieve cards scheduled for review now</li>
 *   <li>getNewCards() - Retrieve unstudied cards and initialize their review records</li>
 *   <li>rateCard() - Process user rating, update scheduling via SM-2, persist changes</li>
 * </ol>
 *
 * <p>All methods use reactive wrappers (Mono.fromCallable with boundedElastic scheduler)
 * to integrate blocking JDBC operations into a reactive WebFlux pipeline.</p>
 */
@Service
public class StudyService {

    private final CardRepository cardRepository;
    private final ReviewRecordRepository reviewRecordRepository;
    private final SM2Service sm2Service;

    /**
     * Constructs StudyService with required dependencies.
     *
     * @param cardRepository Repository for card persistence
     * @param reviewRecordRepository Repository for review record persistence
     * @param sm2Service Service implementing SuperMemo 2 algorithm
     */
    public StudyService(
            CardRepository cardRepository,
            ReviewRecordRepository reviewRecordRepository,
            SM2Service sm2Service
    ) {
        this.cardRepository = cardRepository;
        this.reviewRecordRepository = reviewRecordRepository;
        this.sm2Service = sm2Service;
    }

    /**
     * Inner record combining a card with its review record for study operations.
     */
    public record StudyCard(Card card, ReviewRecord reviewRecord) {}

    /**
     * Retrieves cards that are due for review in a specific deck.
     *
     * <p>Uses ReviewRecordRepository.findDueReviews() to query cards whose
     * nextReviewDate is on or before the current time. Results are joined
     * with their corresponding Card entities.</p>
     *
     * @param deckId ID of the deck to query
     * @param limit Maximum number of due cards to return
     * @return Flux of StudyCard records containing due cards and their review data
     */
    public Flux<StudyCard> getDueCards(Long deckId, int limit) {
        return Mono.fromCallable(() -> {
            LocalDateTime now = LocalDateTime.now();
            List<ReviewRecord> dueReviews = reviewRecordRepository.findDueReviews(deckId, now, limit);

            return dueReviews.stream()
                    .map(reviewRecord -> {
                        Card card = cardRepository.findById(reviewRecord.getCardId())
                                .orElseThrow(() -> new RuntimeException(
                                        "Card not found for review record: " + reviewRecord.getCardId()));
                        return new StudyCard(card, reviewRecord);
                    })
                    .toList();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    /**
     * Retrieves new (never studied) cards from a specific deck.
     *
     * <p>Queries cards with state=NEW and creates initial ReviewRecord entities
     * for them if they don't already exist. This initializes the spaced repetition
     * tracking for first-time study.</p>
     *
     * @param deckId ID of the deck to query
     * @param limit Maximum number of new cards to return
     * @return Flux of StudyCard records containing new cards with initialized review data
     */
    public Flux<StudyCard> getNewCards(Long deckId, int limit) {
        return Mono.fromCallable(() -> {
            List<Card> newCards = cardRepository.findByDeckIdAndState(deckId, "NEW");
            LocalDateTime now = LocalDateTime.now();

            return newCards.stream()
                    .limit(limit)
                    .map(card -> {
                        ReviewRecord reviewRecord = reviewRecordRepository.findByCardId(card.getId())
                                .orElseGet(() -> {
                                    // Create initial review record for new card
                                    ReviewRecord newRecord = ReviewRecord.builder()
                                            .cardId(card.getId())
                                            .easeFactor(2.5)
                                            .repetitions(0)
                                            .intervalDays(0)
                                            .nextReviewDate(now)
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .build();
                                    return reviewRecordRepository.save(newRecord);
                                });
                        return new StudyCard(card, reviewRecord);
                    })
                    .toList();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable);
    }

    /**
     * Processes a user's rating of a card and updates scheduling via SM-2 algorithm.
     *
     * <p>This method performs the core spaced repetition workflow:</p>
     * <ol>
     *   <li>Fetch the card and its review record</li>
     *   <li>Convert user-friendly rating to quality value (0-5 scale)</li>
     *   <li>Calculate new scheduling parameters using SM-2 algorithm</li>
     *   <li>Update card state (NEW -> LEARNING -> REVIEW)</li>
     *   <li>Update review record with new ease factor, repetitions, interval, and next review date</li>
     *   <li>Persist both entities</li>
     *   <li>Return result summary</li>
     * </ol>
     *
     * @param cardId ID of the card being rated
     * @param rating User rating (AGAIN, HARD, GOOD, or EASY)
     * @return Mono of ReviewResult containing updated scheduling information
     * @throws RuntimeException if card or review record not found
     */
    public Mono<ReviewResult> rateCard(Long cardId, SM2Service.UserRating rating) {
        return Mono.fromCallable(() -> {
            LocalDateTime now = LocalDateTime.now();

            // Fetch card and review record
            Card card = cardRepository.findById(cardId)
                    .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

            ReviewRecord reviewRecord = reviewRecordRepository.findByCardId(cardId)
                    .orElseThrow(() -> new RuntimeException("Review record not found for card: " + cardId));

            // Convert rating to quality value
            int quality = sm2Service.mapRatingToQuality(rating);

            // Calculate next review parameters using SM-2
            SM2Service.SM2Result sm2Result = sm2Service.calculateNext(
                    quality,
                    reviewRecord.getEaseFactor(),
                    reviewRecord.getRepetitions(),
                    reviewRecord.getIntervalDays()
            );

            // Update card state
            card.setState(sm2Result.newState());
            card.setUpdatedAt(now);

            // Update review record with SM-2 results
            reviewRecord.setEaseFactor(sm2Result.easeFactor());
            reviewRecord.setRepetitions(sm2Result.repetitions());
            reviewRecord.setIntervalDays(sm2Result.intervalDays());
            reviewRecord.setNextReviewDate(sm2Result.nextReviewDate());
            reviewRecord.setLastReviewedAt(now);
            reviewRecord.setUpdatedAt(now);

            // Persist changes
            cardRepository.save(card);
            reviewRecordRepository.save(reviewRecord);

            // Return result summary
            return new ReviewResult(
                    cardId,
                    sm2Result.newState(),
                    sm2Result.intervalDays(),
                    sm2Result.nextReviewDate(),
                    sm2Result.easeFactor()
            );
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
}
