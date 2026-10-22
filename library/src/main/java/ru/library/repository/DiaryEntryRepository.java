package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.AppUser;
import ru.library.model.DiaryEntry;
import ru.library.model.Publication;

import java.util.List;
import java.util.Optional;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    List<DiaryEntry> findByUserOrderByUpdatedAtDesc(AppUser user);

    Optional<DiaryEntry> findByUserAndPublication(AppUser user, Publication publication);
}