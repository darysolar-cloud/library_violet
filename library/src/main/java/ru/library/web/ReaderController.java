package ru.library.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.model.Loan;
import ru.library.service.LibraryService;
import ru.library.model.AppUser;
import ru.library.service.DiaryService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import ru.library.model.Notification;
import ru.library.service.NotificationService;

import java.util.Set;
import java.util.stream.Collectors;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/reader")
public class ReaderController {

    private final LibraryService service;
    public record Notice(String kind, String title, String text, LocalDate date) {}

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DiaryService diary;
    private final NotificationService notifications;

    public ReaderController(LibraryService service, DiaryService diary, NotificationService notifications) {
        this.service = service;
        this.diary = diary;
        this.notifications = notifications;
    }

    @GetMapping
    public String loans(@RequestParam(defaultValue = "current") String tab, Principal principal, Model model) {
        String email = principal.getName();
        List<Loan> loans = service.myLoans(email);
        List<Loan> current = loans.stream().filter(l -> !l.isReturned()).toList();
        List<Loan> history = loans.stream().filter(Loan::isReturned).toList();
        List<Notification> reminders = notifications.list(email);
        Set<Long> unreadIds = reminders.stream().filter(n -> !n.isReadFlag())
                .map(Notification::getId).collect(Collectors.toSet());

        model.addAttribute("tab", tab);
        model.addAttribute("rows", "history".equals(tab) ? history : current);
        model.addAttribute("countCurrent", current.size());
        model.addAttribute("countHistory", history.size());
        model.addAttribute("overdueLoans", current.stream().filter(Loan::isOverdue).toList());
        model.addAttribute("fineDue", loans.stream().filter(Loan::isFineUnpaid).mapToLong(Loan::getFine).sum());
        model.addAttribute("reminders", reminders);
        model.addAttribute("unreadIds", unreadIds);
        model.addAttribute("unreadCount", "reminders".equals(tab) ? 0 : unreadIds.size());
        model.addAttribute("diary", diary.entryIdsByPublication(email));
        if ("reminders".equals(tab)) {
            notifications.markAllRead(email);
            model.addAttribute("bell", 0L);
        }
        return "reader/loans";
    }

    @PostMapping("/readlist/{id}/remove")
    public String removeFromReadList(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Убрано из ReadList.", "/home", "/home",
                () -> service.toggleReadList(principal.getName(), id));
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        String email = principal.getName();
        AppUser u = service.user(email);
        List<Loan> loans = service.myLoans(email);
        model.addAttribute("u", u);
        model.addAttribute("active", loans.stream().filter(l -> !l.isReturned()).toList());
        model.addAttribute("history", loans.stream().filter(Loan::isReturned).limit(5).toList());
        model.addAttribute("notices", notifications.list(email).stream().limit(4).toList());
        model.addAttribute("overdue", loans.stream().filter(Loan::isOverdue).count());
        model.addAttribute("fineDue", loans.stream().filter(Loan::isFineUnpaid).mapToLong(Loan::getFine).sum());
        return "reader/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@RequestParam String fullName, @RequestParam(required = false) String phone,
                              Principal principal, RedirectAttributes ra) {
        return Flash.run(ra, "Профиль сохранён.", "/reader/profile", "/reader/profile",
                () -> service.updateProfile(principal.getName(), fullName, phone));
    }
}
