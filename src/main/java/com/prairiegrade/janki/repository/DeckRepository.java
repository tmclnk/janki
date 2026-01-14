package com.prairiegrade.janki.repository;

import com.prairiegrade.janki.domain.Deck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Deck entities.
 */
@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {

    /**
     * Find all decks ordered by name.
     *
     * @return List of all decks sorted alphabetically by name
     */
    List<Deck> findAllByOrderByNameAsc();
}
