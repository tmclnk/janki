package com.prairiegrade.janki.controller.api;

import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.dto.DeckStats;
import com.prairiegrade.janki.dto.request.CreateDeckRequest;
import com.prairiegrade.janki.dto.request.UpdateDeckRequest;
import com.prairiegrade.janki.dto.response.DeckResponse;
import com.prairiegrade.janki.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for managing decks.
 * Provides endpoints for CRUD operations and deck statistics.
 */
@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
public class DeckRestController {

    private final DeckService deckService;

    /**
     * Get all decks.
     *
     * @return List of all decks
     */
    @GetMapping
    public ResponseEntity<List<DeckResponse>> getAllDecks() {
        List<Deck> decks = deckService.getAllDecks();
        List<DeckResponse> responses = decks.stream()
                .map(DeckResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a single deck by ID.
     *
     * @param id ID of the deck to retrieve
     * @return The deck
     */
    @GetMapping("/{id}")
    public ResponseEntity<DeckResponse> getDeck(@PathVariable Long id) {
        Deck deck = deckService.getDeck(id);
        return ResponseEntity.ok(DeckResponse.from(deck));
    }

    /**
     * Create a new deck.
     *
     * @param request Deck creation request with name and description
     * @return The created deck with 201 Created status
     */
    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(@Valid @RequestBody CreateDeckRequest request) {
        Deck deck = deckService.createDeck(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(DeckResponse.from(deck));
    }

    /**
     * Update an existing deck.
     *
     * @param id      ID of the deck to update
     * @param request Deck update request with name and description
     * @return The updated deck
     */
    @PutMapping("/{id}")
    public ResponseEntity<DeckResponse> updateDeck(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeckRequest request) {
        Deck deck = deckService.updateDeck(id, request.name(), request.description());
        return ResponseEntity.ok(DeckResponse.from(deck));
    }

    /**
     * Delete a deck by ID.
     *
     * @param id ID of the deck to delete
     * @return 204 No Content on successful deletion
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(@PathVariable Long id) {
        deckService.deleteDeck(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get statistics for a deck including card counts by state.
     *
     * @param id ID of the deck
     * @return Deck statistics
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<DeckStats> getDeckStats(@PathVariable Long id) {
        DeckStats stats = deckService.getDeckStats(id);
        return ResponseEntity.ok(stats);
    }
}
