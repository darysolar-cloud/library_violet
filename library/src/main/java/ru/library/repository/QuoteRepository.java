// QuoteRepository.java
package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.Quote;

public interface QuoteRepository extends JpaRepository<Quote, Long> {
}