package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.ReviewRecord;
import com.prairiegrade.janki.exception.CardNotFoundException;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.ReviewRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer for Card management operations.
 * Uses virtual threads for efficient handling of blocking JDBC operations.
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
     * @return The created Card
     */
    @Transactional
    public Card createCard(Long deckId, String front, String back) {
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
    }

    /**
     * Retrieves all cards in a specific deck.
     *
     * @param deckId ID of the deck
     * @return List of all cards in the deck
     */
    @Transactional(readOnly = true)
    public List<Card> getCardsInDeck(Long deckId) {
        return cardRepository.findByDeckId(deckId);
    }

    /**
     * Retrieves a specific card by ID.
     *
     * @param id ID of the card
     * @return The card
     * @throws CardNotFoundException if the card does not exist
     */
    @Transactional(readOnly = true)
    public Card getCard(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));
    }

    /**
     * Updates the front and back content of an existing card.
     * Updates the updatedAt timestamp.
     *
     * @param id    ID of the card to update
     * @param front New front content
     * @param back  New back content
     * @return The updated card
     * @throws CardNotFoundException if the card does not exist
     */
    @Transactional
    public Card updateCard(Long id, String front, String back) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(id));

        card.setFront(front);
        card.setBack(back);
        card.setUpdatedAt(LocalDateTime.now());

        Card updatedCard = cardRepository.save(card);
        log.debug("Updated card with id: {}", updatedCard.getId());

        return updatedCard;
    }

    /**
     * Deletes a card and its associated review record.
     * The review record is cascade deleted due to the cardId foreign key relationship.
     *
     * @param id ID of the card to delete
     * @throws CardNotFoundException if the card does not exist
     */
    @Transactional
    public void deleteCard(Long id) {
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
    }
}
