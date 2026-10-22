package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.AppUser;
import ru.library.model.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDescIdDesc(AppUser user);

    boolean existsByUserAndLoanIdAndKind(AppUser user, Long loanId, String kind);

    long countByUserAndReadFlagFalse(AppUser user);

    long countByUser(AppUser user);
}