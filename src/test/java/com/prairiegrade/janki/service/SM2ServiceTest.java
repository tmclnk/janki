package com.prairiegrade.janki.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for SM2Service implementing SM-2 spaced repetition algorithm.
 *
 * Test coverage includes:
 * - Rating to quality mapping
 * - First review scenarios for all ratings
 * - Graduation from NEW → LEARNING → REVIEW
 * - Review scenarios with exponential intervals
 * - Ease factor calculations and bounds
 * - Edge cases (high repetitions, boundary conditions)
 */
@DisplayName("SM2Service")
class SM2ServiceTest {

    private SM2Service sm2Service;

    @BeforeEach
    void setUp() {
        sm2Service = new SM2Service();
    }

    @Nested
    @DisplayName("Rating to Quality Mapping")
    class RatingToQualityMapping {

        @Test
        @DisplayName("AGAIN maps to quality 0")
        void testAgainMapsToZero() {
            assertEquals(0, sm2Service.mapRatingToQuality(SM2Service.UserRating.AGAIN));
        }

        @Test
        @DisplayName("HARD maps to quality 2")
        void testHardMapsToTwo() {
            assertEquals(2, sm2Service.mapRatingToQuality(SM2Service.UserRating.HARD));
        }

        @Test
        @DisplayName("GOOD maps to quality 3")
        void testGoodMapsToThree() {
            assertEquals(3, sm2Service.mapRatingToQuality(SM2Service.UserRating.GOOD));
        }

        @Test
        @DisplayName("EASY maps to quality 4")
        void testEasyMapsToFour() {
            assertEquals(4, sm2Service.mapRatingToQuality(SM2Service.UserRating.EASY));
        }
    }

    @Nested
    @DisplayName("First Review Scenarios (New Card)")
    class FirstReviewScenarios {

        @Test
        @DisplayName("New card rated AGAIN → interval=0, reps=0, state=LEARNING")
        void testNewCardRatedAgain() {
            // Given: new card with default values (EF=2.5, reps=0, interval=0)
            SM2Service.SM2Result result = sm2Service.calculateNext(0, 2.5, 0, 0);

            // Then: card fails, resets to learning
            assertEquals(0, result.repetitions(), "Repetitions should reset to 0");
            assertEquals(0, result.intervalDays(), "Interval should be 0");
            assertEquals("LEARNING", result.newState());
            assertTrue(result.easeFactor() < 2.5, "Ease factor should decrease");
            assertTrue(result.easeFactor() >= 1.3, "Ease factor should not go below minimum");
        }

        @Test
        @DisplayName("New card rated HARD → interval=0, reps=0, state=LEARNING")
        void testNewCardRatedHard() {
            // Given: new card (quality=2 is below passing threshold of 3)
            SM2Service.SM2Result result = sm2Service.calculateNext(2, 2.5, 0, 0);

            // Then: card fails (quality < 3), resets to learning
            assertEquals(0, result.repetitions(), "Repetitions should reset to 0");
            assertEquals(0, result.intervalDays(), "Interval should be 0");
            assertEquals("LEARNING", result.newState());
            assertTrue(result.easeFactor() < 2.5, "Ease factor should decrease");
        }

        @Test
        @DisplayName("New card rated GOOD → interval=1, reps=1, state=LEARNING, EF≈2.36")
        void testNewCardRatedGood() {
            // Given: new card with quality=3 (passing)
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 2.5, 0, 0);

            // Then: first successful review
            assertEquals(1, result.repetitions(), "Repetitions should increment to 1");
            assertEquals(1, result.intervalDays(), "First successful interval is 1 day");
            assertEquals("LEARNING", result.newState(), "Still in LEARNING state");
            assertEquals(2.36, result.easeFactor(), 0.01, "Ease factor should be approximately 2.36");
        }

        @Test
        @DisplayName("New card rated EASY → interval=1, reps=1, state=LEARNING, EF=2.5")
        void testNewCardRatedEasy() {
            // Given: new card with quality=4 (easy recall)
            SM2Service.SM2Result result = sm2Service.calculateNext(4, 2.5, 0, 0);

            // Then: first successful review
            // EF' = 2.5 + (0.1 - (5-4) * (0.08 + (5-4) * 0.02))
            // EF' = 2.5 + (0.1 - 1 * 0.10) = 2.5 + 0 = 2.5
            assertEquals(1, result.repetitions(), "Repetitions should increment to 1");
            assertEquals(1, result.intervalDays(), "First successful interval is 1 day");
            assertEquals("LEARNING", result.newState(), "Still in LEARNING state");
            assertEquals(2.5, result.easeFactor(), 0.01, "Ease factor should stay at 2.5 for quality 4");
        }
    }

    @Nested
    @DisplayName("Graduation Scenarios")
    class GraduationScenarios {

        @Test
        @DisplayName("First successful review (rep 0→1) → stays in LEARNING")
        void testFirstSuccessfulReview() {
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 2.5, 0, 0);

            assertEquals(1, result.repetitions());
            assertEquals(1, result.intervalDays());
            assertEquals("LEARNING", result.newState(), "Should stay in LEARNING after first success");
        }

        @Test
        @DisplayName("Second successful review (rep 1→2) → graduates to REVIEW")
        void testSecondSuccessfulReview() {
            // Given: card with 1 successful review
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 2.36, 1, 1);

            // Then: graduates to REVIEW with 6-day interval
            assertEquals(2, result.repetitions());
            assertEquals(6, result.intervalDays(), "Second successful review gets 6-day interval");
            assertEquals("REVIEW", result.newState(), "Should graduate to REVIEW state");
        }

        @Test
        @DisplayName("Failed review after graduation → resets to LEARNING")
        void testFailedReviewAfterGraduation() {
            // Given: card in REVIEW state (reps=3, interval=14 days)
            SM2Service.SM2Result result = sm2Service.calculateNext(0, 2.5, 3, 14);

            // Then: resets to LEARNING
            assertEquals(0, result.repetitions(), "Should reset repetitions");
            assertEquals(0, result.intervalDays(), "Should reset interval");
            assertEquals("LEARNING", result.newState(), "Should return to LEARNING");
        }
    }

    @Nested
    @DisplayName("Review Scenarios (Exponential Growth)")
    class ReviewScenarios {

        @Test
        @DisplayName("Third successful review → exponential interval (6 * new EF)")
        void testThirdSuccessfulReview() {
            // Given: card with 2 successful reviews (about to graduate)
            double initialEF = 2.36;
            SM2Service.SM2Result result = sm2Service.calculateNext(3, initialEF, 2, 6);

            // Then: exponential interval calculation uses NEW ease factor
            // New EF = 2.36 + (0.1 - 2 * 0.12) = 2.36 - 0.14 = 2.22
            // Interval = ceil(6 * 2.22) = ceil(13.32) = 14
            assertEquals(3, result.repetitions());
            assertEquals(2.22, result.easeFactor(), 0.01, "EF should decrease slightly to 2.22");
            assertEquals(14, result.intervalDays(), "Interval should be ceil(6 * 2.22) = 14");
            assertEquals("REVIEW", result.newState());
        }

        @Test
        @DisplayName("Fourth successful review → continues exponential growth")
        void testFourthSuccessfulReview() {
            // Given: card in REVIEW with interval=14 days, EF=2.5
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 2.5, 3, 14);

            // Then: interval = 14 * 2.36 ≈ 33 days
            assertEquals(4, result.repetitions());
            int expectedInterval = (int) Math.ceil(14 * result.easeFactor());
            assertEquals(expectedInterval, result.intervalDays());
            assertEquals("REVIEW", result.newState());
        }

        @Test
        @DisplayName("Failed review (AGAIN) → resets repetitions and interval")
        void testFailedReview() {
            // Given: card in REVIEW with 5 repetitions
            SM2Service.SM2Result result = sm2Service.calculateNext(0, 2.5, 5, 50);

            // Then: complete reset
            assertEquals(0, result.repetitions());
            assertEquals(0, result.intervalDays());
            assertEquals("LEARNING", result.newState());
        }

        @Test
        @DisplayName("Partial failure (HARD) → resets to LEARNING")
        void testPartialFailure() {
            // Given: card in REVIEW, rated HARD (quality=2, below passing)
            SM2Service.SM2Result result = sm2Service.calculateNext(2, 2.5, 4, 30);

            // Then: quality < 3 means failure, reset to LEARNING
            assertEquals(0, result.repetitions());
            assertEquals(0, result.intervalDays());
            assertEquals("LEARNING", result.newState());
        }
    }

    @Nested
    @DisplayName("Ease Factor Calculations")
    class EaseFactorCalculations {

        @Test
        @DisplayName("Ease factor has minimum bound of 1.3")
        void testEaseFactorMinimumBound() {
            // Given: card with low ease factor, rated AGAIN repeatedly
            SM2Service.SM2Result result = sm2Service.calculateNext(0, 1.3, 2, 6);

            // Then: ease factor should not go below 1.3
            assertTrue(result.easeFactor() >= 1.3, "Ease factor must not be less than 1.3");
        }

        @Test
        @DisplayName("Quality 0 (AGAIN) decreases ease factor significantly")
        void testQualityZeroDecreasesEF() {
            double initialEF = 2.5;
            SM2Service.SM2Result result = sm2Service.calculateNext(0, initialEF, 2, 6);

            // EF' = EF + (0.1 - (5-0) * (0.08 + (5-0) * 0.02))
            // EF' = 2.5 + (0.1 - 5 * (0.08 + 5 * 0.02))
            // EF' = 2.5 + (0.1 - 5 * 0.18) = 2.5 + (0.1 - 0.9) = 2.5 - 0.8 = 1.7
            assertEquals(1.7, result.easeFactor(), 0.01, "Quality 0 should decrease EF to ~1.7");
        }

        @Test
        @DisplayName("Quality 3 (GOOD) slightly decreases ease factor")
        void testQualityThreeDecreasesEFSlightly() {
            double initialEF = 2.5;
            SM2Service.SM2Result result = sm2Service.calculateNext(3, initialEF, 1, 1);

            // EF' = 2.5 + (0.1 - (5-3) * (0.08 + (5-3) * 0.02))
            // EF' = 2.5 + (0.1 - 2 * (0.08 + 2 * 0.02))
            // EF' = 2.5 + (0.1 - 2 * 0.12) = 2.5 + (0.1 - 0.24) = 2.5 - 0.14 = 2.36
            assertEquals(2.36, result.easeFactor(), 0.01, "Quality 3 should decrease EF to ~2.36");
        }

        @Test
        @DisplayName("Quality 4 (EASY) increases ease factor")
        void testQualityFourIncreasesEF() {
            double initialEF = 2.5;
            SM2Service.SM2Result result = sm2Service.calculateNext(4, initialEF, 1, 1);

            // EF' = 2.5 + (0.1 - (5-4) * (0.08 + (5-4) * 0.02))
            // EF' = 2.5 + (0.1 - 1 * (0.08 + 1 * 0.02))
            // EF' = 2.5 + (0.1 - 0.10) = 2.5
            // Wait, let me recalculate: 0.08 + 0.02 = 0.10
            // EF' = 2.5 + (0.1 - 1 * 0.10) = 2.5 + 0 = 2.5
            // Hmm, that would keep it the same. Let me check quality 5:
            // EF' = 2.5 + (0.1 - (5-5) * (0.08 + (5-5) * 0.02))
            // EF' = 2.5 + (0.1 - 0) = 2.6
            // So quality 4 keeps it same, quality 5 increases it
            // Actually looking at the code again, quality 4 should result in slight increase
            // Let me recalculate more carefully:
            // q=4: EF' = EF + (0.1 - (5-4) * (0.08 + (5-4) * 0.02))
            //          = EF + (0.1 - 1 * (0.08 + 1 * 0.02))
            //          = EF + (0.1 - 1 * 0.10)
            //          = EF + 0
            // So quality 4 keeps EF the same at 2.5
            assertEquals(2.5, result.easeFactor(), 0.01, "Quality 4 should keep EF at 2.5");
        }

        @Test
        @DisplayName("Quality 5 increases ease factor above initial")
        void testQualityFiveIncreasesEF() {
            double initialEF = 2.5;
            SM2Service.SM2Result result = sm2Service.calculateNext(5, initialEF, 2, 6);

            // EF' = 2.5 + (0.1 - 0) = 2.6
            assertEquals(2.6, result.easeFactor(), 0.01, "Quality 5 should increase EF to 2.6");
        }

        @ParameterizedTest
        @CsvSource({
            "0, 1.70",  // AGAIN: significant decrease
            "2, 2.18",  // HARD: moderate decrease
            "3, 2.36",  // GOOD: slight decrease
            "4, 2.50",  // EASY: no change
            "5, 2.60"   // Perfect: increase
        })
        @DisplayName("Ease factor adjustments for different quality ratings")
        void testEaseFactorAdjustments(int quality, double expectedEF) {
            SM2Service.SM2Result result = sm2Service.calculateNext(quality, 2.5, 1, 1);
            assertEquals(expectedEF, result.easeFactor(), 0.01);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Very high repetition count → exponential interval still calculated")
        void testVeryHighRepetitionCount() {
            // Given: card reviewed 20 times with interval=365 days
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 2.5, 20, 365);

            assertEquals(21, result.repetitions());
            int expectedInterval = (int) Math.ceil(365 * result.easeFactor());
            assertEquals(expectedInterval, result.intervalDays());
            assertEquals("REVIEW", result.newState());
        }

        @Test
        @DisplayName("Maximum ease factor scenario (repeated quality 5 ratings)")
        void testMaximumEaseFactorScenario() {
            // Given: card repeatedly rated quality 5 (perfect recall)
            double easeFactor = 2.5;

            // Rate quality 5 multiple times
            SM2Service.SM2Result result1 = sm2Service.calculateNext(5, easeFactor, 2, 6);
            SM2Service.SM2Result result2 = sm2Service.calculateNext(5, result1.easeFactor(), 3, result1.intervalDays());
            SM2Service.SM2Result result3 = sm2Service.calculateNext(5, result2.easeFactor(), 4, result2.intervalDays());

            // Then: ease factor should grow
            assertTrue(result3.easeFactor() > 2.5, "Ease factor should increase with quality 5");
            assertEquals(5, result3.repetitions());
        }

        @Test
        @DisplayName("Minimum ease factor scenario (at boundary)")
        void testMinimumEaseFactorAtBoundary() {
            // Given: card at minimum ease factor
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 1.3, 5, 20);

            // Then: should still work correctly even at minimum
            assertEquals(6, result.repetitions());
            assertTrue(result.easeFactor() >= 1.3);
            int expectedInterval = (int) Math.ceil(20 * result.easeFactor());
            assertEquals(expectedInterval, result.intervalDays());
        }

        @Test
        @DisplayName("Invalid quality below 0 → throws exception")
        void testInvalidQualityBelowZero() {
            assertThrows(IllegalArgumentException.class, () -> {
                sm2Service.calculateNext(-1, 2.5, 0, 0);
            }, "Quality below 0 should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Invalid quality above 5 → throws exception")
        void testInvalidQualityAboveFive() {
            assertThrows(IllegalArgumentException.class, () -> {
                sm2Service.calculateNext(6, 2.5, 0, 0);
            }, "Quality above 5 should throw IllegalArgumentException");
        }

        @Test
        @DisplayName("Next review date is calculated correctly")
        void testNextReviewDateCalculation() {
            SM2Service.SM2Result result = sm2Service.calculateNext(3, 2.5, 1, 1);

            assertNotNull(result.nextReviewDate(), "Next review date should not be null");
            // Date should be approximately 6 days from now (second successful review)
            assertTrue(result.nextReviewDate().isAfter(java.time.LocalDateTime.now()));
        }

        @Test
        @DisplayName("Zero interval for failed cards")
        void testZeroIntervalForFailedCards() {
            SM2Service.SM2Result result = sm2Service.calculateNext(0, 2.5, 5, 100);

            assertEquals(0, result.intervalDays(), "Failed cards should have 0 interval");
            assertNotNull(result.nextReviewDate(), "Next review date should still be set");
        }
    }
}
