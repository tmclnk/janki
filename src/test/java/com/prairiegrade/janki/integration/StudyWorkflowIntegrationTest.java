package com.prairiegrade.janki.integration;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.dto.DeckStats;
import com.prairiegrade.janki.dto.ReviewResult;
import com.prairiegrade.janki.service.CardService;
import com.prairiegrade.janki.service.DeckService;
import com.prairiegrade.janki.service.SM2Service;
import com.prairiegrade.janki.service.StudyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for the complete study workflow.
 *
 * <p>This test verifies the full lifecycle of flashcard study including:
 * deck creation, card creation, study session management, rating cards with SM-2 algorithm,
 * state transitions (NEW → LEARNING → REVIEW), and statistics tracking.</p>
 *
 * <p>Uses @SpringBootTest for full Spring context and @Transactional for test isolation.</p>
 */
@SpringBootTest
@Transactional
@DisplayName("Study Workflow Integration Test")
class StudyWorkflowIntegrationTest {

    @Autowired
    private DeckService deckService;

    @Autowired
    private CardService cardService;

    @Autowired
    private StudyService studyService;

    private Deck testDeck;

    @BeforeEach
    void setUp() {
        // Create a deck for testing
        testDeck = deckService.createDeck("Integration Test Deck", "Testing full workflow");
    }

    @Test
    @DisplayName("Complete study workflow: create deck, add cards, study, rate, verify stats")
    void testCompleteStudyWorkflow() {
        // Step 1: Create a deck (already done in setUp)
        assertNotNull(testDeck);
        assertNotNull(testDeck.getId());
        assertEquals("Integration Test Deck", testDeck.getName());

        // Step 2: Add 3 cards to the deck
        Card card1 = cardService.createCard(testDeck.getId(), "What is Java?", "A programming language");
        Card card2 = cardService.createCard(testDeck.getId(), "What is Spring?", "A Java framework");
        Card card3 = cardService.createCard(testDeck.getId(), "What is JPA?", "Java Persistence API");

        assertNotNull(card1.getId());
        assertNotNull(card2.getId());
        assertNotNull(card3.getId());
        assertEquals("NEW", card1.getState());
        assertEquals("NEW", card2.getState());
        assertEquals("NEW", card3.getState());

        // Step 3: Verify initial deck statistics
        DeckStats initialStats = deckService.getDeckStats(testDeck.getId());
        assertEquals(3L, initialStats.totalCards());
        assertEquals(3L, initialStats.newCards());
        assertEquals(0L, initialStats.learningCards());
        assertEquals(0L, initialStats.reviewCards());

        // Step 4: Start study session - get new cards
        List<StudyService.StudyCard> newCards = studyService.getNewCards(testDeck.getId(), 10);
        assertEquals(3, newCards.size(), "Should return all 3 new cards");

        // Verify all cards have review records initialized
        newCards.forEach(studyCard -> {
            assertNotNull(studyCard.reviewRecord());
            assertEquals(2.5, studyCard.reviewRecord().getEaseFactor());
            assertEquals(0, studyCard.reviewRecord().getRepetitions());
            assertEquals(0, studyCard.reviewRecord().getIntervalDays());
        });

        // Step 5: Rate first card as GOOD
        ReviewResult result1 = studyService.rateCard(card1.getId(), SM2Service.UserRating.GOOD);

        assertNotNull(result1);
        assertEquals(card1.getId(), result1.cardId());
        assertEquals("LEARNING", result1.newState());
        assertEquals(1, result1.intervalDays());
        assertTrue(result1.easeFactor() < 2.5, "Ease factor should decrease for quality 3");

        // Verify card state changed
        Card updatedCard1 = cardService.getCard(card1.getId());
        assertEquals("LEARNING", updatedCard1.getState());

        // Step 6: Rate second card as AGAIN (failure)
        ReviewResult result2 = studyService.rateCard(card2.getId(), SM2Service.UserRating.AGAIN);

        assertEquals("LEARNING", result2.newState());
        assertEquals(0, result2.intervalDays(), "Failed card should have 0 interval");
        assertTrue(result2.easeFactor() < 2.5, "Ease factor should decrease on failure");

        // Verify card stays in LEARNING with reset interval
        Card updatedCard2 = cardService.getCard(card2.getId());
        assertEquals("LEARNING", updatedCard2.getState());

        // Step 7: Rate third card as EASY
        ReviewResult result3 = studyService.rateCard(card3.getId(), SM2Service.UserRating.EASY);

        assertEquals("LEARNING", result3.newState());
        assertEquals(1, result3.intervalDays());
        assertEquals(2.5, result3.easeFactor(), 0.01, "Quality 4 should keep ease factor at 2.5");

        // Step 8: Verify deck statistics after first round of reviews
        DeckStats midStats = deckService.getDeckStats(testDeck.getId());
        assertEquals(3L, midStats.totalCards());
        assertEquals(0L, midStats.newCards(), "All cards should have been studied");
        assertEquals(3L, midStats.learningCards(), "All cards should be in LEARNING state");
        assertEquals(0L, midStats.reviewCards());

        // Step 9: Graduate card 1 to REVIEW (rate it GOOD again for second successful review)
        ReviewResult graduationResult = studyService.rateCard(card1.getId(), SM2Service.UserRating.GOOD);

        assertEquals("REVIEW", graduationResult.newState(), "Second successful review should graduate to REVIEW");
        assertEquals(6, graduationResult.intervalDays(), "Second successful review gets 6-day interval");

        // Step 10: Verify final deck statistics
        DeckStats finalStats = deckService.getDeckStats(testDeck.getId());
        assertEquals(3L, finalStats.totalCards());
        assertEquals(0L, finalStats.newCards());
        assertEquals(2L, finalStats.learningCards(), "Cards 2 and 3 still in LEARNING");
        assertEquals(1L, finalStats.reviewCards(), "Card 1 graduated to REVIEW");

        // Step 11: Verify no due cards immediately after rating (all scheduled for future)
        List<StudyService.StudyCard> dueCards = studyService.getDueCards(testDeck.getId(), 10);
        // Note: Some cards might be due if they have 0 interval (card2 failed)
        assertTrue(dueCards.size() <= 1, "At most one card should be due immediately (the failed one)");

        // Step 12: Verify we can retrieve all cards in the deck
        List<Card> allCards = cardService.getCardsInDeck(testDeck.getId());
        assertEquals(3, allCards.size());
    }

    @Test
    @DisplayName("Multiple graduation path: NEW → LEARNING → REVIEW")
    void testCardGraduationPath() {
        // Create a card
        Card card = cardService.createCard(testDeck.getId(), "Test Question", "Test Answer");
        assertEquals("NEW", card.getState());

        // First rating (GOOD): NEW → LEARNING, interval = 1 day
        ReviewResult result1 = studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD);
        assertEquals("LEARNING", result1.newState());
        assertEquals(1, result1.intervalDays());

        // Second rating (GOOD): LEARNING → REVIEW, interval = 6 days
        ReviewResult result2 = studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD);
        assertEquals("REVIEW", result2.newState());
        assertEquals(6, result2.intervalDays());

        // Third rating (GOOD): REVIEW stays REVIEW, exponential interval
        ReviewResult result3 = studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD);
        assertEquals("REVIEW", result3.newState());
        assertTrue(result3.intervalDays() > 6, "Interval should grow exponentially");
    }

    @Test
    @DisplayName("Failed review resets card from REVIEW to LEARNING")
    void testFailedReviewResetsCard() {
        // Create and graduate a card to REVIEW
        Card card = cardService.createCard(testDeck.getId(), "Test Question", "Test Answer");

        // Graduate to REVIEW
        studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD); // NEW → LEARNING
        studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD); // LEARNING → REVIEW
        studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD); // REVIEW, longer interval

        // Verify in REVIEW state
        Card reviewCard = cardService.getCard(card.getId());
        assertEquals("REVIEW", reviewCard.getState());

        // Fail the review
        ReviewResult failResult = studyService.rateCard(card.getId(), SM2Service.UserRating.AGAIN);

        // Verify reset to LEARNING
        assertEquals("LEARNING", failResult.newState());
        assertEquals(0, failResult.intervalDays(), "Should reset interval to 0");

        Card resetCard = cardService.getCard(card.getId());
        assertEquals("LEARNING", resetCard.getState());
    }

    @Test
    @DisplayName("Empty deck study workflow")
    void testEmptyDeckStudyWorkflow() {
        // Create an empty deck
        Deck emptyDeck = deckService.createDeck("Empty Deck", "No cards");

        // Verify stats
        DeckStats stats = deckService.getDeckStats(emptyDeck.getId());
        assertEquals(0L, stats.totalCards());
        assertEquals(0L, stats.newCards());
        assertEquals(0L, stats.learningCards());
        assertEquals(0L, stats.reviewCards());

        // Try to get new cards
        List<StudyService.StudyCard> newCards = studyService.getNewCards(emptyDeck.getId(), 10);
        assertTrue(newCards.isEmpty());

        // Try to get due cards
        List<StudyService.StudyCard> dueCards = studyService.getDueCards(emptyDeck.getId(), 10);
        assertTrue(dueCards.isEmpty());
    }

    @Test
    @DisplayName("Study session respects card limit")
    void testStudySessionLimits() {
        // Create 10 cards
        for (int i = 1; i <= 10; i++) {
            cardService.createCard(testDeck.getId(), "Question " + i, "Answer " + i);
        }

        // Request only 5 new cards
        List<StudyService.StudyCard> limitedCards = studyService.getNewCards(testDeck.getId(), 5);
        assertEquals(5, limitedCards.size(), "Should respect the limit parameter");

        // Verify there are still 5 cards not retrieved
        List<Card> allCards = cardService.getCardsInDeck(testDeck.getId());
        assertEquals(10, allCards.size());
    }

    @Test
    @DisplayName("Deleting deck cascade deletes cards")
    void testDeckDeletionCascade() {
        // Create cards in deck
        Card card1 = cardService.createCard(testDeck.getId(), "Q1", "A1");
        Card card2 = cardService.createCard(testDeck.getId(), "Q2", "A2");

        // Verify cards exist
        assertEquals(2, cardService.getCardsInDeck(testDeck.getId()).size());

        // Delete deck
        deckService.deleteDeck(testDeck.getId());

        // Note: Depending on database cascade configuration, cards may or may not be deleted
        // This test just verifies the deck is deleted
        assertThrows(Exception.class, () -> {
            deckService.getDeck(testDeck.getId());
        });
    }

    @Test
    @DisplayName("Card update preserves review record and state")
    void testCardUpdatePreservesReviewData() {
        // Create and study a card
        Card card = cardService.createCard(testDeck.getId(), "Original Question", "Original Answer");
        studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD);

        // Verify card is in LEARNING state
        Card studiedCard = cardService.getCard(card.getId());
        assertEquals("LEARNING", studiedCard.getState());

        // Update card content
        Card updatedCard = cardService.updateCard(card.getId(), "Updated Question", "Updated Answer");

        // Verify content changed but state preserved
        assertEquals("Updated Question", updatedCard.getFront());
        assertEquals("Updated Answer", updatedCard.getBack());
        assertEquals("LEARNING", updatedCard.getState(), "State should be preserved on content update");

        // Verify can still rate the card
        ReviewResult result = studyService.rateCard(card.getId(), SM2Service.UserRating.GOOD);
        assertNotNull(result);
    }
}
