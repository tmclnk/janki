package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.dto.DeckStats;
import com.prairiegrade.janki.exception.DeckNotFoundException;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing decks and their statistics.
 * Uses virtual threads for efficient handling of blocking JDBC operations.
 */
@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    /**
     * Create a new deck with the specified name and description.
     *
     * @param name        Name of the deck
     * @param description Description of the deck
     * @return The created deck
     */
    @Transactional
    public Deck createDeck(String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        Deck deck = Deck.builder()
                .name(name)
                .description(description)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return deckRepository.save(deck);
    }

    /**
     * Retrieve all decks ordered by name.
     *
     * @return List of all decks
     */
    @Transactional(readOnly = true)
    public List<Deck> getAllDecks() {
        return deckRepository.findAllByOrderByNameAsc();
    }

    /**
     * Retrieve a single deck by ID.
     *
     * @param id ID of the deck to retrieve
     * @return The deck
     * @throws DeckNotFoundException if the deck is not found
     */
    @Transactional(readOnly = true)
    public Deck getDeck(Long id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException(id));
    }

    /**
     * Update an existing deck's name and description.
     *
     * @param id          ID of the deck to update
     * @param name        New name for the deck
     * @param description New description for the deck
     * @return The updated deck
     * @throws DeckNotFoundException if the deck is not found
     */
    @Transactional
    public Deck updateDeck(Long id, String name, String description) {
        Deck deck = deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException(id));
        deck.setName(name);
        deck.setDescription(description);
        deck.setUpdatedAt(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    /**
     * Delete a deck by ID.
     *
     * @param id ID of the deck to delete
     * @throws DeckNotFoundException if the deck is not found
     */
    @Transactional
    public void deleteDeck(Long id) {
        if (!deckRepository.existsById(id)) {
            throw new DeckNotFoundException(id);
        }
        deckRepository.deleteById(id);
    }

    /**
     * Get statistics for a deck including card counts by state.
     *
     * @param id ID of the deck
     * @return Deck statistics
     * @throws DeckNotFoundException if the deck is not found
     */
    @Transactional(readOnly = true)
    public DeckStats getDeckStats(Long id) {
        if (!deckRepository.existsById(id)) {
            throw new DeckNotFoundException(id);
        }

        long totalCards = cardRepository.countByDeckId(id);
        long newCards = cardRepository.findByDeckIdAndState(id, "NEW").size();
        long learningCards = cardRepository.findByDeckIdAndState(id, "LEARNING").size();
        long reviewCards = cardRepository.findByDeckIdAndState(id, "REVIEW").size();

        return new DeckStats(totalCards, newCards, learningCards, reviewCards);
    }
}
