package com.prairiegrade.janki.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SM-2 (SuperMemo 2) Spaced Repetition Algorithm Service.
 *
 * Implements the SuperMemo 2 algorithm for calculating optimal review intervals
 * based on user performance ratings.
 *
 * @see <a href="https://super-memory.com/english/ol/sm2.htm">SM-2 Algorithm</a>
 */
@Service
public class SM2Service {

    /**
     * User-friendly rating options that map to quality values.
     */
    public enum UserRating {
        AGAIN,  // Complete failure (quality 0)
        HARD,   // Difficult recall (quality 2)
        GOOD,   // Standard recall (quality 3)
        EASY    // Trivial recall (quality 4)
    }

    /**
     * Card state in the learning process.
     */
    public enum CardState {
        NEW,      // Never studied
        LEARNING, // In learning phase (intervals < 6 days)
        REVIEW    // Graduated to review (intervals >= 6 days)
    }

    /**
     * Result of SM-2 calculation containing all updated parameters.
     */
    public record SM2Result(
            double easeFactor,
            int repetitions,
            int intervalDays,
            LocalDateTime nextReviewDate,
            String newState
    ) {}

    /**
     * Calculate next review parameters based on quality rating using SM-2 algorithm.
     *
     * Algorithm:
     * - EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
     * - EF minimum is 1.3
     * - If quality < 3: reset to learning, interval = 0
     * - If quality >= 3: increment repetitions
     *   - rep 1: interval = 1 day
     *   - rep 2: interval = 6 days (graduate to REVIEW)
     *   - rep >= 3: interval = previous_interval * EF
     *
     * @param quality Quality rating (0-5): 0=again, 2=hard, 3=good, 4=easy
     * @param currentEF Current ease factor
     * @param currentRepetitions Current repetition count
     * @param currentInterval Current interval in days
     * @return SM2Result with new parameters
     * @throws IllegalArgumentException if quality is not in range 0-5
     */
    public SM2Result calculateNext(
            int quality,
            double currentEF,
            int currentRepetitions,
            int currentInterval
    ) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be 0-5, got: " + quality);
        }

        // Calculate new ease factor
        // Formula: EF' = EF + (0.1 - (5-q) * (0.08 + (5-q) * 0.02))
        double newEF = currentEF + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));

        // Enforce minimum ease factor of 1.3
        if (newEF < 1.3) {
            newEF = 1.3;
        }

        int newRepetitions;
        int newInterval;
        String newState;

        if (quality < 3) {
            // Failed: reset to learning but keep ease factor
            newRepetitions = 0;
            newInterval = 0;
            newState = CardState.LEARNING.name();
        } else {
            // Passed: increment repetitions and calculate interval
            newRepetitions = currentRepetitions + 1;

            if (newRepetitions == 1) {
                // First successful review: 1 day
                newInterval = 1;
                newState = CardState.LEARNING.name();
            } else if (newRepetitions == 2) {
                // Second successful review: 6 days, graduate to REVIEW
                newInterval = 6;
                newState = CardState.REVIEW.name();
            } else {
                // Subsequent reviews: multiply previous interval by ease factor
                newInterval = (int) Math.ceil(currentInterval * newEF);
                newState = CardState.REVIEW.name();
            }
        }

        LocalDateTime nextReviewDate = LocalDateTime.now().plusDays(newInterval);

        return new SM2Result(newEF, newRepetitions, newInterval, nextReviewDate, newState);
    }

    /**
     * Map user-friendly rating to quality value for SM-2 algorithm.
     *
     * @param rating User rating (AGAIN, HARD, GOOD, EASY)
     * @return Quality value (0, 2, 3, or 4)
     */
    public int mapRatingToQuality(UserRating rating) {
        return switch (rating) {
            case AGAIN -> 0;
            case HARD -> 2;
            case GOOD -> 3;
            case EASY -> 4;
        };
    }
}
