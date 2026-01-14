package com.prairiegrade.janki.controller.web;

import com.prairiegrade.janki.domain.Card;
import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/decks")
@RequiredArgsConstructor
public class DeckWebController {

    private final DeckService deckService;

    @GetMapping
    public String listDecks(Model model) {
        model.addAttribute("pageTitle", "My Decks");
        model.addAttribute("decks", deckService.getAllDecks());
        return "decks/list";
    }

    @GetMapping("/{id}")
    public String viewDeck(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ALL") String filter,
            Model model) {
        Deck deck = deckService.getDeck(id);
        model.addAttribute("pageTitle", deck.getName());
        model.addAttribute("deck", deck);
        model.addAttribute("stats", deckService.getDeckStats(id));

        // Add filter to model for template to know active filter
        model.addAttribute("filter", filter);

        // Get filtered cards
        List<Card> cards = deckService.getFilteredCards(id, filter);
        model.addAttribute("cards", cards);

        return "decks/view";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("deck", new Deck());
        return "decks/form";
    }

    @PostMapping
    public String createDeck(
            @Valid @ModelAttribute Deck deck,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "decks/form";
        }

        Deck createdDeck = deckService.createDeck(deck.getName(), deck.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Deck created successfully!");
        return "redirect:/decks/" + createdDeck.getId();
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("deck", deckService.getDeck(id));
        return "decks/form";
    }

    @PostMapping("/{id}")
    public String updateDeck(
            @PathVariable Long id,
            @Valid @ModelAttribute Deck deck,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "decks/form";
        }

        deckService.updateDeck(id, deck.getName(), deck.getDescription());
        redirectAttributes.addFlashAttribute("successMessage", "Deck updated successfully!");
        return "redirect:/decks/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteDeck(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        deckService.deleteDeck(id);
        redirectAttributes.addFlashAttribute("successMessage", "Deck deleted successfully!");
        return "redirect:/decks";
    }
}
