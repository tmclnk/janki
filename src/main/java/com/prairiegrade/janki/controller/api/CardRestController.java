package com.prairiegrade.janki.controller.api;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.dto.request.CreateCardRequest;
import com.prairiegrade.janki.dto.request.UpdateCardRequest;
import com.prairiegrade.janki.dto.response.CardResponse;
import com.prairiegrade.janki.exception.CardNotFoundException;
import com.prairiegrade.janki.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller for Card management operations.
 * Provides CRUD endpoints for cards within decks.
 */
@RestController
@RequestMapping("/api/decks/{deckId}/cards")
@RequiredArgsConstructor
public class CardRestController {

    private final CardService cardService;

    /**
     * Get all cards in a specific deck.
     *
     * @param deckId ID of the deck
     * @return List of cards in the deck
     */
    @GetMapping
    public ResponseEntity<List<CardResponse>> getCardsInDeck(@PathVariable Long deckId) {
        List<CardResponse> cards = cardService.getCardsInDeck(deckId)
                .stream()
                .map(CardResponse::from)
                .toList();
        return ResponseEntity.ok(cards);
    }

    /**
     * Get a specific card by ID.
     * Verifies that the card belongs to the specified deck.
     *
     * @param deckId ID of the deck
     * @param cardId ID of the card
     * @return The card
     * @throws CardNotFoundException if the card does not exist or doesn't belong to the deck
     */
    @GetMapping("/{cardId}")
    public ResponseEntity<CardResponse> getCard(
            @PathVariable Long deckId,
            @PathVariable Long cardId) {
        Card card = cardService.getCard(cardId);
        verifyCardBelongsToDeck(card, deckId);
        return ResponseEntity.ok(CardResponse.from(card));
    }

    /**
     * Create a new card in the specified deck.
     *
     * @param deckId  ID of the deck
     * @param request Card creation request with front and back content
     * @return The created card with 201 Created status
     */
    @PostMapping
    public ResponseEntity<CardResponse> createCard(
            @PathVariable Long deckId,
            @Valid @RequestBody CreateCardRequest request) {
        Card card = cardService.createCard(deckId, request.front(), request.back());
        return ResponseEntity.status(HttpStatus.CREATED).body(CardResponse.from(card));
    }

    /**
     * Update an existing card.
     * Verifies that the card belongs to the specified deck.
     *
     * @param deckId  ID of the deck
     * @param cardId  ID of the card to update
     * @param request Card update request with new front and back content
     * @return The updated card
     * @throws CardNotFoundException if the card does not exist or doesn't belong to the deck
     */
    @PutMapping("/{cardId}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable Long deckId,
            @PathVariable Long cardId,
            @Valid @RequestBody UpdateCardRequest request) {
        Card card = cardService.getCard(cardId);
        verifyCardBelongsToDeck(card, deckId);

        Card updatedCard = cardService.updateCard(cardId, request.front(), request.back());
        return ResponseEntity.ok(CardResponse.from(updatedCard));
    }

    /**
     * Delete a card.
     * Verifies that the card belongs to the specified deck.
     *
     * @param deckId ID of the deck
     * @param cardId ID of the card to delete
     * @return 204 No Content on successful deletion
     * @throws CardNotFoundException if the card does not exist or doesn't belong to the deck
     */
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
            @PathVariable Long deckId,
            @PathVariable Long cardId) {
        Card card = cardService.getCard(cardId);
        verifyCardBelongsToDeck(card, deckId);

        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifies that a card belongs to the specified deck.
     * Throws CardNotFoundException if the card belongs to a different deck.
     *
     * @param card   The card to verify
     * @param deckId The expected deck ID
     * @throws CardNotFoundException if the card doesn't belong to the deck
     */
    private void verifyCardBelongsToDeck(Card card, Long deckId) {
        if (!card.getDeckId().equals(deckId)) {
            throw new CardNotFoundException(card.getId());
        }
    }
}
