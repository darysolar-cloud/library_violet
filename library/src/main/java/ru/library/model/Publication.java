package ru.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor
public class Publication {
    private LocalDate addedAt = LocalDate.now();
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationType type = PublicationType.BOOK;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    private Integer publishYear;

    private String genre;

    /** Аннотация: обычный текст, добавляется через админку. */
    @Column(columnDefinition = "TEXT")
    private String annotation;

    /** Имя файла обложки в папке uploads/covers (null — показывается заглушка). */
    private String coverFile;

    private int totalCopies = 1;

    private int availableCopies = 1;

    public String getCoverUrl() {
        return coverFile == null ? null : "/covers/" + coverFile;
    }
}
