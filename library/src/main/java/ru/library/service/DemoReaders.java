package ru.library.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.library.model.AppUser;
import ru.library.model.Loan;
import ru.library.model.Publication;
import ru.library.model.Role;
import ru.library.repository.LoanRepository;
import ru.library.repository.PublicationRepository;
import ru.library.repository.UserRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Создаёт 30 тестовых читателей с разными выдачами.
 * Работает только при library.demo-readers=true; уже существующие почты пропускает.
 */
@Component
public class DemoReaders {

    private static final String PASSWORD = "reader123";

    /** Одна выдача: сколько дней назад выдана, сколько дней назад возвращена (null — ещё на руках), оплачен ли штраф. */
    private record L(int issuedAgo, Integer returnedAgo, boolean paid) {}

    private record R(String name, String email, boolean blocked, List<L> loans) {}

    private static L active(int issuedAgo) {
        return new L(issuedAgo, null, false);
    }

    private static L done(int issuedAgo, int returnedAgo, boolean paid) {
        return new L(issuedAgo, returnedAgo, paid);
    }

    private static R r(String name, String email, L... loans) {
        return new R(name, email + "@library.local", false, List.of(loans));
    }

    private static R blocked(String name, String email, L... loans) {
        return new R(name, email + "@library.local", true, List.of(loans));
    }

    private final LibraryService service;
    private final UserRepository users;
    private final PublicationRepository pubs;
    private final LoanRepository loans;
    private final boolean enabled;

    public DemoReaders(LibraryService service, UserRepository users, PublicationRepository pubs,
                       LoanRepository loans, @Value("${library.demo-readers:false}") boolean enabled) {
        this.service = service;
        this.users = users;
        this.pubs = pubs;
        this.loans = loans;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(20)
    @Transactional
    public void seed() {
        if (!enabled) {
            return;
        }
        List<Publication> all = pubs.findAll(Sort.by("id"));
        if (all.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        int[] cursor = {3};
        int index = 0;

        for (R r : READERS) {
            index++;
            if (users.existsByEmailIgnoreCase(r.email())) {
                continue;
            }
            String phone = String.format("+7 9%02d %03d-%02d-%02d", 10 + index, 100 + index * 7, index, 90 - index);
            AppUser u = service.createUser(r.name(), r.email(), PASSWORD, phone, Role.READER);
            u.setRegisteredAt(today.minusDays(100 + index * 6L));
            u.setBlocked(r.blocked());

            Set<Long> used = new HashSet<>();
            for (L spec : r.loans()) {
                boolean isActive = spec.returnedAgo() == null;
                Publication p = pick(all, cursor, used, isActive);
                if (p == null) {
                    continue;
                }
                Loan loan = new Loan();
                loan.setUser(u);
                loan.setPublication(p);
                loan.setIssuedAt(today.minusDays(spec.issuedAgo()));
                loan.setDueDate(loan.getIssuedAt().plusDays(service.getLoanDays()));
                loan.setFinePerDay(service.getFinePerDay());
                if (isActive) {
                    p.setAvailableCopies(p.getAvailableCopies() - 1);
                } else {
                    loan.setReturnedAt(today.minusDays(spec.returnedAgo()));
                    loan.setFinePaid(spec.paid());
                }
                loans.save(loan);
            }
        }
    }

    /** Берёт следующее издание по кругу; для выдач «на руках» нужен свободный экземпляр. */
    private static Publication pick(List<Publication> all, int[] cursor, Set<Long> used, boolean needCopy) {
        for (int tries = 0; tries < all.size(); tries++) {
            Publication p = all.get(cursor[0] % all.size());
            cursor[0]++;
            if (used.contains(p.getId())) {
                continue;
            }
            if (needCopy && p.getAvailableCopies() <= 0) {
                continue;
            }
            used.add(p.getId());
            return p;
        }
        return null;
    }

    private static final List<R> READERS = List.of(
            // обычные читатели: что-то на руках и история
            r("Алексей Кузнецов", "alexey.kuznetsov", active(3), done(40, 28, false), done(75, 63, false)),
            r("Мария Попова", "maria.popova", active(12), done(30, 20, false)),                // скоро вернуть (2 дня)
            // просрочено
            r("Дмитрий Соколов", "dmitry.sokolov", active(24), done(60, 50, false)),           // просрочка 10 дн.
            r("Екатерина Лебедева", "ekaterina.lebedeva", done(20, 10, false), done(45, 35, false), done(70, 60, false)),
            r("Никита Козлов", "nikita.kozlov", active(1), active(6), active(9)),
            r("Ольга Новикова", "olga.novikova"),                                              // без выдач
            r("Сергей Морозов", "sergey.morozov", active(5), done(50, 30, true)),              // вернул с опозданием, штраф оплачен
            r("Дарья Петрова", "darya.petrova", done(40, 25, false)),                          // неоплаченный штраф
            r("Артём Волков", "artem.volkov", active(30), active(28)),                         // две просрочки
            r("Юлия Соловьёва", "yulia.solovieva", active(13), active(2), done(35, 22, false)),
            r("Максим Васильев", "maxim.vasiliev", done(15, 8, false), active(11)),            // скоро вернуть (3 дня)
            r("Анастасия Зайцева", "anastasia.zaytseva", active(14), done(60, 48, false)),     // срок сегодня
            r("Андрей Павлов", "andrey.pavlov"),                                               // без выдач
            r("Полина Семёнова", "polina.semenova", done(25, 12, false)),
            r("Илья Голубев", "ilya.golubev", active(4), active(7), active(10), active(12), active(13)), // 5 книг — лимит
            r("Виктория Виноградова", "victoria.vinogradova", done(90, 80, false), done(60, 52, false),
                    done(30, 21, false), done(12, 5, false)),                                  // активный читатель
            r("Кирилл Богданов", "kirill.bogdanov", active(20)),                               // просрочка 6 дн.
            r("Алина Воробьёва", "alina.vorobyova", done(55, 35, false)),                      // неоплаченный штраф
            blocked("Тимофей Фёдоров", "timofey.fedorov", done(70, 40, false)),                // заблокирован
            r("Елизавета Михайлова", "elizaveta.mikhailova", active(8), done(33, 24, false)),
            r("Роман Беляев", "roman.belyaev", done(18, 9, false), done(44, 31, false)),
            r("София Тарасова", "sofia.tarasova"),                                             // без выдач
            r("Егор Белов", "egor.belov", active(16)),                                         // просрочка 2 дн.
            r("Ксения Комарова", "ksenia.komarova", done(20, 14, false), active(10)),
            r("Владислав Орлов", "vladislav.orlov", done(80, 55, true), done(40, 30, false)),  // штраф оплачен
            r("Варвара Киселёва", "varvara.kiseleva", active(0), done(25, 15, false)),         // взяла сегодня
            r("Павел Макаров", "pavel.makarov"),                                               // без выдач
            r("Милана Андреева", "milana.andreeva", active(6), done(35, 20, true)),
            r("Глеб Ковалёв", "gleb.kovalev", active(22), done(50, 38, false)),                // просрочка 8 дн.
            r("Арина Ильина", "arina.ilyina", done(10, 2, false), done(33, 25, false), active(1))
    );
}