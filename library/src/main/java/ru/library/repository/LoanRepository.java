package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.AppUser;
import ru.library.model.Loan;
import ru.library.model.Publication;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserOrderByIssuedAtDescIdDesc(AppUser user);

    List<Loan> findAllByOrderByIssuedAtDescIdDesc();

    boolean existsByPublication(Publication publication);

    long countByPublicationAndReturnedAtIsNull(Publication publication);
}
