package ru.library.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.model.Loan;
import ru.library.model.Publication;
import ru.library.model.PublicationType;
import ru.library.service.LibraryService;
import ru.library.service.ContentService;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final LibraryService service;
    private final ContentService content;

    public AdminController(LibraryService service, ContentService content) {
        this.service = service;
        this.content = content;
    }
    // ---------- обзор
    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", service.stats());
        model.addAttribute("overdue", service.loansByFilter("overdue"));
        model.addAttribute("popular", service.popular());
        return "admin/dashboard";
    }

    // ---------- читатели
    @GetMapping("/readers")
    public String readers(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("rows", service.readerRows(q));
        model.addAttribute("q", q);
        return "admin/readers";
    }

    @PostMapping("/readers/{id}/block")
    public String toggleBlock(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Статус читателя изменён.", "/admin/readers", "/admin/readers",
                () -> service.toggleBlock(id));
    }

    // ---------- издания
    @GetMapping("/publications")
    public String publications(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("publications", service.search(q, null));
        model.addAttribute("q", q);
        return "admin/publications";
    }

    @GetMapping("/publications/new")
    public String newPublication(Model model) {
        model.addAttribute("pub", new Publication());
        model.addAttribute("types", PublicationType.values());
        // в newPublication(...) и в editPublication(...), перед return:
        model.addAttribute("genres", content.genreNames());
        return "admin/publication-form";
    }

    @GetMapping("/publications/{id}/edit")
    public String editPublication(@PathVariable Long id, Model model) {
        model.addAttribute("pub", service.publication(id));
        // в newPublication(...) и в editPublication(...), перед return:
        model.addAttribute("genres", content.genreNames());
        model.addAttribute("types", PublicationType.values());
        return "admin/publication-form";
    }

    @PostMapping("/publications")
    public String savePublication(@ModelAttribute("pub") Publication pub,
                                  @RequestParam(value = "cover", required = false) MultipartFile cover,
                                  @RequestParam(defaultValue = "false") boolean removeCover,
                                  RedirectAttributes ra) {
        String back = pub.getId() == null ? "/admin/publications/new"
                : "/admin/publications/" + pub.getId() + "/edit";
        return Flash.run(ra, "Издание сохранено.", "/admin/publications", back,
                () -> service.savePublication(pub, cover, removeCover));
    }

    @PostMapping("/publications/{id}/delete")
    public String deletePublication(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Издание удалено.", "/admin/publications", "/admin/publications",
                () -> service.deletePublication(id));
    }

    // ---------- выдачи
    @GetMapping("/loans")
    public String loans(@RequestParam(defaultValue = "all") String filter,
                        @RequestParam(defaultValue = "1") int page, Model model) {
        List<Loan> all = service.loansByFilter("all");
        List<Loan> filtered = service.loansByFilter(filter);

        int size = 10;
        int pages = Math.max(1, (int) Math.ceil(filtered.size() / (double) size));
        int current = Math.min(Math.max(page, 1), pages);
        List<Loan> slice = filtered.subList((current - 1) * size, Math.min(current * size, filtered.size()));

        model.addAttribute("loans", slice);
        model.addAttribute("filter", filter);
        model.addAttribute("page", current);
        model.addAttribute("pages", pages);
        model.addAttribute("total", filtered.size());
        model.addAttribute("countAll", all.size());
        model.addAttribute("countActive", all.stream().filter(l -> !l.isReturned()).count());
        model.addAttribute("countOverdue", all.stream().filter(Loan::isOverdue).count());
        model.addAttribute("countReturned", all.stream().filter(Loan::isReturned).count());
        model.addAttribute("readers", service.activeReaders());
        model.addAttribute("available", service.availablePublications());
        return "admin/loans";
    }

    @PostMapping("/loans")
    public String issue(@RequestParam Long userId, @RequestParam Long publicationId, RedirectAttributes ra) {
        return Flash.run(ra, "Выдача оформлена.", "/admin/loans", "/admin/loans",
                () -> service.issueTo(userId, publicationId));
    }

    @PostMapping("/loans/{id}/return")
    public String returnLoan(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Возврат принят.", "/admin/loans", "/admin/loans",
                () -> service.returnLoan(id));
    }

    // ---------- штрафы
    @GetMapping("/fines")
    public String fines(Model model) {
        List<Loan> fined = service.finedLoans();
        model.addAttribute("loans", fined);
        model.addAttribute("total", fined.stream().filter(Loan::isFineUnpaid).mapToLong(Loan::getFine).sum());
        model.addAttribute("rate", service.getFinePerDay());
        return "admin/fines";
    }

    @PostMapping("/fines/{id}/pay")
    public String payFine(@PathVariable Long id, RedirectAttributes ra) {
        return Flash.run(ra, "Оплата отмечена.", "/admin/fines", "/admin/fines",
                () -> service.payFine(id));
    }
}
