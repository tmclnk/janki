package com.prairiegrade.janki.controller.api;

import com.prairiegrade.janki.dto.request.RateCardRequest;
import com.prairiegrade.janki.dto.ReviewResult;
import com.prairiegrade.janki.dto.response.StudyCardResponse;
import com.prairiegrade.janki.service.StudyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
public class StudyRestController {

    private final StudyService studyService;

    @GetMapping("/decks/{deckId}/due")
    public ResponseEntity<List<StudyCardResponse>> getDueCards(
            @PathVariable Long deckId,
            @RequestParam(defaultValue = "20") int limit) {
        List<StudyCardResponse> dueCards = studyService.getDueCards(deckId, limit)
                .stream()
                .map(StudyCardResponse::from)
                .toList();
        return ResponseEntity.ok(dueCards);
    }

    @GetMapping("/decks/{deckId}/new")
    public ResponseEntity<List<StudyCardResponse>> getNewCards(
            @PathVariable Long deckId,
            @RequestParam(defaultValue = "20") int limit) {
        List<StudyCardResponse> newCards = studyService.getNewCards(deckId, limit)
                .stream()
                .map(StudyCardResponse::from)
                .toList();
        return ResponseEntity.ok(newCards);
    }

    @PostMapping("/cards/{cardId}/rate")
    public ResponseEntity<ReviewResult> rateCard(
            @PathVariable Long cardId,
            @Valid @RequestBody RateCardRequest request) {
        ReviewResult result = studyService.rateCard(cardId, request.rating());
        return ResponseEntity.ok(result);
    }
}
