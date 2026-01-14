package com.prairiegrade.janki.controller.web;

import com.prairiegrade.janki.domain.Deck;
import com.prairiegrade.janki.service.DeckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/decks")
@RequiredArgsConstructor
public class DeckWebController {

    private final DeckService deckService;

    @GetMapping
    public String listDecks(Model model) {
        model.addAttribute("decks", deckService.getAllDecks());
        return "decks/list";
    }

    @GetMapping("/{id}")
    public String viewDeck(@PathVariable Long id, Model model) {
        model.addAttribute("deck", deckService.getDeck(id));
        model.addAttribute("stats", deckService.getDeckStats(id));
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
