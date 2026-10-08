package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.records.requests.CreateCardRequest;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;

import java.math.BigDecimal;

/**
 * Business operations for benefit card management.
 */
public interface CardService {

    /**
     * Creates a new card with an initial balance of R$500.00.
     *
     * @param request contains the card number and plain-text PIN
     * @return the created card payload (card number + raw PIN echoed back)
     * @throws com.br.vr.miniautorizador.exceptions.CardAlreadyExistsException
     *         if a card with the same {@code numeroCartao} already exists
     */
    CreateCardResponse createCard(CreateCardRequest request);

    /**
     * Returns the current available balance for the given card number.
     *
     * @param cardNumber the card identifier
     * @return current balance, always {@code >= 0.00}
     * @throws com.br.vr.miniautorizador.exceptions.CardNotFoundException
     *         if no card with that number exists
     */
    BigDecimal getBalance(String cardNumber);
}
