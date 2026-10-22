package ru.library.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.service.ContentService;

@Controller
@RequestMapping("/admin")
public class AdminContentController {

    private final ContentService content;

    public AdminContentController(ContentService content) {
        this.content = content;
    }

    // ---------- жанры
    @GetMapping("/genres")
    public String genres(Model model) {
        model.addAttribute("genres", content.genres());
        model.addAttribute("counts", content.genreCounts());
        return "admin/genres";
    }

    @PostMapping("/genres")
    public String addGenre(@RequestParam String name, RedirectAttributes ra) {
        return Flash.run(ra, "Жанр добавлен.", "/admin/genres", "/admin/genres", () -> content.addGenre(name));
    }

    @PostMapping("/genres/{id}/rename")
    public String renameGenre(@PathVariable Long id, @RequestParam String name, RedirectAttributes ra) {
        return Flash.run(ra, "Жанр переименован.", "/admin/genres", "/admin/genres", () -> content.renameGenre(id, name));
    }

    @PostMapping("/genres/{id}/delete")
    public String deleteGenre(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Жанр удалён.", "/admin/genres", "/admin/genres", () -> content.deleteGenre(id));
    }

    // ---------- цитаты
    @GetMapping("/quotes")
    public String quotes(Model model) {
        model.addAttribute("quotes", content.allQuotes());
        return "admin/quotes";
    }

    @PostMapping("/quotes")
    public String addQuote(@RequestParam String body, @RequestParam String author, RedirectAttributes ra) {
        return Flash.run(ra, "Цитата добавлена.", "/admin/quotes", "/admin/quotes", () -> content.addQuote(body, author));
    }

    @PostMapping("/quotes/{id}/delete")
    public String deleteQuote(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Цитата удалена.", "/admin/quotes", "/admin/quotes", () -> content.deleteQuote(id));
    }

    // ---------- книга дня
    @PostMapping("/book-of-day/{id}")
    public String setBookOfDay(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Эта книга показывается на главной сегодня.", "/admin/publications", "/admin/publications",
                () -> content.setBookOfDay(id));
    }

    @PostMapping("/book-of-day/reset")
    public String resetBookOfDay(RedirectAttributes ra) {
        return Flash.run(ra, "Выбор сброшен: на главной будет случайная доступная книга.", "/admin/publications",
                "/admin/publications", () -> content.resetBookOfDay());
    }
}