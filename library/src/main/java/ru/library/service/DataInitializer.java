package ru.library.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.library.model.*;
import ru.library.repository.LoanRepository;
import ru.library.repository.PublicationRepository;
import ru.library.repository.UserRepository;

import java.time.LocalDate;

/** Заполняет пустую базу тестовыми аккаунтами, изданиями и выдачами (только при первом запуске). */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository users;
    private final PublicationRepository pubs;
    private final LoanRepository loans;
    private final LibraryService service;

    public DataInitializer(UserRepository users, PublicationRepository pubs, LoanRepository loans,
                           LibraryService service) {
        this.users = users;
        this.pubs = pubs;
        this.loans = loans;
        this.service = service;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        service.createUser("Елена Соколова", "admin@library.local", "admin123", "+7 900 000-00-01", Role.LIBRARIAN);
        AppUser anna = service.createUser("Анна Смирнова", "reader@library.local", "reader123", "+7 900 111-22-33", Role.READER);
        AppUser ivan = service.createUser("Иван Петров", "ivan@library.local", "reader123", "+7 900 444-55-66", Role.READER);

        Publication master = pub(PublicationType.BOOK, "Мастер и Маргарита", "Михаил Булгаков", 1967, "Роман", 3,
                "Москва тридцатых годов, появление загадочного иностранца и роман внутри романа.");
        Publication crime = pub(PublicationType.BOOK, "Преступление и наказание", "Фёдор Достоевский", 1866, "Роман", 2, null);
        Publication clean = pub(PublicationType.BOOK, "Чистый код", "Роберт Мартин", 2008, "Программирование", 2,
                "Практики написания понятного и поддерживаемого кода.");
        Publication java = pub(PublicationType.BOOK, "Java. Полное руководство", "Герберт Шилдт", 2022, "Программирование", 1, null);
        pub(PublicationType.BOOK, "Алгоритмы: построение и анализ", "Томас Кормен", 2013, "Информатика", 2, null);
        Publication kvant = pub(PublicationType.MAGAZINE, "Квант", "Редакция журнала", 2024, "Наука", 4, null);
        pub(PublicationType.MAGAZINE, "Наука и жизнь", "Редакция журнала", 2025, "Наука", 3, null);
        pub(PublicationType.MAGAZINE, "Хакер", "Редакция журнала", 2024, "Технологии", 2, null);

        // Анна: одна книга на руках, один журнал возвращён вовремя
        lend(anna, master, 3, null, false);
        lend(anna, kvant, 30, 20, false);
        // Иван: просрочка (штраф растёт), старый возврат с оплаченным штрафом и с неоплаченным
        lend(ivan, clean, 25, null, false);
        lend(ivan, java, 60, 40, true);
        lend(ivan, crime, 45, 30, false);
    }

    private Publication pub(PublicationType type, String title, String author, int year, String genre,
                            int copies, String annotation) {
        Publication p = new Publication();
        p.setType(type);
        p.setTitle(title);
        p.setAuthor(author);
        p.setPublishYear(year);
        p.setGenre(genre);
        p.setAnnotation(annotation);
        p.setTotalCopies(copies);
        p.setAvailableCopies(copies);
        return pubs.save(p);
    }

    /** daysAgoIssued — сколько дней назад выдано; daysAgoReturned == null — ещё на руках. */
    private void lend(AppUser user, Publication pub, int daysAgoIssued, Integer daysAgoReturned, boolean finePaid) {
        Loan l = new Loan();
        l.setUser(user);
        l.setPublication(pub);
        l.setIssuedAt(LocalDate.now().minusDays(daysAgoIssued));
        l.setDueDate(l.getIssuedAt().plusDays(service.getLoanDays()));
        l.setFinePerDay(service.getFinePerDay());
        l.setFinePaid(finePaid);
        if (daysAgoReturned == null) {
            pub.setAvailableCopies(pub.getAvailableCopies() - 1);
            pubs.save(pub);
        } else {
            l.setReturnedAt(LocalDate.now().minusDays(daysAgoReturned));
        }
        loans.save(l);
    }
}
