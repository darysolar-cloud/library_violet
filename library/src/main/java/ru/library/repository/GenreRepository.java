// GenreRepository.java
package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.Genre;

import java.util.List;
import java.util.Optional;

public interface GenreRepository extends JpaRepository<Genre, Long> {
    Optional<Genre> findByNameIgnoreCase(String name);

    List<Genre> findAllByOrderByName();
}