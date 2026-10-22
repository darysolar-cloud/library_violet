package ru.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Getter @Setter @NoArgsConstructor
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser user;

    @ManyToOne(optional = false)
    private Publication publication;

    private LocalDate issuedAt;
    private LocalDate dueDate;
    private LocalDate returnedAt;

    /** Тариф штрафа (₽ в день) фиксируется на момент выдачи. */
    private int finePerDay;

    private boolean finePaid;

    public boolean isReturned() {
        return returnedAt != null;
    }

    /** Дней просрочки: до возврата считается по сегодняшнему дню, после — по дате возврата. */
    public long getOverdueDays() {
        LocalDate end = returnedAt != null ? returnedAt : LocalDate.now();
        return Math.max(ChronoUnit.DAYS.between(dueDate, end), 0);
    }

    public boolean isOverdue() {
        return returnedAt == null && getOverdueDays() > 0;
    }

    public long getDaysLeft() {
        return ChronoUnit.DAYS.between(LocalDate.now(), dueDate);
    }

    /** Штраф считается автоматически: дни просрочки × тариф. */
    public long getFine() {
        return getOverdueDays() * finePerDay;
    }

    public boolean isFineUnpaid() {
        return getFine() > 0 && !finePaid;
    }
}
