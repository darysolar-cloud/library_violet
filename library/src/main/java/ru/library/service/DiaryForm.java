package ru.library.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import ru.library.model.DiaryEntry;

import java.time.LocalDate;

@Getter @Setter
public class DiaryForm {

    private Long publicationId;
    private int rating;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate readDate;

    private String mood;
    private String tags;
    private String opinion;
    private String associations;
    private String moments;
    private String characters;
    private boolean favorite;

    public static DiaryForm of(DiaryEntry e) {
        DiaryForm f = new DiaryForm();
        f.publicationId = e.getPublication().getId();
        f.rating = e.getRating();
        f.readDate = e.getReadDate();
        f.mood = e.getMood();
        f.tags = e.getTags();
        f.opinion = e.getOpinion();
        f.associations = e.getAssociations();
        f.moments = e.getMoments();
        f.characters = e.getCharacters();
        f.favorite = e.isFavorite();
        return f;
    }
}