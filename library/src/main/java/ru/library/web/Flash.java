package ru.library.web;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.library.service.LibraryException;

/** Выполняет действие и кладёт результат во «флеш»-сообщение (зелёное или красное). */
public final class Flash {

    private Flash() {}

    public static String run(RedirectAttributes ra, String okMessage, String okUrl, String errorUrl, Runnable action) {
        try {
            action.run();
            ra.addFlashAttribute("ok", okMessage);
            return "redirect:" + okUrl;
        } catch (LibraryException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:" + errorUrl;
        }
    }
}
