package com.prairiegrade.janki.controller.web;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.service.CardService;
import com.prairiegrade.janki.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/decks/{deckId}/cards")
@RequiredArgsConstructor
public class CardWebController {

    private final CardService cardService;
    private final DeckService deckService;

    @GetMapping("/new")
    public String showCreateForm(@PathVariable Long deckId, Model model) {
        model.addAttribute("pageTitle", "Add Card");
        model.addAttribute("deck", deckService.getDeck(deckId));
        model.addAttribute("card", new Card());
        return "cards/form";
    }

    @PostMapping
    public String createCard(
            @PathVariable Long deckId,
            @RequestParam String front,
            @RequestParam String back,
            RedirectAttributes redirectAttributes) {

        cardService.createCard(deckId, front, back);
        redirectAttributes.addFlashAttribute("message", "Card added successfully!");
        return "redirect:/decks/" + deckId;
    }

    @GetMapping("/{cardId}/edit")
    public String showEditForm(
            @PathVariable Long deckId,
            @PathVariable Long cardId,
            Model model) {

        Card card = cardService.getCard(cardId);
        if (!card.getDeckId().equals(deckId)) {
            throw new IllegalArgumentException("Card does not belong to this deck");
        }

        model.addAttribute("pageTitle", "Edit Card");
        model.addAttribute("deck", deckService.getDeck(deckId));
        model.addAttribute("card", card);
        return "cards/form";
    }

    @PostMapping("/{cardId}")
    public String updateCard(
            @PathVariable Long deckId,
            @PathVariable Long cardId,
            @RequestParam String front,
            @RequestParam String back,
            RedirectAttributes redirectAttributes) {

        cardService.updateCard(cardId, front, back);
        redirectAttributes.addFlashAttribute("message", "Card updated successfully!");
        return "redirect:/decks/" + deckId;
    }

    @PostMapping("/{cardId}/delete")
    public String deleteCard(
            @PathVariable Long deckId,
            @PathVariable Long cardId,
            RedirectAttributes redirectAttributes) {

        cardService.deleteCard(cardId);
        redirectAttributes.addFlashAttribute("message", "Card deleted successfully!");
        return "redirect:/decks/" + deckId;
    }
}
