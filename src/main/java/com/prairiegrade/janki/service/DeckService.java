package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.dto.DeckStats;
import com.prairiegrade.janki.exception.DeckNotFoundException;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.DeckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

/**
 * Service for managing decks and their statistics.
 * Provides reactive wrappers around blocking JDBC repository operations.
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
     * @return Mono containing the created deck
     */
    public Mono<Deck> createDeck(String name, String description) {
        return Mono.fromCallable(() -> {
            LocalDateTime now = LocalDateTime.now();
            Deck deck = Deck.builder()
                    .name(name)
                    .description(description)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            return deckRepository.save(deck);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieve all decks ordered by name.
     *
     * @return Flux of all decks
     */
    public Flux<Deck> getAllDecks() {
        return Mono.fromCallable(() -> deckRepository.findAllByOrderByNameAsc())
                .flatMapMany(Flux::fromIterable)
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Retrieve a single deck by ID.
     *
     * @param id ID of the deck to retrieve
     * @return Mono containing the deck
     * @throws DeckNotFoundException if the deck is not found
     */
    public Mono<Deck> getDeck(Long id) {
        return Mono.fromCallable(() -> deckRepository.findById(id)
                .orElseThrow(() -> new DeckNotFoundException(id)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Update an existing deck's name and description.
     *
     * @param id          ID of the deck to update
     * @param name        New name for the deck
     * @param description New description for the deck
     * @return Mono containing the updated deck
     * @throws DeckNotFoundException if the deck is not found
     */
    public Mono<Deck> updateDeck(Long id, String name, String description) {
        return Mono.fromCallable(() -> {
            Deck deck = deckRepository.findById(id)
                    .orElseThrow(() -> new DeckNotFoundException(id));
            deck.setName(name);
            deck.setDescription(description);
            deck.setUpdatedAt(LocalDateTime.now());
            return deckRepository.save(deck);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Delete a deck by ID.
     *
     * @param id ID of the deck to delete
     * @return Mono that completes when the deck is deleted
     * @throws DeckNotFoundException if the deck is not found
     */
    public Mono<Void> deleteDeck(Long id) {
        return Mono.fromCallable(() -> {
            if (!deckRepository.existsById(id)) {
                throw new DeckNotFoundException(id);
            }
            deckRepository.deleteById(id);
            return null;
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    /**
     * Get statistics for a deck including card counts by state.
     *
     * @param id ID of the deck
     * @return Mono containing deck statistics
     * @throws DeckNotFoundException if the deck is not found
     */
    public Mono<DeckStats> getDeckStats(Long id) {
        return Mono.fromCallable(() -> {
            if (!deckRepository.existsById(id)) {
                throw new DeckNotFoundException(id);
            }

            long totalCards = cardRepository.countByDeckId(id);
            long newCards = cardRepository.findByDeckIdAndState(id, "NEW").size();
            long learningCards = cardRepository.findByDeckIdAndState(id, "LEARNING").size();
            long reviewCards = cardRepository.findByDeckIdAndState(id, "REVIEW").size();

            return new DeckStats(totalCards, newCards, learningCards, reviewCards);
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
