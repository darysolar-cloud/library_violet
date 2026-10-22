package ru.library.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.service.LibraryService;
import ru.library.model.Notification;
import ru.library.service.NotificationService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import java.security.Principal;

@Controller
public class AccountController {

    private final LibraryService service;
    private final NotificationService notifications;

    public AccountController(LibraryService service, NotificationService notifications) {
        this.service = service;
        this.notifications = notifications;
    }

    @GetMapping("/admin/notifications")
    public String adminNotifications(Principal principal, Model model) {
        String email = principal.getName();
        List<Notification> items = notifications.list(email);
        Set<Long> unreadIds = items.stream().filter(n -> !n.isReadFlag())
                .map(Notification::getId).collect(Collectors.toSet());
        model.addAttribute("items", items);
        model.addAttribute("unreadIds", unreadIds);
        notifications.markAllRead(email);
        model.addAttribute("bell", 0L);
        return "admin/notifications";
    }

    @GetMapping("/admin/profile")
    public String adminProfile(Principal principal, Model model) {
        model.addAttribute("u", service.user(principal.getName()));
        return "admin/profile";
    }

    @PostMapping("/account/avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile avatar, Principal principal,
                               Authentication auth, RedirectAttributes ra) {
        String back = backTo(auth);
        return Flash.run(ra, "Аватарка обновлена.", back, back,
                () -> service.setAvatar(principal.getName(), avatar));
    }

    @PostMapping("/account/avatar/remove")
    public String removeAvatar(Principal principal, Authentication auth, RedirectAttributes ra) {
        String back = backTo(auth);
        return Flash.run(ra, "Фото удалено.", back, back, () -> service.removeAvatar(principal.getName()));
    }

    private String backTo(Authentication auth) {
        boolean librarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        return librarian ? "/admin/profile" : "/reader/profile";
    }
}