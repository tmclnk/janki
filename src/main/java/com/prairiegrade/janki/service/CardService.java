package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.ReviewRecord;
import com.prairiegrade.janki.exception.CardNotFoundException;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.ReviewRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * Service layer for Card management operations.
 * Provides reactive wrappers around blocking JDBC operations using boundedElastic scheduler.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final ReviewRecordRepository reviewRecordRepository;

    /**
     * Creates a new card with its associated review record.
     * The card is initialized with state=NEW and timestamps.
     * A ReviewRecord is automatically created with default SM-2 values.
     *
     * @param deckId ID of the deck the card belongs to
     * @param front  Front content of the card
     * @param back   Back content of the card
     * @return Mono emitting the created Card
     */
    public Mono<Card> createCard(Long deckId, String front, String back) {
        return Mono.fromCallable(() -> {
            LocalDateTime now = LocalDateTime.now();

            // Create and save the card
            Card card = Card.builder()
                    .deckId(deckId)
                    .front(front)
                    .back(back)
                    .state("NEW")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            Card savedCard = cardRepository.save(card);
            log.debug("Created card with id: {}", savedCard.getId());

            // Create the associated review record with default values
            ReviewRecord reviewRecord = ReviewRecord.builder()
                    .cardId(savedCard.getId())
                    .easeFactor(2.5)
                    .repetitions(0)
                    .intervalDays(0)
                    .nextReviewDate(now)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            reviewRecordRepository.save(reviewRecord);
            log.debug("Created review record for card id: {}", savedCard.getId());

            return savedCard;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieves all cards in a specific deck.
     *
     * @param deckId ID of the deck
     * @return Flux emitting all cards in the deck
     */
    public Flux<Card> getCardsInDeck(Long deckId) {
        return Mono.fromCallable(() -> cardRepository.findByDeckId(deckId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * Retrieves a specific card by ID.
     *
     * @param id ID of the card
     * @return Mono emitting the card if found
     * @throws CardNotFoundException if the card does not exist
     */
    public Mono<Card> getCard(Long id) {
        return Mono.fromCallable(() ->
                cardRepository.findById(id)
                        .orElseThrow(() -> new CardNotFoundException(id))
        ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Updates the front and back content of an existing card.
     * Updates the updatedAt timestamp.
     *
     * @param id    ID of the card to update
     * @param front New front content
     * @param back  New back content
     * @return Mono emitting the updated card
     * @throws CardNotFoundException if the card does not exist
     */
    public Mono<Card> updateCard(Long id, String front, String back) {
        return Mono.fromCallable(() -> {
            Card card = cardRepository.findById(id)
                    .orElseThrow(() -> new CardNotFoundException(id));

            card.setFront(front);
            card.setBack(back);
            card.setUpdatedAt(LocalDateTime.now());

            Card updatedCard = cardRepository.save(card);
            log.debug("Updated card with id: {}", updatedCard.getId());

            return updatedCard;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Deletes a card and its associated review record.
     * The review record is cascade deleted due to the cardId foreign key relationship.
     *
     * @param id ID of the card to delete
     * @return Mono that completes when deletion is done
     * @throws CardNotFoundException if the card does not exist
     */
    public Mono<Void> deleteCard(Long id) {
        return Mono.fromCallable(() -> {
            if (!cardRepository.existsById(id)) {
                throw new CardNotFoundException(id);
            }

            // Delete the review record first
            reviewRecordRepository.findByCardId(id)
                    .ifPresent(reviewRecord -> {
                        reviewRecordRepository.deleteById(reviewRecord.getId());
                        log.debug("Deleted review record for card id: {}", id);
                    });

            // Delete the card
            cardRepository.deleteById(id);
            log.debug("Deleted card with id: {}", id);

            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
