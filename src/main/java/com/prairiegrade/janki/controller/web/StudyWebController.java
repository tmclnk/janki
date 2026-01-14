package com.prairiegrade.janki.controller.web;

import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.service.DeckService;
import com.prairiegrade.janki.service.SM2Service;
import com.prairiegrade.janki.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/study")
@RequiredArgsConstructor
public class StudyWebController {

    private final StudyService studyService;
    private final DeckService deckService;

    /**
     * Start a study session for a deck.
     * Fetches due cards (limit 20) and new cards (limit 10) for the user to study.
     *
     * @param deckId the ID of the deck to study
     * @param model the model to add attributes for the view
     * @return the study session view template
     */
    @GetMapping("/decks/{deckId}")
    public String startStudySession(@PathVariable Long deckId, Model model) {
        Deck deck = deckService.getDeck(deckId);
        List<StudyService.StudyCard> dueCards = studyService.getDueCards(deckId, 20);
        List<StudyService.StudyCard> newCards = studyService.getNewCards(deckId, 10);

        model.addAttribute("deck", deck);
        model.addAttribute("dueCards", dueCards);
        model.addAttribute("newCards", newCards);

        return "study/session";
    }

    /**
     * Rate a card based on user's self-assessment.
     * Uses the SM2 algorithm to update the card's review schedule.
     *
     * @param cardId the ID of the card being rated
     * @param rating the user's rating (AGAIN, HARD, GOOD, EASY)
     * @param deckId the ID of the deck to return to
     * @param redirectAttributes attributes to pass feedback to the redirected page
     * @return redirect to the study session for the deck
     */
    @PostMapping("/cards/{cardId}/rate")
    public String rateCard(
            @PathVariable Long cardId,
            @RequestParam SM2Service.UserRating rating,
            @RequestParam Long deckId,
            RedirectAttributes redirectAttributes) {

        var result = studyService.rateCard(cardId, rating);

        redirectAttributes.addFlashAttribute("ratingResult", result);
        redirectAttributes.addFlashAttribute("message",
            String.format("Card rated as %s. Next review in %d days.",
                rating.name(), result.intervalDays()));

        return "redirect:/study/decks/" + deckId;
    }
}
