// BookOfDayRepository.java
package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.BookOfDay;

import java.time.LocalDate;

public interface BookOfDayRepository extends JpaRepository<BookOfDay, LocalDate> {
}