package com.br.vr.miniautorizador.repositories;

import com.br.vr.miniautorizador.domains.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data access layer for {@link Card} entities.
 *
 * <p>Spring Data JPA provides all CRUD operations. The {@code @Version} field
 * on {@link Card} is handled transparently by Hibernate — no custom queries
 * are needed for optimistic locking.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, String> {
}
