package com.prairiegrade.janki.exception;

/**
 * Exception thrown when a card is not found by ID.
 */
public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException(Long id) {
        super("Card not found with id: " + id);
    }
}
