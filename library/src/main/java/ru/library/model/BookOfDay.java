// BookOfDay.java  (ручной выбор книги дня; одна запись на дату)
package ru.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class BookOfDay {
    @Id
    private LocalDate day;

    private Long publicationId;
}