package com.br.vr.miniautorizador.repositories;

import com.br.vr.miniautorizador.domains.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
}
