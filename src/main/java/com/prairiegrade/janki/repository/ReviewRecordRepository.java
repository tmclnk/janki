package com.prairiegrade.janki.repository;

import com.prairiegrade.janki.domain.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ReviewRecord entities.
 */
@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    /**
     * Find the review record for a specific card.
     *
     * @param cardId ID of the card
     * @return Optional containing the review record if found
     */
    Optional<ReviewRecord> findByCardId(Long cardId);

    /**
     * Find all review records that are due for review in a specific deck.
     *
     * @param deckId ID of the deck
     * @param now    Current timestamp
     * @param limit  Maximum number of records to return
     * @return List of due review records
     */
    @Query(value = "SELECT rr.* FROM review_records rr " +
           "INNER JOIN cards c ON rr.card_id = c.id " +
           "WHERE c.deck_id = :deckId AND rr.next_review_date <= :now " +
           "ORDER BY rr.next_review_date ASC LIMIT :limit",
           nativeQuery = true)
    List<ReviewRecord> findDueReviews(
            @Param("deckId") Long deckId,
            @Param("now") LocalDateTime now,
            @Param("limit") int limit
    );
}
