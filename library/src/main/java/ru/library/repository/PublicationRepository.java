package ru.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.library.model.Publication;

public interface PublicationRepository extends JpaRepository<Publication, Long> {
}
