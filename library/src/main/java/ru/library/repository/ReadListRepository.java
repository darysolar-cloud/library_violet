package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.AppUser;
import ru.library.model.Publication;
import ru.library.model.ReadListItem;

import java.util.List;
import java.util.Optional;

public interface ReadListRepository extends JpaRepository<ReadListItem, Long> {
    List<ReadListItem> findByUserOrderByIdDesc(AppUser user);

    Optional<ReadListItem> findByUserAndPublication(AppUser user, Publication publication);

    void deleteByPublication(Publication publication);
}