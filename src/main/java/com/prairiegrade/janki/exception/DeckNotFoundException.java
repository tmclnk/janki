package com.prairiegrade.janki.exception;

/**
 * Exception thrown when a deck is not found by ID.
 */
public class DeckNotFoundException extends RuntimeException {
    public DeckNotFoundException(Long id) {
        super("Deck not found with id: " + id);
    }
}
