package ru.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor
public class DiaryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @ManyToOne(optional = false)
    private Publication publication;

    /** 0 — без оценки, иначе от 1 до 10. */
    private int rating;

    private LocalDate readDate;

    private String mood;

    /** Теги через запятую. */
    @Column(length = 500)
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String opinion;

    @Column(columnDefinition = "TEXT")
    private String associations;

    /** Любимые моменты, по одному на строку. */
    @Column(columnDefinition = "TEXT")
    private String moments;

    /** Сцены и персонажи. */
    @Column(length = 500)
    private String characters;

    /** Быстрая заметка. */
    @Column(columnDefinition = "TEXT")
    private String note;

    private boolean favorite;

    private boolean draft;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public List<String> getMomentLines() {
        if (moments == null || moments.isBlank()) {
            return List.of();
        }
        return Arrays.stream(moments.split("\\R")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}