package ru.library.web;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ru.library.service.LibraryException;
import ru.library.service.LibraryService;

@Controller
public class AuthController {

    private final LibraryService service;

    public AuthController(LibraryService service) {
        this.service = service;
    }

    /** После входа каждый попадает в свой раздел. */
    @GetMapping("/")
    public String home(Authentication auth) {
        boolean librarian = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"));
        return "redirect:" + (librarian ? "/admin" : "/home");
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            service.registerReader(form.getFullName(), form.getEmail(), form.getPassword(), form.getPhone());
        } catch (LibraryException e) {
            result.rejectValue("email", "exists", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";
    }
}
