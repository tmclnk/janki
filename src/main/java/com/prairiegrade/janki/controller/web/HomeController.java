package com.prairiegrade.janki.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home controller for root URL.
 * Redirects to the main decks list page.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/decks";
    }
}
