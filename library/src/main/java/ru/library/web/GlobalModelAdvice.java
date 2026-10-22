package ru.library.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.library.model.AppUser;
import ru.library.model.Role;
import ru.library.service.LibraryService;
import ru.library.service.NotificationService;

import java.security.Principal;

@ControllerAdvice
public class GlobalModelAdvice {

    private final LibraryService service;
    private final NotificationService notifications;

    public GlobalModelAdvice(LibraryService service, NotificationService notifications) {
        this.service = service;
        this.notifications = notifications;
    }

    @ModelAttribute
    public void addCommon(Principal principal, Model model) {
        if (principal == null) {
            return;
        }
        AppUser me = service.user(principal.getName());
        model.addAttribute("me", me);
        if (me.getRole() == Role.LIBRARIAN) {
            notifications.syncLibrarian(me.getEmail());
        } else {
            notifications.sync(me.getEmail());
        }
        model.addAttribute("bell", notifications.unreadCount(me.getEmail()));
    }
}