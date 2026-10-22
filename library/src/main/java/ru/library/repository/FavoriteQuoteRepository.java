// FavoriteQuoteRepository.java
package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.AppUser;
import ru.library.model.FavoriteQuote;
import ru.library.model.Quote;

import java.util.List;
import java.util.Optional;

public interface FavoriteQuoteRepository extends JpaRepository<FavoriteQuote, Long> {
    Optional<FavoriteQuote> findByUserAndQuote(AppUser user, Quote quote);

    List<FavoriteQuote> findByUserOrderByIdDesc(AppUser user);

    void deleteByQuote(Quote quote);
}