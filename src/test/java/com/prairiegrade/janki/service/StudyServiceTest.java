package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.ReviewRecord;
import com.prairiegrade.janki.dto.ReviewResult;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudyService covering due cards, new cards, and rating workflow.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StudyService")
class StudyServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private ReviewRecordRepository reviewRecordRepository;

    @Mock
    private SM2Service sm2Service;

    @InjectMocks
    private StudyService studyService;

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
    @DisplayName("Get Due Cards")
    class GetDueCards {

        @Test
        @DisplayName("Should return due cards with batch fetching (N+1 fix)")
        void shouldReturnDueCardsWithBatchFetching() {
            // Given
            Long deckId = 10L;
            int limit = 10;
            LocalDateTime now = LocalDateTime.now();

            ReviewRecord review1 = ReviewRecord.builder().id(1L).cardId(1L).easeFactor(2.5).build();
            ReviewRecord review2 = ReviewRecord.builder().id(2L).cardId(2L).easeFactor(2.4).build();
            ReviewRecord review3 = ReviewRecord.builder().id(3L).cardId(3L).easeFactor(2.6).build();
            List<ReviewRecord> dueReviews = Arrays.asList(review1, review2, review3);

            Card card1 = Card.builder().id(1L).deckId(deckId).front("Q1").back("A1").build();
            Card card2 = Card.builder().id(2L).deckId(deckId).front("Q2").back("A2").build();
            Card card3 = Card.builder().id(3L).deckId(deckId).front("Q3").back("A3").build();
            List<Card> cards = Arrays.asList(card1, card2, card3);

            when(reviewRecordRepository.findDueReviews(eq(deckId), any(LocalDateTime.class), eq(limit)))
                    .thenReturn(dueReviews);
            when(cardRepository.findAllByIds(anyList())).thenReturn(cards);

            // When
            List<StudyService.StudyCard> result = studyService.getDueCards(deckId, limit);

            // Then
            assertEquals(3, result.size());

            // Verify batch fetching: findAllByIds called once with all card IDs
            verify(cardRepository, times(1)).findAllByIds(Arrays.asList(1L, 2L, 3L));

            // Verify no individual card fetches (N+1 problem avoided)
            verify(cardRepository, never()).findById(anyLong());

            // Verify correct pairing of cards and review records
            assertEquals(1L, result.get(0).card().getId());
            assertEquals(1L, result.get(0).reviewRecord().getCardId());
            assertEquals(2L, result.get(1).card().getId());
            assertEquals(2L, result.get(1).reviewRecord().getCardId());
        }

        @Test
        @DisplayName("Should return empty list when no cards are due")
        void shouldReturnEmptyListWhenNoCardsDue() {
            // Given
            Long deckId = 10L;
            when(reviewRecordRepository.findDueReviews(eq(deckId), any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of());
            when(cardRepository.findAllByIds(anyList())).thenReturn(List.of());

            // When
            List<StudyService.StudyCard> result = studyService.getDueCards(deckId, 10);

            // Then
            assertTrue(result.isEmpty());
            verify(reviewRecordRepository, times(1)).findDueReviews(eq(deckId), any(LocalDateTime.class), eq(10));
            // Note: findAllByIds is still called with empty list (batch fetching optimization)
            verify(cardRepository, times(1)).findAllByIds(List.of());
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() {
            // Given
            Long deckId = 10L;
            int limit = 5;

            List<ReviewRecord> limitedReviews = Arrays.asList(
                    ReviewRecord.builder().id(1L).cardId(1L).build(),
                    ReviewRecord.builder().id(2L).cardId(2L).build()
            );

            when(reviewRecordRepository.findDueReviews(eq(deckId), any(LocalDateTime.class), eq(limit)))
                    .thenReturn(limitedReviews);
            when(cardRepository.findAllByIds(anyList())).thenReturn(Arrays.asList(
                    Card.builder().id(1L).build(),
                    Card.builder().id(2L).build()
            ));

            // When
            List<StudyService.StudyCard> result = studyService.getDueCards(deckId, limit);

            // Then
            assertEquals(2, result.size());
            verify(reviewRecordRepository, times(1)).findDueReviews(eq(deckId), any(LocalDateTime.class), eq(limit));
        }

        @Test
        @DisplayName("Should throw exception when card not found for review record")
        void shouldThrowExceptionWhenCardNotFoundForReview() {
            // Given
            Long deckId = 10L;
            ReviewRecord orphanedReview = ReviewRecord.builder().id(1L).cardId(999L).build();
            when(reviewRecordRepository.findDueReviews(eq(deckId), any(LocalDateTime.class), anyInt()))
                    .thenReturn(List.of(orphanedReview));
            when(cardRepository.findAllByIds(anyList())).thenReturn(List.of()); // Card not found

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                studyService.getDueCards(deckId, 10);
            });
        }
    }

    @Nested
    @DisplayName("Get New Cards")
    class GetNewCards {

        @Test
        @DisplayName("Should return new cards with existing review records")
        void shouldReturnNewCardsWithExistingReviewRecords() {
            // Given
            Long deckId = 10L;
            int limit = 10;

            Card newCard1 = Card.builder().id(1L).deckId(deckId).state("NEW").build();
            Card newCard2 = Card.builder().id(2L).deckId(deckId).state("NEW").build();
            List<Card> newCards = Arrays.asList(newCard1, newCard2);

            ReviewRecord review1 = ReviewRecord.builder().id(1L).cardId(1L).easeFactor(2.5).build();
            ReviewRecord review2 = ReviewRecord.builder().id(2L).cardId(2L).easeFactor(2.5).build();

            when(cardRepository.findByDeckIdAndState(deckId, "NEW")).thenReturn(newCards);
            when(reviewRecordRepository.findByCardId(1L)).thenReturn(Optional.of(review1));
            when(reviewRecordRepository.findByCardId(2L)).thenReturn(Optional.of(review2));

            // When
            List<StudyService.StudyCard> result = studyService.getNewCards(deckId, limit);

            // Then
            assertEquals(2, result.size());
            verify(reviewRecordRepository, never()).save(any(ReviewRecord.class)); // No creation needed
        }

        @Test
        @DisplayName("Should create review records for new cards without them")
        void shouldCreateReviewRecordsForCardsWithoutThem() {
            // Given
            Long deckId = 10L;
            Card newCard = Card.builder().id(1L).deckId(deckId).state("NEW").build();

            ReviewRecord newReviewRecord = ReviewRecord.builder()
                    .id(1L)
                    .cardId(1L)
                    .easeFactor(2.5)
                    .repetitions(0)
                    .intervalDays(0)
                    .nextReviewDate(LocalDateTime.now())
                    .build();

            when(cardRepository.findByDeckIdAndState(deckId, "NEW")).thenReturn(List.of(newCard));
            when(reviewRecordRepository.findByCardId(1L)).thenReturn(Optional.empty()); // No existing record
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenReturn(newReviewRecord);

            // When
            List<StudyService.StudyCard> result = studyService.getNewCards(deckId, 10);

            // Then
            assertEquals(1, result.size());

            // Verify review record created with default SM-2 values
            verify(reviewRecordRepository, times(1)).save(argThat(reviewRecord ->
                    reviewRecord.getCardId().equals(1L) &&
                    reviewRecord.getEaseFactor() == 2.5 &&
                    reviewRecord.getRepetitions() == 0 &&
                    reviewRecord.getIntervalDays() == 0
            ));
        }

        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitForNewCards() {
            // Given
            Long deckId = 10L;
            int limit = 2;

            List<Card> manyNewCards = Arrays.asList(
                    Card.builder().id(1L).state("NEW").build(),
                    Card.builder().id(2L).state("NEW").build(),
                    Card.builder().id(3L).state("NEW").build(),
                    Card.builder().id(4L).state("NEW").build(),
                    Card.builder().id(5L).state("NEW").build()
            );

            when(cardRepository.findByDeckIdAndState(deckId, "NEW")).thenReturn(manyNewCards);
            when(reviewRecordRepository.findByCardId(anyLong()))
                    .thenReturn(Optional.of(sampleReviewRecord));

            // When
            List<StudyService.StudyCard> result = studyService.getNewCards(deckId, limit);

            // Then
            assertEquals(2, result.size(), "Should only return limited number of cards");
        }

        @Test
        @DisplayName("Should return empty list when no new cards exist")
        void shouldReturnEmptyListWhenNoNewCards() {
            // Given
            Long deckId = 10L;
            when(cardRepository.findByDeckIdAndState(deckId, "NEW")).thenReturn(List.of());

            // When
            List<StudyService.StudyCard> result = studyService.getNewCards(deckId, 10);

            // Then
            assertTrue(result.isEmpty());
            verify(reviewRecordRepository, never()).findByCardId(anyLong());
        }
    }

    @Nested
    @DisplayName("Rate Card")
    class RateCard {

        @Test
        @DisplayName("Should rate card and update state to LEARNING")
        void shouldRateCardAndUpdateToLearning() {
            // Given
            Long cardId = 1L;
            SM2Service.UserRating rating = SM2Service.UserRating.GOOD;

            SM2Service.SM2Result sm2Result = new SM2Service.SM2Result(
                    2.36, // easeFactor
                    1,    // repetitions
                    1,    // intervalDays
                    LocalDateTime.now().plusDays(1),
                    "LEARNING"
            );

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));
            when(reviewRecordRepository.findByCardId(cardId)).thenReturn(Optional.of(sampleReviewRecord));
            when(sm2Service.mapRatingToQuality(rating)).thenReturn(3);
            when(sm2Service.calculateNext(anyInt(), anyDouble(), anyInt(), anyInt())).thenReturn(sm2Result);
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ReviewResult result = studyService.rateCard(cardId, rating);

            // Then
            assertNotNull(result);
            assertEquals(cardId, result.cardId());
            assertEquals("LEARNING", result.newState());
            assertEquals(1, result.intervalDays());
            assertEquals(2.36, result.easeFactor(), 0.01);

            // Verify SM2Service integration
            verify(sm2Service, times(1)).mapRatingToQuality(rating);
            verify(sm2Service, times(1)).calculateNext(3, 2.5, 0, 0);

            // Verify card state updated
            verify(cardRepository, times(1)).save(argThat(card ->
                    card.getState().equals("LEARNING")
            ));

            // Verify review record updated
            verify(reviewRecordRepository, times(1)).save(argThat(reviewRecord ->
                    reviewRecord.getEaseFactor() == 2.36 &&
                    reviewRecord.getRepetitions() == 1 &&
                    reviewRecord.getIntervalDays() == 1 &&
                    reviewRecord.getLastReviewedAt() != null
            ));
        }

        @Test
        @DisplayName("Should graduate card to REVIEW state")
        void shouldGraduateCardToReview() {
            // Given
            Long cardId = 1L;
            sampleCard.setState("LEARNING");
            sampleReviewRecord.setRepetitions(1);
            sampleReviewRecord.setIntervalDays(1);

            SM2Service.SM2Result sm2Result = new SM2Service.SM2Result(
                    2.36,
                    2,
                    6,
                    LocalDateTime.now().plusDays(6),
                    "REVIEW"
            );

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));
            when(reviewRecordRepository.findByCardId(cardId)).thenReturn(Optional.of(sampleReviewRecord));
            when(sm2Service.mapRatingToQuality(SM2Service.UserRating.GOOD)).thenReturn(3);
            when(sm2Service.calculateNext(anyInt(), anyDouble(), anyInt(), anyInt())).thenReturn(sm2Result);
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ReviewResult result = studyService.rateCard(cardId, SM2Service.UserRating.GOOD);

            // Then
            assertEquals("REVIEW", result.newState());
            assertEquals(6, result.intervalDays());
            verify(cardRepository, times(1)).save(argThat(card ->
                    card.getState().equals("REVIEW")
            ));
        }

        @Test
        @DisplayName("Should reset card to LEARNING on failed review")
        void shouldResetCardToLearningOnFailure() {
            // Given
            Long cardId = 1L;
            sampleCard.setState("REVIEW");
            sampleReviewRecord.setRepetitions(5);
            sampleReviewRecord.setIntervalDays(30);

            SM2Service.SM2Result sm2Result = new SM2Service.SM2Result(
                    1.7,  // Decreased ease factor
                    0,    // Reset repetitions
                    0,    // Reset interval
                    LocalDateTime.now(),
                    "LEARNING"
            );

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));
            when(reviewRecordRepository.findByCardId(cardId)).thenReturn(Optional.of(sampleReviewRecord));
            when(sm2Service.mapRatingToQuality(SM2Service.UserRating.AGAIN)).thenReturn(0);
            when(sm2Service.calculateNext(anyInt(), anyDouble(), anyInt(), anyInt())).thenReturn(sm2Result);
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ReviewResult result = studyService.rateCard(cardId, SM2Service.UserRating.AGAIN);

            // Then
            assertEquals("LEARNING", result.newState());
            assertEquals(0, result.intervalDays());
            verify(reviewRecordRepository, times(1)).save(argThat(reviewRecord ->
                    reviewRecord.getRepetitions() == 0 &&
                    reviewRecord.getIntervalDays() == 0
            ));
        }

        @Test
        @DisplayName("Should throw exception when card not found")
        void shouldThrowExceptionWhenCardNotFound() {
            // Given
            Long nonExistentId = 999L;
            when(cardRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                studyService.rateCard(nonExistentId, SM2Service.UserRating.GOOD);
            });
            verify(sm2Service, never()).calculateNext(anyInt(), anyDouble(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should throw exception when review record not found")
        void shouldThrowExceptionWhenReviewRecordNotFound() {
            // Given
            Long cardId = 1L;
            when(cardRepository.findById(cardId)).thenReturn(Optional.of(sampleCard));
            when(reviewRecordRepository.findByCardId(cardId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class, () -> {
                studyService.rateCard(cardId, SM2Service.UserRating.GOOD);
            });
            verify(sm2Service, never()).calculateNext(anyInt(), anyDouble(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("Should handle all rating types correctly")
        void shouldHandleAllRatingTypes() {
            // Given
            when(cardRepository.findById(anyLong())).thenReturn(Optional.of(sampleCard));
            when(reviewRecordRepository.findByCardId(anyLong())).thenReturn(Optional.of(sampleReviewRecord));
            when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(reviewRecordRepository.save(any(ReviewRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Test each rating type
            SM2Service.UserRating[] ratings = {
                    SM2Service.UserRating.AGAIN,
                    SM2Service.UserRating.HARD,
                    SM2Service.UserRating.GOOD,
                    SM2Service.UserRating.EASY
            };

            for (SM2Service.UserRating rating : ratings) {
                // Mock SM2 service responses
                when(sm2Service.mapRatingToQuality(rating)).thenReturn(
                        switch (rating) {
                            case AGAIN -> 0;
                            case HARD -> 2;
                            case GOOD -> 3;
                            case EASY -> 4;
                        }
                );
                when(sm2Service.calculateNext(anyInt(), anyDouble(), anyInt(), anyInt()))
                        .thenReturn(new SM2Service.SM2Result(2.5, 1, 1, LocalDateTime.now().plusDays(1), "LEARNING"));

                // When
                ReviewResult result = studyService.rateCard(1L, rating);

                // Then
                assertNotNull(result, "Result should not be null for rating: " + rating);
            }

            // Verify all ratings were processed
            verify(sm2Service, times(4)).mapRatingToQuality(any());
            verify(sm2Service, times(4)).calculateNext(anyInt(), anyDouble(), anyInt(), anyInt());
        }
    }
}
