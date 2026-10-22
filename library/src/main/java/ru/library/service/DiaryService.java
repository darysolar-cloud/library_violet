package ru.library.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.model.*;
import ru.library.repository.DiaryEntryRepository;
import ru.library.repository.LoanRepository;
import ru.library.repository.PublicationRepository;
import ru.library.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DiaryService {

    public static final List<String> MOODS =
            List.of("Вдохновляющее", "Уютное", "Тревожное", "Грустное", "Весёлое", "Задумчивое", "Перечитать");

    private final DiaryEntryRepository entries;
    private final UserRepository users;
    private final PublicationRepository pubs;
    private final LoanRepository loans;

    public DiaryService(DiaryEntryRepository entries, UserRepository users,
                        PublicationRepository pubs, LoanRepository loans) {
        this.entries = entries;
        this.users = users;
        this.pubs = pubs;
        this.loans = loans;
    }

    @Transactional(readOnly = true)
    public List<DiaryEntry> entries(String email) {
        return entries.findByUserOrderByUpdatedAtDesc(user(email));
    }

    /** Запись читателя; чужие записи недоступны. */
    @Transactional(readOnly = true)
    public DiaryEntry entry(String email, Long id) {
        AppUser u = user(email);
        DiaryEntry e = entries.findById(id).orElseThrow(() -> new LibraryException("Запись не найдена."));
        if (!e.getUser().getId().equals(u.getId())) {
            throw new LibraryException("Запись не найдена.");
        }
        return e;
    }

    @Transactional(readOnly = true)
    public Optional<DiaryEntry> entryFor(String email, Long publicationId) {
        AppUser u = user(email);
        return pubs.findById(publicationId).flatMap(p -> entries.findByUserAndPublication(u, p));
    }

    /** Для кнопок в «Мои выдачи»: id издания → id записи. */
    @Transactional(readOnly = true)
    public Map<Long, Long> entryIdsByPublication(String email) {
        return entries.findByUserOrderByUpdatedAtDesc(user(email)).stream()
                .collect(Collectors.toMap(e -> e.getPublication().getId(), DiaryEntry::getId, (a, b) -> a));
    }

    /** Книги, которые читатель брал и по которым ещё нет записи. */
    @Transactional(readOnly = true)
    public List<Publication> candidates(String email) {
        AppUser u = user(email);
        Set<Long> written = entries.findByUserOrderByUpdatedAtDesc(u).stream()
                .map(e -> e.getPublication().getId()).collect(Collectors.toSet());
        return loans.findByUserOrderByIssuedAtDescIdDesc(u).stream()
                .map(Loan::getPublication)
                .filter(p -> !written.contains(p.getId()))
                .distinct()
                .toList();
    }

    public DiaryEntry save(String email, Long entryId, DiaryForm f, boolean draft) {
        AppUser u = user(email);
        DiaryEntry e;
        if (entryId == null) {
            if (f.getPublicationId() == null) {
                throw new LibraryException("Выберите книгу.");
            }
            Publication p = pubs.findById(f.getPublicationId())
                    .orElseThrow(() -> new LibraryException("Издание не найдено."));
            boolean borrowed = loans.findByUserOrderByIssuedAtDescIdDesc(u).stream()
                    .anyMatch(l -> l.getPublication().getId().equals(p.getId()));
            if (!borrowed) {
                throw new LibraryException("Записи можно вести только по книгам, которые вы брали в библиотеке.");
            }
            if (entries.findByUserAndPublication(u, p).isPresent()) {
                throw new LibraryException("Запись по этой книге уже есть — откройте её и отредактируйте.");
            }
            e = new DiaryEntry();
            e.setUser(u);
            e.setPublication(p);
        } else {
            e = entry(email, entryId);
        }
        if (f.getRating() < 0 || f.getRating() > 10) {
            throw new LibraryException("Оценка — от 0 до 10.");
        }
        if (f.getReadDate() != null && f.getReadDate().isAfter(LocalDate.now())) {
            throw new LibraryException("Дата прочтения не может быть в будущем.");
        }
        e.setRating(f.getRating());
        e.setReadDate(f.getReadDate());
        e.setMood(f.getMood() != null && MOODS.contains(f.getMood()) ? f.getMood() : null);
        e.setTags(normalizeTags(f.getTags()));
        e.setOpinion(text(f.getOpinion()));
        e.setAssociations(text(f.getAssociations()));
        e.setMoments(text(f.getMoments()));
        e.setCharacters(text(f.getCharacters()));
        e.setFavorite(f.isFavorite());
        e.setDraft(draft);
        e.setUpdatedAt(LocalDateTime.now());
        return entries.save(e);
    }

    public void saveNote(String email, Long id, String note) {
        DiaryEntry e = entry(email, id);
        e.setNote(text(note));
        e.setUpdatedAt(LocalDateTime.now());
    }

    public void toggleFavorite(String email, Long id) {
        DiaryEntry e = entry(email, id);
        e.setFavorite(!e.isFavorite());
    }

    public void delete(String email, Long id) {
        entries.delete(entry(email, id));
    }

    private AppUser user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new LibraryException("Пользователь не найден."));
    }

    private static String text(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String normalizeTags(String raw) {
        if (raw == null) {
            return null;
        }
        String joined = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(t -> t.length() > 30 ? t.substring(0, 30) : t)
                .distinct()
                .limit(8)
                .collect(Collectors.joining(", "));
        return joined.isEmpty() ? null : joined;
    }
}