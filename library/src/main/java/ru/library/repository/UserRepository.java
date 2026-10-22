package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.AppUser;
import ru.library.model.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AppUser> findByRoleOrderByFullName(Role role);
}
