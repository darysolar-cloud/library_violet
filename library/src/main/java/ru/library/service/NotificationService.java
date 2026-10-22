package ru.library.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.model.AppUser;
import ru.library.model.Loan;
import ru.library.model.Notification;
import ru.library.repository.LoanRepository;
import ru.library.repository.NotificationRepository;
import ru.library.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final NotificationRepository notifications;
    private final UserRepository users;
    private final LoanRepository loans;

    public NotificationService(NotificationRepository notifications, UserRepository users, LoanRepository loans) {
        this.notifications = notifications;
        this.users = users;
        this.loans = loans;
    }

    // ------------------------------------------------------------ автоматические напоминания

    /** Читатель: приветствие при первом входе, скоро вернуть, просрочка. */
    public void sync(String email) {
        AppUser u = user(email);
        if (notifications.countByUser(u) == 0) {
            push(u, "INFO", "Добро пожаловать в библиотеку!",
                    "Рады, что вы с нами. Выберите книгу в каталоге и оформите выдачу.", null);
        }
        for (Loan l : loans.findByUserOrderByIssuedAtDescIdDesc(u)) {
            if (l.isReturned()) {
                continue;
            }
            if (l.isOverdue()) {
                addOnce(u, l, "OVERDUE", "Издание просрочено",
                        quoted(l) + " — просрочка " + l.getOverdueDays() + " дн. Верните книгу, чтобы штраф не рос.");
            } else if (l.getDaysLeft() <= 3) {
                addOnce(u, l, "SOON", "Срок возврата приближается",
                        quoted(l) + " — вернуть до " + l.getDueDate().format(FMT) + ", осталось " + l.getDaysLeft() + " дн.");
            }
        }
    }

    /** Библиотекарь: каждая новая просроченная выдача. */
    public void syncLibrarian(String email) {
        AppUser u = user(email);
        for (Loan l : loans.findAll()) {
            if (l.isOverdue()) {
                addOnce(u, l, "OVERDUE", "Просрочена выдача",
                        l.getUser().getFullName() + " — " + quoted(l) + ", просрочка " + l.getOverdueDays()
                                + " дн., штраф " + l.getFine() + " ₽.");
            }
        }
    }

    // ------------------------------------------------------------ события

    public void onIssued(Loan l) {
        push(l.getUser(), "INFO", "Выдача оформлена",
                quoted(l) + " — вернуть до " + l.getDueDate().format(FMT) + ".", l.getId());
    }

    public void onReturned(Loan l) {
        if (l.getFine() > 0) {
            push(l.getUser(), "FINE", "Издание возвращено с опозданием",
                    quoted(l) + " — просрочка " + l.getOverdueDays() + " дн., начислен штраф " + l.getFine()
                            + " ₽. Оплатить его можно у библиотекаря.", l.getId());
        } else {
            push(l.getUser(), "INFO", "Возврат принят",
                    quoted(l) + " — спасибо, что вернули вовремя!", l.getId());
        }
    }

    public void onFinePaid(Loan l) {
        push(l.getUser(), "INFO", "Штраф оплачен", quoted(l) + " — " + l.getFine() + " ₽. Спасибо!", l.getId());
    }

    public void onBlockChanged(AppUser u) {
        if (u.isBlocked()) {
            push(u, "FINE", "Аккаунт заблокирован", "Обратитесь к библиотекарю.", null);
        } else {
            push(u, "INFO", "Аккаунт разблокирован", "Вы снова можете оформлять выдачи.", null);
        }
    }

    // ------------------------------------------------------------ чтение

    @Transactional(readOnly = true)
    public List<Notification> list(String email) {
        return notifications.findByUserOrderByCreatedAtDescIdDesc(user(email));
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return notifications.countByUserAndReadFlagFalse(user(email));
    }

    public void markAllRead(String email) {
        notifications.findByUserOrderByCreatedAtDescIdDesc(user(email)).stream()
                .filter(n -> !n.isReadFlag())
                .forEach(n -> n.setReadFlag(true));
    }

    // ------------------------------------------------------------ внутреннее

    private void addOnce(AppUser u, Loan l, String kind, String title, String text) {
        if (!notifications.existsByUserAndLoanIdAndKind(u, l.getId(), kind)) {
            push(u, kind, title, text, l.getId());
        }
    }

    private void push(AppUser u, String kind, String title, String text, Long loanId) {
        Notification n = new Notification();
        n.setUser(u);
        n.setKind(kind);
        n.setTitle(title);
        n.setText(text);
        n.setLoanId(loanId);
        notifications.save(n);
    }

    private static String quoted(Loan l) {
        return "«" + l.getPublication().getTitle() + "»";
    }

    private AppUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new LibraryException("Пользователь не найден."));
    }
}