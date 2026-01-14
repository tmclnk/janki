package com.prairiegrade.janki.repository;

import com.prairiegrade.janki.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Card entities.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Find all cards in a specific deck.
     *
     * @param deckId ID of the deck
     * @return List of cards in the deck
     */
    List<Card> findByDeckId(Long deckId);

    /**
     * Find cards in a deck with a specific state.
     *
     * @param deckId ID of the deck
     * @param state  Card state (NEW, LEARNING, REVIEW)
     * @return List of cards matching the criteria
     */
    List<Card> findByDeckIdAndState(Long deckId, String state);

    /**
     * Count total cards in a deck.
     *
     * @param deckId ID of the deck
     * @return Number of cards in the deck
     */
    long countByDeckId(Long deckId);
}
