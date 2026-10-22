package ru.library.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.model.Quote;
import ru.library.service.ContentService;

import java.security.Principal;

@Controller
@RequestMapping("/home")
public class HomeController {

    private final ContentService content;

    public HomeController(ContentService content) {
        this.content = content;
    }

    @GetMapping
    public String home(Principal principal, Model model) {
        Quote quote = content.quoteOfDay();
        model.addAttribute("quote", quote);
        model.addAttribute("quoteFav", quote != null && content.isFavorite(principal.getName(), quote.getId()));
        model.addAttribute("book", content.bookOfDay());
        model.addAttribute("favorites", content.favoriteQuotes(principal.getName()));
        return "reader/home";
    }

    @PostMapping("/quote/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Избранное обновлено.", "/home", "/home",
                () -> content.toggleFavorite(principal.getName(), id));
    }
}