package com.prairiegrade.janki.service;

import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.dto.DeckStats;
import com.prairiegrade.janki.exception.DeckNotFoundException;
import com.prairiegrade.janki.repository.CardRepository;
import com.prairiegrade.janki.repository.DeckRepository;
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
 * Unit tests for DeckService covering CRUD operations and statistics.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeckService")
class DeckServiceTest {

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private DeckService deckService;

    private Deck sampleDeck;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        sampleDeck = Deck.builder()
                .id(1L)
                .name("Java Basics")
                .description("Core Java concepts")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Nested
    @DisplayName("Create Deck")
    class CreateDeck {

        @Test
        @DisplayName("Should create deck with name and description")
        void shouldCreateDeckSuccessfully() {
            // Given
            String name = "Java Basics";
            String description = "Core Java concepts";
            when(deckRepository.save(any(Deck.class))).thenReturn(sampleDeck);

            // When
            Deck result = deckService.createDeck(name, description);

            // Then
            assertNotNull(result);
            assertEquals(name, result.getName());
            assertEquals(description, result.getDescription());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
            assertEquals(result.getCreatedAt(), result.getUpdatedAt());

            verify(deckRepository, times(1)).save(any(Deck.class));
        }

        @Test
        @DisplayName("Should create deck with empty description")
        void shouldCreateDeckWithEmptyDescription() {
            // Given
            String name = "Quick Deck";
            String description = "";
            Deck deckWithEmptyDesc = Deck.builder()
                    .id(2L)
                    .name(name)
                    .description(description)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            when(deckRepository.save(any(Deck.class))).thenReturn(deckWithEmptyDesc);

            // When
            Deck result = deckService.createDeck(name, description);

            // Then
            assertNotNull(result);
            assertEquals(name, result.getName());
            assertEquals("", result.getDescription());
            verify(deckRepository, times(1)).save(any(Deck.class));
        }
    }

    @Nested
    @DisplayName("Get All Decks")
    class GetAllDecks {

        @Test
        @DisplayName("Should return all decks ordered by name")
        void shouldReturnAllDecksOrderedByName() {
            // Given
            Deck deck1 = Deck.builder().id(1L).name("Algebra").build();
            Deck deck2 = Deck.builder().id(2L).name("Biology").build();
            Deck deck3 = Deck.builder().id(3L).name("Chemistry").build();
            List<Deck> decks = Arrays.asList(deck1, deck2, deck3);
            when(deckRepository.findAllByOrderByNameAsc()).thenReturn(decks);

            // When
            List<Deck> result = deckService.getAllDecks();

            // Then
            assertEquals(3, result.size());
            assertEquals("Algebra", result.get(0).getName());
            assertEquals("Biology", result.get(1).getName());
            assertEquals("Chemistry", result.get(2).getName());
            verify(deckRepository, times(1)).findAllByOrderByNameAsc();
        }

        @Test
        @DisplayName("Should return empty list when no decks exist")
        void shouldReturnEmptyListWhenNoDecks() {
            // Given
            when(deckRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

            // When
            List<Deck> result = deckService.getAllDecks();

            // Then
            assertTrue(result.isEmpty());
            verify(deckRepository, times(1)).findAllByOrderByNameAsc();
        }
    }

    @Nested
    @DisplayName("Get Deck by ID")
    class GetDeckById {

        @Test
        @DisplayName("Should return deck when ID exists")
        void shouldReturnDeckWhenIdExists() {
            // Given
            Long deckId = 1L;
            when(deckRepository.findById(deckId)).thenReturn(Optional.of(sampleDeck));

            // When
            Deck result = deckService.getDeck(deckId);

            // Then
            assertNotNull(result);
            assertEquals(deckId, result.getId());
            assertEquals("Java Basics", result.getName());
            verify(deckRepository, times(1)).findById(deckId);
        }

        @Test
        @DisplayName("Should throw DeckNotFoundException when ID doesn't exist")
        void shouldThrowExceptionWhenIdNotFound() {
            // Given
            Long nonExistentId = 999L;
            when(deckRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(DeckNotFoundException.class, () -> {
                deckService.getDeck(nonExistentId);
            });
            verify(deckRepository, times(1)).findById(nonExistentId);
        }
    }

    @Nested
    @DisplayName("Update Deck")
    class UpdateDeck {

        @Test
        @DisplayName("Should update deck name and description")
        void shouldUpdateDeckSuccessfully() {
            // Given
            Long deckId = 1L;
            String newName = "Advanced Java";
            String newDescription = "Advanced Java topics";
            LocalDateTime originalCreatedAt = sampleDeck.getCreatedAt();

            when(deckRepository.findById(deckId)).thenReturn(Optional.of(sampleDeck));
            when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Deck result = deckService.updateDeck(deckId, newName, newDescription);

            // Then
            assertNotNull(result);
            assertEquals(newName, result.getName());
            assertEquals(newDescription, result.getDescription());
            assertEquals(originalCreatedAt, result.getCreatedAt());
            assertTrue(result.getUpdatedAt().isAfter(result.getCreatedAt()) ||
                      result.getUpdatedAt().isEqual(result.getCreatedAt()));

            verify(deckRepository, times(1)).findById(deckId);
            verify(deckRepository, times(1)).save(sampleDeck);
        }

        @Test
        @DisplayName("Should throw DeckNotFoundException when updating non-existent deck")
        void shouldThrowExceptionWhenUpdatingNonExistentDeck() {
            // Given
            Long nonExistentId = 999L;
            when(deckRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(DeckNotFoundException.class, () -> {
                deckService.updateDeck(nonExistentId, "New Name", "New Description");
            });
            verify(deckRepository, times(1)).findById(nonExistentId);
            verify(deckRepository, never()).save(any(Deck.class));
        }

        @Test
        @DisplayName("Should update only name when description is empty")
        void shouldUpdateOnlyName() {
            // Given
            Long deckId = 1L;
            String newName = "Updated Name";
            String emptyDescription = "";

            when(deckRepository.findById(deckId)).thenReturn(Optional.of(sampleDeck));
            when(deckRepository.save(any(Deck.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            Deck result = deckService.updateDeck(deckId, newName, emptyDescription);

            // Then
            assertEquals(newName, result.getName());
            assertEquals(emptyDescription, result.getDescription());
            verify(deckRepository, times(1)).save(sampleDeck);
        }
    }

    @Nested
    @DisplayName("Delete Deck")
    class DeleteDeck {

        @Test
        @DisplayName("Should delete deck when ID exists")
        void shouldDeleteDeckSuccessfully() {
            // Given
            Long deckId = 1L;
            when(deckRepository.existsById(deckId)).thenReturn(true);
            doNothing().when(deckRepository).deleteById(deckId);

            // When
            deckService.deleteDeck(deckId);

            // Then
            verify(deckRepository, times(1)).existsById(deckId);
            verify(deckRepository, times(1)).deleteById(deckId);
        }

        @Test
        @DisplayName("Should throw DeckNotFoundException when deleting non-existent deck")
        void shouldThrowExceptionWhenDeletingNonExistentDeck() {
            // Given
            Long nonExistentId = 999L;
            when(deckRepository.existsById(nonExistentId)).thenReturn(false);

            // When & Then
            assertThrows(DeckNotFoundException.class, () -> {
                deckService.deleteDeck(nonExistentId);
            });
            verify(deckRepository, times(1)).existsById(nonExistentId);
            verify(deckRepository, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("Get Deck Statistics")
    class GetDeckStatistics {

        @Test
        @DisplayName("Should return correct statistics for deck with cards")
        void shouldReturnCorrectStatistics() {
            // Given
            Long deckId = 1L;
            when(deckRepository.existsById(deckId)).thenReturn(true);
            when(cardRepository.countByDeckId(deckId)).thenReturn(10L);
            when(cardRepository.findByDeckIdAndState(deckId, "NEW")).thenReturn(List.of(/* 4 cards */
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class)
            ));
            when(cardRepository.findByDeckIdAndState(deckId, "LEARNING")).thenReturn(List.of(/* 3 cards */
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class)
            ));
            when(cardRepository.findByDeckIdAndState(deckId, "REVIEW")).thenReturn(List.of(/* 3 cards */
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class),
                    mock(com.prairiegrade.janki.domain.Card.class)
            ));

            // When
            DeckStats result = deckService.getDeckStats(deckId);

            // Then
            assertNotNull(result);
            assertEquals(10L, result.totalCards());
            assertEquals(4L, result.newCards());
            assertEquals(3L, result.learningCards());
            assertEquals(3L, result.reviewCards());

            verify(deckRepository, times(1)).existsById(deckId);
            verify(cardRepository, times(1)).countByDeckId(deckId);
            verify(cardRepository, times(3)).findByDeckIdAndState(eq(deckId), anyString());
        }

        @Test
        @DisplayName("Should return zero statistics for deck with no cards")
        void shouldReturnZeroStatisticsForEmptyDeck() {
            // Given
            Long deckId = 1L;
            when(deckRepository.existsById(deckId)).thenReturn(true);
            when(cardRepository.countByDeckId(deckId)).thenReturn(0L);
            when(cardRepository.findByDeckIdAndState(eq(deckId), anyString())).thenReturn(List.of());

            // When
            DeckStats result = deckService.getDeckStats(deckId);

            // Then
            assertEquals(0L, result.totalCards());
            assertEquals(0L, result.newCards());
            assertEquals(0L, result.learningCards());
            assertEquals(0L, result.reviewCards());
        }

        @Test
        @DisplayName("Should throw DeckNotFoundException when deck doesn't exist")
        void shouldThrowExceptionForNonExistentDeck() {
            // Given
            Long nonExistentId = 999L;
            when(deckRepository.existsById(nonExistentId)).thenReturn(false);

            // When & Then
            assertThrows(DeckNotFoundException.class, () -> {
                deckService.getDeckStats(nonExistentId);
            });
            verify(deckRepository, times(1)).existsById(nonExistentId);
            verify(cardRepository, never()).countByDeckId(anyLong());
        }
    }
}
