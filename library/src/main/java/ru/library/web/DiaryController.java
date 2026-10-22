package ru.library.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.model.DiaryEntry;
import ru.library.model.Publication;
import ru.library.service.*;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/reader/diary")
public class DiaryController {

    private final DiaryService diary;
    private final LibraryService service;
    private final ContentService content;

    public DiaryController(DiaryService diary, LibraryService service, ContentService content) {
        this.diary = diary;
        this.service = service;
        this.content = content;
    }

    // ---------- список записей
    @GetMapping
    public String list(@RequestParam(defaultValue = "mine") String tab,
                       @RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) String genre,
                       Principal principal, Model model) {
        List<DiaryEntry> all = diary.entries(principal.getName());
        String s = q.trim().toLowerCase();
        List<DiaryEntry> shown = all.stream()
                .filter(e -> switch (tab) {
                    case "fav" -> e.isFavorite() && !e.isDraft();
                    case "draft" -> e.isDraft();
                    default -> !e.isDraft();
                })
                .filter(e -> s.isEmpty()
                        || e.getPublication().getTitle().toLowerCase().contains(s)
                        || e.getPublication().getAuthor().toLowerCase().contains(s))
                .filter(e -> genre == null || genre.isBlank() || genre.equals(e.getPublication().getGenre()))
                .toList();
        model.addAttribute("entries", shown);
        model.addAttribute("tab", tab);
        model.addAttribute("q", q);
        model.addAttribute("genre", genre);
        model.addAttribute("genres", content.genreNames());
        model.addAttribute("countMine", all.stream().filter(e -> !e.isDraft()).count());
        model.addAttribute("countFav", all.stream().filter(e -> e.isFavorite() && !e.isDraft()).count());
        model.addAttribute("countDraft", all.stream().filter(DiaryEntry::isDraft).count());
        return "reader/diary";
    }

    // ---------- новая запись
    @GetMapping("/new")
    public String create(@RequestParam(required = false) Long publicationId, Principal principal, Model model) {
        if (publicationId != null) {
            Optional<DiaryEntry> existing = diary.entryFor(principal.getName(), publicationId);
            if (existing.isPresent()) {
                return "redirect:/reader/diary/" + existing.get().getId();
            }
        }
        Publication book = null;
        if (publicationId != null) {
            try {
                book = service.publication(publicationId);
            } catch (LibraryException ignored) {
                // неверный id: просто покажем пустой выбор
            }
        }
        DiaryForm form = new DiaryForm();
        form.setPublicationId(publicationId);
        model.addAttribute("form", form);
        model.addAttribute("book", book);
        model.addAttribute("candidates", diary.candidates(principal.getName()));
        model.addAttribute("moods", DiaryService.MOODS);
        return "reader/diary-form";
    }

    @PostMapping
    public String createEntry(@ModelAttribute DiaryForm form,
                              @RequestParam(defaultValue = "save") String action,
                              Principal principal, RedirectAttributes ra) {
        try {
            DiaryEntry e = diary.save(principal.getName(), null, form, "draft".equals(action));
            ra.addFlashAttribute("ok", "Запись сохранена.");
            return "redirect:/reader/diary/" + e.getId();
        } catch (LibraryException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reader/diary/new"
                    + (form.getPublicationId() == null ? "" : "?publicationId=" + form.getPublicationId());
        }
    }

    // ---------- просмотр и редактирование
    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            model.addAttribute("e", diary.entry(principal.getName(), id));
            return "reader/diary-entry";
        } catch (LibraryException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reader/diary";
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Principal principal, Model model, RedirectAttributes ra) {
        try {
            DiaryEntry e = diary.entry(principal.getName(), id);
            model.addAttribute("form", DiaryForm.of(e));
            model.addAttribute("entry", e);
            model.addAttribute("book", e.getPublication());
            model.addAttribute("candidates", List.of());
            model.addAttribute("moods", DiaryService.MOODS);
            return "reader/diary-form";
        } catch (LibraryException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reader/diary";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute DiaryForm form,
                         @RequestParam(defaultValue = "save") String action,
                         Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Запись сохранена.", "/reader/diary/" + id, "/reader/diary/" + id + "/edit",
                () -> diary.save(principal.getName(), id, form, "draft".equals(action)));
    }

    @PostMapping("/{id}/note")
    public String saveNote(@PathVariable Long id, @RequestParam(required = false) String note,
                           Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Заметка сохранена.", "/reader/diary/" + id, "/reader/diary/" + id,
                () -> diary.saveNote(principal.getName(), id, note));
    }

    @PostMapping("/{id}/favorite")
    public String favorite(@PathVariable Long id, Principal principal,
                           @RequestHeader(value = "Referer", required = false) String referer,
                           RedirectAttributes ra) {
        String back = backTo(referer, "/reader/diary/" + id);
        return Flash.run(ra, "Избранное обновлено.", back, back,
                () -> diary.toggleFavorite(principal.getName(), id));
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Запись удалена.", "/reader/diary", "/reader/diary/" + id,
                () -> diary.delete(principal.getName(), id));
    }

    /** Возвращает на страницу дневника, откуда пришёл запрос (с фильтрами), иначе на запасной адрес. */
    private String backTo(String referer, String fallback) {
        try {
            URI u = URI.create(referer);
            if (u.getPath() != null && u.getPath().startsWith("/reader/diary")) {
                return u.getRawQuery() == null ? u.getPath() : u.getPath() + "?" + u.getRawQuery();
            }
        } catch (Exception ignored) {
            // нет Referer или он некорректный
        }
        return fallback;
    }
}