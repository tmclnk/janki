package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.ReviewRecord;
import com.prairiegrade.janki.exception.CardNotFoundException;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.ReviewRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CardService covering CRUD operations and review record initialization.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CardService")
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReviewRecordRepository reviewRecordRepository;

    @InjectMocks
    private CardService cardService;

    private Card sampleCard;
    private ReviewRecord sampleReviewRecord;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        sampleCard = Card.builder()
                .id(1L)
                .deckId(10L)
                .front("What is Java?")
                .back("A programming language")
                .state("NEW")
                .createdAt(now)
                .updatedAt(now)
                .build();

        sampleReviewRecord = ReviewRecord.builder()
                .id(1L)
                .cardId(1L)
                .easeFactor(2.5)
                .repetitions(0)
                .intervalDays(0)
                .nextReviewDate(now)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Nested
    @DisplayName("Create Card")
    class CreateCard {

        @Test
        @DisplayName("Should create card with review record")
        void shouldCreateCardWithReviewRecord() {
            // Given
            Long deckId = 10L;
            String front = "What is Java?";
            String back = "A programming language";

            when(cardRepository.save(any(Card.class))).thenReturn(sampleCard);
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenReturn(sampleReviewRecord);

            // When
            Card result = cardService.createCard(deckId, front, back);

            // Then
            assertNotNull(result);
            assertEquals(deckId, result.getDeckId());
            assertEquals(front, result.getFront());
            assertEquals(back, result.getBack());
            assertEquals("NEW", result.getState());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());

            // Verify card was saved
            verify(cardRepository, times(1)).save(any(Card.class));

            // Verify review record was created with default SM-2 values
            verify(reviewRecordRepository, times(1)).save(argThat(reviewRecord ->
                    reviewRecord.getCardId().equals(result.getId()) &&
                    reviewRecord.getEaseFactor() == 2.5 &&
                    reviewRecord.getRepetitions() == 0 &&
                    reviewRecord.getIntervalDays() == 0
            ));
        }

        @Test
        @DisplayName("Should create card with multiline content")
        void shouldCreateCardWithMultilineContent() {
            // Given
            Long deckId = 10L;
            String front = "List the principles of OOP:\n1. ?\n2. ?\n3. ?\n4. ?";
            String back = "1. Encapsulation\n2. Inheritance\n3. Polymorphism\n4. Abstraction";

            Card multilineCard = Card.builder()
                    .id(2L)
                    .deckId(deckId)
                    .front(front)
                    .back(back)
                    .state("NEW")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(cardRepository.save(any(Card.class))).thenReturn(multilineCard);
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenReturn(sampleReviewRecord);

            // When
            Card result = cardService.createCard(deckId, front, back);

            // Then
            assertTrue(result.getFront().contains("\n"));
            assertTrue(result.getBack().contains("\n"));
            verify(cardRepository, times(1)).save(any(Card.class));
            verify(reviewRecordRepository, times(1)).save(any(ReviewRecord.class));
        }
    }

    @Nested
    @DisplayName("Get Cards in Deck")
    class GetCardsInDeck {

        @Test
        @DisplayName("Should return all cards in deck")
        void shouldReturnAllCardsInDeck() {
            // Given
            Long deckId = 10L;
            Card card1 = Card.builder().id(1L).deckId(deckId).front("Q1").back("A1").build();
            Card card2 = Card.builder().id(2L).deckId(deckId).front("Q2").back("A2").build();
            Card card3 = Card.builder().id(3L).deckId(deckId).front("Q3").back("A3").build();
            List<Card> cards = Arrays.asList(card1, card2, card3);

            when(cardRepository.findByDeckId(deckId)).thenReturn(cards);

            // When
            List<Card> result = cardService.getCardsInDeck(deckId);

            // Then
            assertEquals(3, result.size());
            assertTrue(result.stream().allMatch(card -> card.getDeckId().equals(deckId)));
            verify(cardRepository, times(1)).findByDeckId(deckId);
        }

        @Test
        @DisplayName("Should return empty list when deck has no cards")
        void shouldReturnEmptyListWhenNoCards() {
            // Given
            Long deckId = 10L;
            when(cardRepository.findByDeckId(deckId)).thenReturn(List.of());

            // When
            List<Card> result = cardService.getCardsInDeck(deckId);

            // Then
            assertTrue(result.isEmpty());
            verify(cardRepository, times(1)).findByDeckId(deckId);
        }
    }

    @Nested
    @DisplayName("Get Card by ID")
    class GetCardById {

        @Test
        @DisplayName("Should return card when ID exists")
        void shouldReturnCardWhenIdExists() {
            // Given
            Long cardId = 1L;
            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));

            // When
            Card result = cardService.getCard(cardId);

            // Then
            assertNotNull(result);
            assertEquals(cardId, result.getId());
            assertEquals("What is Java?", result.getFront());
            verify(cardRepository, times(1)).findById(cardId);
        }

        @Test
        @DisplayName("Should throw CardNotFoundException when ID doesn't exist")
        void shouldThrowExceptionWhenIdNotFound() {
            // Given
            Long nonExistentId = 999L;
            when(cardRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(CardNotFoundException.class, () -> {
                cardService.getCard(nonExistentId);
            });
            verify(cardRepository, times(1)).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Update Card")
    class UpdateCard {

        @Test
        @DisplayName("Should update card front and back content")
        void shouldUpdateCardSuccessfully() {
            // Given
            Long cardId = 1L;
            String newFront = "What is Spring Boot?";
            String newBack = "A framework for building Java applications";
            LocalDateTime originalCreatedAt = sampleCard.getCreatedAt();

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Card result = cardService.updateCard(cardId, newFront, newBack);

            // Then
            assertNotNull(result);
            assertEquals(newFront, result.getFront());
            assertEquals(newBack, result.getBack());
            assertEquals(originalCreatedAt, result.getCreatedAt());
            assertTrue(result.getUpdatedAt().isAfter(result.getCreatedAt()) ||
                      result.getUpdatedAt().isEqual(result.getCreatedAt()));

            verify(cardRepository, times(1)).findById(cardId);
            verify(cardRepository, times(1)).save(sampleCard);
        }

        @Test
        @DisplayName("Should throw CardNotFoundException when updating non-existent card")
        void shouldThrowExceptionWhenUpdatingNonExistentCard() {
            // Given
            Long nonExistentId = 999L;
            when(cardRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(CardNotFoundException.class, () -> {
                cardService.updateCard(nonExistentId, "New Front", "New Back");
            });
            verify(cardRepository, times(1)).findById(nonExistentId);
            verify(cardRepository, never()).save(any(Card.class));
        }

        @Test
        @DisplayName("Should preserve card state when updating content")
        void shouldPreserveCardState() {
            // Given
            Long cardId = 1L;
            String originalState = "REVIEW";
            sampleCard.setState(originalState);

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Card result = cardService.updateCard(cardId, "Updated Front", "Updated Back");

            // Then
            assertEquals(originalState, result.getState(), "State should not change when updating content");
            verify(cardRepository, times(1)).save(sampleCard);
        }
    }

    @Nested
    @DisplayName("Delete Card")
    class DeleteCard {

        @Test
        @DisplayName("Should delete card and its review record")
        void shouldDeleteCardAndReviewRecord() {
            // Given
            Long cardId = 1L;
            when(cardRepository.existsById(cardId)).thenReturn(true);
            when(reviewRecordRepository.findByCardId(cardId)).thenReturn(Optional.of(sampleReviewRecord));
            doNothing().when(reviewRecordRepository).deleteById(sampleReviewRecord.getId());
            doNothing().when(cardRepository).deleteById(cardId);

            // When
            cardService.deleteCard(cardId);

            // Then
            verify(cardRepository, times(1)).existsById(cardId);
            verify(reviewRecordRepository, times(1)).findByCardId(cardId);
            verify(reviewRecordRepository, times(1)).deleteById(sampleReviewRecord.getId());
            verify(cardRepository, times(1)).deleteById(cardId);
        }

        @Test
        @DisplayName("Should delete card even if review record doesn't exist")
        void shouldDeleteCardWithoutReviewRecord() {
            // Given
            Long cardId = 1L;
            when(cardRepository.existsById(cardId)).thenReturn(true);
            when(reviewRecordRepository.findByCardId(cardId)).thenReturn(Optional.empty());
            doNothing().when(cardRepository).deleteById(cardId);

            // When
            cardService.deleteCard(cardId);

            // Then
            verify(reviewRecordRepository, times(1)).findByCardId(cardId);
            verify(reviewRecordRepository, never()).deleteById(anyLong());
            verify(cardRepository, times(1)).deleteById(cardId);
        }

        @Test
        @DisplayName("Should throw CardNotFoundException when deleting non-existent card")
        void shouldThrowExceptionWhenDeletingNonExistentCard() {
            // Given
            Long nonExistentId = 999L;
            when(cardRepository.existsById(nonExistentId)).thenReturn(false);

            // When & Then
            assertThrows(CardNotFoundException.class, () -> {
                cardService.deleteCard(nonExistentId);
            });
            verify(cardRepository, times(1)).existsById(nonExistentId);
            verify(reviewRecordRepository, never()).findByCardId(anyLong());
            verify(cardRepository, never()).deleteById(anyLong());
        }
    }
}
