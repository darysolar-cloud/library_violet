package ru.library.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.model.PublicationType;
import ru.library.service.LibraryService;
import ru.library.service.ContentService;
import java.security.Principal;
import ru.library.model.Publication;

import java.net.URI;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/catalog")
public class CatalogController {

    private final LibraryService service;
    private final ContentService content;

    public CatalogController(LibraryService service, ContentService content) {
        this.service = service;
        this.content = content;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) PublicationType type,
                       @RequestParam(required = false) String genre,
                       @RequestParam(defaultValue = "title") String sort,
                       @RequestParam(defaultValue = "false") boolean available,
                       Principal principal, Model model) {
        model.addAttribute("publications", service.search(q, type, genre, sort, available));
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        model.addAttribute("genre", genre);
        model.addAttribute("sort", sort);
        model.addAttribute("available", available);
        model.addAttribute("types", PublicationType.values());
        model.addAttribute("genres", content.genreNames());
        model.addAttribute("quote", content.quoteOfDay());
        model.addAttribute("readIds", service.readList(principal.getName()).stream()
                .map(Publication::getId).collect(Collectors.toSet()));
        return "catalog";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("p", service.publication(id));
        model.addAttribute("loanDays", service.getLoanDays());
        model.addAttribute("inReadList", service.inReadList(principal.getName(), id));
        return "publication";
    }

    @PostMapping("/{id}/readlist")
    public String toggleReadList(@PathVariable Long id, Principal principal,
                                 @RequestHeader(value = "Referer", required = false) String referer,
                                 RedirectAttributes ra) {
        String back = backTo(referer, id);
        return Flash.run(ra, "Избранное обновлено.", back, back,
                () -> service.toggleReadList(principal.getName(), id));
    }

    /** Возвращает на страницу каталога, откуда пришёл запрос; иначе на карточку издания. */
    private String backTo(String referer, Long id) {
        try {
            URI u = URI.create(referer);
            if (u.getPath() != null && u.getPath().startsWith("/catalog")) {
                return u.getRawQuery() == null ? u.getPath() : u.getPath() + "?" + u.getRawQuery();
            }
        } catch (Exception ignored) {
            // нет заголовка Referer или он некорректный
        }
        return "/catalog/" + id;
    }
    @PostMapping("/{id}/borrow")
    public String borrow(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Готово — издание оформлено. Срок возврата указан в списке выдач.",
                "/reader", "/catalog/" + id, () -> service.borrow(principal.getName(), id));
    }
}
