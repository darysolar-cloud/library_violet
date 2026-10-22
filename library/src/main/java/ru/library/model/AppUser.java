package ru.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "app_user")
@Getter @Setter @NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.READER;

    private boolean blocked;

    /** Номер читательского билета (только у читателей). */
    private String ticketNumber;

    private LocalDate registeredAt = LocalDate.now();
    /** Имя файла аватарки в папке uploads/avatars (null — показывается буква имени). */
    private String avatarFile;

    public String getAvatarUrl() {
        return avatarFile == null ? null : "/avatars/" + avatarFile;
    }
}
