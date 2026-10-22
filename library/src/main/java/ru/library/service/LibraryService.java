package ru.library.service;

import ru.library.repository.ReadListRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.library.model.*;
import ru.library.repository.LoanRepository;
import ru.library.repository.PublicationRepository;
import ru.library.repository.UserRepository;
import ru.library.repository.ReadListRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LibraryService {

    public record ReaderRow(AppUser user, long active, long fineDue) {}
    public record Stats(long readers, long titles, long onHand, long overdue, long fineDue) {}
    public record Popular(String title, long count) {}

    private final UserRepository users;
    private final PublicationRepository pubs;
    private final LoanRepository loans;
    private final PasswordEncoder encoder;
    private final FileStorageService files;

    @Value("${library.loan-days}")
    private int loanDays;
    @Value("${library.fine-per-day}")
    private int finePerDay;
    @Value("${library.max-active-loans}")
    private int maxActive;

    private final NotificationService notifications;
    private final ReadListRepository readListItems;

    public LibraryService(UserRepository users, PublicationRepository pubs, LoanRepository loans,
                          ReadListRepository readListItems, PasswordEncoder encoder,
                          FileStorageService files, NotificationService notifications) {
        this.users = users;
        this.pubs = pubs;
        this.loans = loans;
        this.readListItems = readListItems;
        this.encoder = encoder;
        this.files = files;
        this.notifications = notifications;
    }

    public int getLoanDays() { return loanDays; }

    public int getFinePerDay() { return finePerDay; }

    // ------------------------------------------------------------ пользователи

    public AppUser createUser(String fullName, String email, String password, String phone, Role role) {
        String mail = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(mail)) {
            throw new LibraryException("Эта почта уже зарегистрирована.");
        }
        AppUser u = new AppUser();
        u.setFullName(fullName.trim());
        u.setEmail(mail);
        u.setPasswordHash(encoder.encode(password));
        u.setPhone(blank(phone) ? null : phone.trim());
        u.setRole(role);
        u = users.save(u);
        if (role == Role.READER) {
            u.setTicketNumber(String.format("ЧБ-%05d", u.getId()));
        }
        return u;
    }

    public AppUser registerReader(String fullName, String email, String password, String phone) {
        return createUser(fullName, email, password, phone, Role.READER);
    }

    @Transactional(readOnly = true)
    public AppUser user(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new LibraryException("Пользователь не найден."));
    }
    public void setAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new LibraryException("Выберите картинку.");
        }
        AppUser u = user(email);
        String stored = files.saveAvatar(file);
        if (u.getAvatarFile() != null) {
            files.deleteAvatar(u.getAvatarFile());
        }
        u.setAvatarFile(stored);
    }

    public void removeAvatar(String email) {
        AppUser u = user(email);
        if (u.getAvatarFile() != null) {
            files.deleteAvatar(u.getAvatarFile());
            u.setAvatarFile(null);
        }
    }
    public void updateProfile(String email, String fullName, String phone) {
        if (blank(fullName)) {
            throw new LibraryException("Укажите имя.");
        }
        AppUser u = user(email);
        u.setFullName(fullName.trim());
        u.setPhone(blank(phone) ? null : phone.trim());
    }

    public void toggleBlock(Long userId) {
        AppUser u = users.findById(userId).orElseThrow(() -> new LibraryException("Читатель не найден."));
        if (u.getRole() != Role.READER) {
            throw new LibraryException("Заблокировать можно только читателя.");
        }
        u.setBlocked(!u.isBlocked());
        notifications.onBlockChanged(u);
    }

    @Transactional(readOnly = true)
    public List<AppUser> activeReaders() {
        return users.findByRoleOrderByFullName(Role.READER).stream().filter(u -> !u.isBlocked()).toList();
    }

    @Transactional(readOnly = true)
    public List<ReaderRow> readerRows(String q) {
        String s = q == null ? "" : q.trim().toLowerCase();
        Map<Long, List<Loan>> byUser = loans.findAll().stream()
                .collect(Collectors.groupingBy(l -> l.getUser().getId()));
        return users.findByRoleOrderByFullName(Role.READER).stream()
                .filter(u -> s.isEmpty() || has(u.getFullName(), s) || has(u.getEmail(), s) || has(u.getTicketNumber(), s))
                .map(u -> {
                    List<Loan> ls = byUser.getOrDefault(u.getId(), List.of());
                    return new ReaderRow(u,
                            ls.stream().filter(l -> !l.isReturned()).count(),
                            ls.stream().filter(Loan::isFineUnpaid).mapToLong(Loan::getFine).sum());
                })
                .toList();
    }

    // ------------------------------------------------------------ каталог

    @Transactional(readOnly = true)
    public List<Publication> search(String q, PublicationType type) {
        return search(q, type, null, "title", false);
    }

    /** sort: title (по алфавиту) | new (новые) | popular (чаще берут) */
    @Transactional(readOnly = true)
    public List<Publication> search(String q, PublicationType type, String genre, String sort, boolean onlyAvailable) {
        String s = q == null ? "" : q.trim().toLowerCase();
        Comparator<Publication> byTitle = Comparator.comparing((Publication p) -> p.getTitle().toLowerCase());
        Comparator<Publication> cmp;
        if ("new".equals(sort)) {
            cmp = (a, b) -> {
                LocalDate x = a.getAddedAt() == null ? LocalDate.MIN : a.getAddedAt();
                LocalDate y = b.getAddedAt() == null ? LocalDate.MIN : b.getAddedAt();
                int c = y.compareTo(x);
                return c != 0 ? c : Long.compare(b.getId(), a.getId());
            };
        } else if ("popular".equals(sort)) {
            Map<Long, Long> counts = loans.findAll().stream()
                    .collect(Collectors.groupingBy(l -> l.getPublication().getId(), Collectors.counting()));
            cmp = (a, b) -> {
                int c = Long.compare(counts.getOrDefault(b.getId(), 0L), counts.getOrDefault(a.getId(), 0L));
                return c != 0 ? c : byTitle.compare(a, b);
            };
        } else {
            cmp = byTitle;
        }
        return pubs.findAll().stream()
                .filter(p -> type == null || p.getType() == type)
                .filter(p -> genre == null || genre.isBlank() || genre.equals(p.getGenre()))
                .filter(p -> !onlyAvailable || p.getAvailableCopies() > 0)
                .filter(p -> s.isEmpty() || has(p.getTitle(), s) || has(p.getAuthor(), s) || has(p.getGenre(), s))
                .sorted(cmp)
                .toList();
    }

    @Transactional(readOnly = true)
    public Publication publication(Long id) {
        return pubs.findById(id).orElseThrow(() -> new LibraryException("Издание не найдено."));
    }

    @Transactional(readOnly = true)
    public List<Publication> availablePublications() {
        return pubs.findAll(Sort.by("title")).stream().filter(p -> p.getAvailableCopies() > 0).toList();
    }

    public Publication savePublication(Publication form, MultipartFile cover, boolean removeCover) {
        if (blank(form.getTitle()) || blank(form.getAuthor())) {
            throw new LibraryException("Укажите название и автора.");
        }
        if (form.getTotalCopies() < 1) {
            throw new LibraryException("Экземпляров должно быть не меньше одного.");
        }
        Publication p = form.getId() == null ? new Publication()
                : pubs.findById(form.getId()).orElseThrow(() -> new LibraryException("Издание не найдено."));
        long onHand = p.getId() == null ? 0 : loans.countByPublicationAndReturnedAtIsNull(p);
        if (form.getTotalCopies() < onHand) {
            throw new LibraryException("Сейчас на руках " + onHand + " экз. — всего должно быть не меньше.");
        }
        p.setType(form.getType() == null ? PublicationType.BOOK : form.getType());
        p.setTitle(form.getTitle().trim());
        p.setAuthor(form.getAuthor().trim());
        p.setPublishYear(form.getPublishYear());
        p.setGenre(blank(form.getGenre()) ? null : form.getGenre().trim());
        p.setAnnotation(blank(form.getAnnotation()) ? null : form.getAnnotation().trim());
        p.setTotalCopies(form.getTotalCopies());
        p.setAvailableCopies((int) (form.getTotalCopies() - onHand));

        if (removeCover && p.getCoverFile() != null) {
            files.delete(p.getCoverFile());
            p.setCoverFile(null);
        }
        if (cover != null && !cover.isEmpty()) {
            String old = p.getCoverFile();
            p.setCoverFile(files.save(cover));
            if (old != null) {
                files.delete(old);
            }
        }
        return pubs.save(p);
    }

    public void deletePublication(Long id) {
        Publication p = publication(id);
        if (loans.existsByPublication(p)) {
            throw new LibraryException("Нельзя удалить: по этому изданию есть выдачи в истории.");
        }
        if (p.getCoverFile() != null) {
            files.delete(p.getCoverFile());
        }
        readListItems.deleteByPublication(p);
        pubs.delete(p);
    }

    // ------------------------------------------------------------ выдачи и штрафы

    public Loan borrow(String email, Long publicationId) {
        return issue(user(email), publicationId);
    }
    /** Добавляет издание в ReadList или убирает, если оно уже там. */
    public void toggleReadList(String email, Long publicationId) {
        AppUser u = user(email);
        Publication p = publication(publicationId);
        Optional<ReadListItem> existing = readListItems.findByUserAndPublication(u, p);
        if (existing.isPresent()) {
            readListItems.delete(existing.get());
        } else {
            ReadListItem item = new ReadListItem();
            item.setUser(u);
            item.setPublication(p);
            readListItems.save(item);
        }
    }

    @Transactional(readOnly = true)
    public List<Publication> readList(String email) {
        return readListItems.findByUserOrderByIdDesc(user(email)).stream()
                .map(ReadListItem::getPublication).toList();
    }

    @Transactional(readOnly = true)
    public boolean inReadList(String email, Long publicationId) {
        return readListItems.findByUserAndPublication(user(email), publication(publicationId)).isPresent();
    }
    public Loan issueTo(Long userId, Long publicationId) {
        AppUser u = users.findById(userId).orElseThrow(() -> new LibraryException("Читатель не найден."));
        if (u.getRole() != Role.READER) {
            throw new LibraryException("Выдать можно только читателю.");
        }
        return issue(u, publicationId);
    }

    private Loan issue(AppUser user, Long publicationId) {
        Publication pub = publication(publicationId);
        if (user.isBlocked()) {
            throw new LibraryException("Аккаунт читателя заблокирован.");
        }
        List<Loan> mine = loans.findByUserOrderByIssuedAtDescIdDesc(user);
        if (mine.stream().anyMatch(Loan::isOverdue)) {
            throw new LibraryException("Есть просроченное издание — сначала верните его.");
        }
        if (mine.stream().anyMatch(Loan::isFineUnpaid)) {
            throw new LibraryException("Есть неоплаченный штраф — оплатите его у библиотекаря.");
        }
        if (mine.stream().filter(l -> !l.isReturned()).count() >= maxActive) {
            throw new LibraryException("Одновременно на руках может быть не больше " + maxActive + " изданий.");
        }
        if (mine.stream().anyMatch(l -> !l.isReturned() && l.getPublication().getId().equals(pub.getId()))) {
            throw new LibraryException("Это издание уже у вас на руках.");
        }
        if (pub.getAvailableCopies() <= 0) {
            throw new LibraryException("Свободных экземпляров нет.");
        }
        pub.setAvailableCopies(pub.getAvailableCopies() - 1);
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setPublication(pub);
        loan.setIssuedAt(LocalDate.now());
        loan.setDueDate(LocalDate.now().plusDays(loanDays));
        loan.setFinePerDay(finePerDay);
        Loan saved = loans.save(loan);
        notifications.onIssued(saved);
        return saved;
    }

    public void returnLoan(Long loanId) {
        Loan l = loan(loanId);
        if (l.isReturned()) {
            throw new LibraryException("Это издание уже возвращено.");
        }
        l.setReturnedAt(LocalDate.now());
        Publication p = l.getPublication();
        p.setAvailableCopies(p.getAvailableCopies() + 1);
        notifications.onReturned(l);
    }

    public void payFine(Long loanId) {
        Loan l = loan(loanId);
        if (!l.isReturned()) {
            throw new LibraryException("Штраф растёт, пока издание не возвращено. Сначала примите возврат.");
        }
        if (l.getFine() == 0 || l.isFinePaid()) {
            throw new LibraryException("По этой выдаче нечего оплачивать.");
        }
        l.setFinePaid(true);
        notifications.onFinePaid(l);
    }

    private Loan loan(Long id) {
        return loans.findById(id).orElseThrow(() -> new LibraryException("Выдача не найдена."));
    }

    @Transactional(readOnly = true)
    public List<Loan> myLoans(String email) {
        return loans.findByUserOrderByIssuedAtDescIdDesc(user(email));
    }

    /** filter: all | active | overdue | returned */
    @Transactional(readOnly = true)
    public List<Loan> loansByFilter(String filter) {
        return loans.findAllByOrderByIssuedAtDescIdDesc().stream()
                .filter(l -> switch (filter == null ? "all" : filter) {
                    case "active" -> !l.isReturned();
                    case "overdue" -> l.isOverdue();
                    case "returned" -> l.isReturned();
                    default -> true;
                })
                .toList();
    }

    /** Все выдачи, по которым есть штраф; неоплаченные — сверху. */
    @Transactional(readOnly = true)
    public List<Loan> finedLoans() {
        return loans.findAllByOrderByIssuedAtDescIdDesc().stream()
                .filter(l -> l.getFine() > 0)
                .sorted(Comparator.comparing(Loan::isFineUnpaid).reversed())
                .toList();
    }

    // ------------------------------------------------------------ статистика

    @Transactional(readOnly = true)
    public Stats stats() {
        List<Loan> all = loans.findAll();
        return new Stats(
                users.findByRoleOrderByFullName(Role.READER).size(),
                pubs.count(),
                all.stream().filter(l -> !l.isReturned()).count(),
                all.stream().filter(Loan::isOverdue).count(),
                all.stream().filter(Loan::isFineUnpaid).mapToLong(Loan::getFine).sum());
    }

    @Transactional(readOnly = true)
    public List<Popular> popular() {
        return loans.findAll().stream()
                .collect(Collectors.groupingBy(l -> l.getPublication().getTitle(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new Popular(e.getKey(), e.getValue()))
                .toList();
    }

    // ------------------------------------------------------------ утилиты

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean has(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }
}
