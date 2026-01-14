package com.prairiegrade.janki.dto;

/**
 * Statistics for a deck including card counts by state.
 *
 * @param totalCards    Total number of cards in the deck
 * @param newCards      Number of cards in NEW state
 * @param learningCards Number of cards in LEARNING state
 * @param reviewCards   Number of cards in REVIEW state
 */
public record DeckStats(
    Long totalCards,
    Long newCards,
    Long learningCards,
    Long reviewCards
) {}
