package ru.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    private Long loanId;

    /** SOON — срок возврата близко, OVERDUE — просрочка. */
    private String kind;

    private String title;

    @Column(length = 500)
    private String text;

    private LocalDateTime createdAt = LocalDateTime.now();

    private boolean readFlag;
}