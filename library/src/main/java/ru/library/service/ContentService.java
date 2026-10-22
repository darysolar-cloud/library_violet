package ru.library.service;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.library.model.*;
import ru.library.repository.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContentService {

    private final GenreRepository genres;
    private final PublicationRepository pubs;
    private final QuoteRepository quotes;
    private final FavoriteQuoteRepository favorites;
    private final BookOfDayRepository bookOfDays;
    private final UserRepository users;

    public ContentService(GenreRepository genres, PublicationRepository pubs, QuoteRepository quotes,
                          FavoriteQuoteRepository favorites, BookOfDayRepository bookOfDays, UserRepository users) {
        this.genres = genres;
        this.pubs = pubs;
        this.quotes = quotes;
        this.favorites = favorites;
        this.bookOfDays = bookOfDays;
        this.users = users;
    }

    // ------------------------------------------------------------ жанры

    @Transactional(readOnly = true)
    public List<Genre> genres() {
        return genres.findAllByOrderByName();
    }

    @Transactional(readOnly = true)
    public List<String> genreNames() {
        return genres().stream().map(Genre::getName).toList();
    }

    /** Сколько изданий в каждом жанре. */
    @Transactional(readOnly = true)
    public Map<String, Long> genreCounts() {
        return pubs.findAll().stream()
                .filter(p -> p.getGenre() != null)
                .collect(Collectors.groupingBy(Publication::getGenre, Collectors.counting()));
    }

    public void addGenre(String name) {
        String n = clean(name);
        if (n.isEmpty()) {
            throw new LibraryException("Введите название жанра.");
        }
        if (genres.findByNameIgnoreCase(n).isPresent()) {
            throw new LibraryException("Такой жанр уже есть.");
        }
        Genre g = new Genre();
        g.setName(n);
        genres.save(g);
    }

    /** При переименовании жанр обновляется и у всех изданий. */
    public void renameGenre(Long id, String name) {
        Genre g = genre(id);
        String n = clean(name);
        if (n.isEmpty()) {
            throw new LibraryException("Введите название жанра.");
        }
        Optional<Genre> same = genres.findByNameIgnoreCase(n);
        if (same.isPresent() && !same.get().getId().equals(id)) {
            throw new LibraryException("Жанр с таким названием уже есть.");
        }
        String old = g.getName();
        pubs.findAll().stream().filter(p -> old.equals(p.getGenre())).forEach(p -> p.setGenre(n));
        g.setName(n);
    }

    public void deleteGenre(Long id) {
        Genre g = genre(id);
        long used = pubs.findAll().stream().filter(p -> g.getName().equals(p.getGenre())).count();
        if (used > 0) {
            throw new LibraryException("Жанр используется в изданиях (" + used + "). Сначала смените жанр у этих изданий.");
        }
        genres.delete(g);
    }

    private Genre genre(Long id) {
        return genres.findById(id).orElseThrow(() -> new LibraryException("Жанр не найден."));
    }

    // ------------------------------------------------------------ цитаты

    /** Одна цитата на день: выбирается по дате, у всех читателей одинаковая. */
    @Transactional(readOnly = true)
    public Quote quoteOfDay() {
        List<Quote> all = quotes.findAll(Sort.by("id"));
        if (all.isEmpty()) {
            return null;
        }
        return all.get((int) (LocalDate.now().toEpochDay() % all.size()));
    }

    @Transactional(readOnly = true)
    public List<Quote> allQuotes() {
        return quotes.findAll(Sort.by("id"));
    }

    public void addQuote(String body, String author) {
        if (clean(body).isEmpty() || clean(author).isEmpty()) {
            throw new LibraryException("Заполните текст цитаты и автора.");
        }
        Quote q = new Quote();
        q.setBody(clean(body));
        q.setAuthor(clean(author));
        quotes.save(q);
    }

    public void deleteQuote(Long id) {
        Quote q = quote(id);
        favorites.deleteByQuote(q);
        quotes.delete(q);
    }

    public void toggleFavorite(String email, Long quoteId) {
        AppUser u = userByEmail(email);
        Quote q = quote(quoteId);
        Optional<FavoriteQuote> existing = favorites.findByUserAndQuote(u, q);
        if (existing.isPresent()) {
            favorites.delete(existing.get());
        } else {
            FavoriteQuote f = new FavoriteQuote();
            f.setUser(u);
            f.setQuote(q);
            favorites.save(f);
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String email, Long quoteId) {
        return favorites.findByUserAndQuote(userByEmail(email), quote(quoteId)).isPresent();
    }

    @Transactional(readOnly = true)
    public List<Quote> favoriteQuotes(String email) {
        return favorites.findByUserOrderByIdDesc(userByEmail(email)).stream().map(FavoriteQuote::getQuote).toList();
    }

    private Quote quote(Long id) {
        return quotes.findById(id).orElseThrow(() -> new LibraryException("Цитата не найдена."));
    }

    private AppUser userByEmail(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() -> new LibraryException("Пользователь не найден."));
    }

    // ------------------------------------------------------------ книга дня

    /** Выбранная библиотекарем на сегодня, а если не выбрана — случайная доступная (одна и та же в течение дня). */
    @Transactional(readOnly = true)
    public Publication bookOfDay() {
        LocalDate today = LocalDate.now();
        Optional<BookOfDay> manual = bookOfDays.findById(today);
        if (manual.isPresent()) {
            Optional<Publication> p = pubs.findById(manual.get().getPublicationId());
            if (p.isPresent()) {
                return p.get();
            }
        }
        List<Publication> available = pubs.findAll(Sort.by("id")).stream()
                .filter(p -> p.getAvailableCopies() > 0).toList();
        if (available.isEmpty()) {
            return null;
        }
        return available.get(new Random(today.toEpochDay()).nextInt(available.size()));
    }

    public void setBookOfDay(Long publicationId) {
        pubs.findById(publicationId).orElseThrow(() -> new LibraryException("Издание не найдено."));
        BookOfDay b = new BookOfDay();
        b.setDay(LocalDate.now());
        b.setPublicationId(publicationId);
        bookOfDays.save(b);
    }

    public void resetBookOfDay() {
        if (bookOfDays.existsById(LocalDate.now())) {
            bookOfDays.deleteById(LocalDate.now());
        }
    }

    // ------------------------------------------------------------ начальные данные

    /** Заполняет справочник жанров из уже существующих изданий и добавляет несколько цитат. */
    public void seedDefaults() {
        if (genres.count() == 0) {
            Set<String> names = new TreeSet<>(List.of("Классика", "Роман", "Фантастика", "Детектив", "Наука", "Программирование"));
            pubs.findAll().stream().map(Publication::getGenre)
                    .filter(g -> g != null && !g.isBlank()).forEach(names::add);
            names.forEach(n -> {
                Genre g = new Genre();
                g.setName(n);
                genres.save(g);
            });
        }
        if (quotes.count() == 0) {
            sample("Книга — это мечта, которую вы держите в руках.", "Нил Гейман");
            sample("Чтение — вот лучшее учение.", "А. С. Пушкин");
            sample("Книги — корабли мысли, странствующие по волнам времени.", "Фрэнсис Бэкон");
            sample("Рукописи не горят.", "М. А. Булгаков");
            sample("Нет такого корабля, как книга, чтобы унести нас в дальние страны.", "Эмили Дикинсон");
        }
    }

    private void sample(String body, String author) {
        Quote q = new Quote();
        q.setBody(body);
        q.setAuthor(author);
        quotes.save(q);
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }
}