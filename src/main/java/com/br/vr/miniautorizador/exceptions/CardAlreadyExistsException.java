package com.br.vr.miniautorizador.exceptions;

import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import lombok.Getter;

/**
 * Thrown when a POST /cartoes request tries to create a card with a
 * {@code numeroCartao} that already exists in the database.
 *
 * <p>The exception carries the original request payload so that
 * {@code RestExceptionHandler} can echo it back as the HTTP 422 response body,
 * as required by the contract.
 */
@Getter
public class CardAlreadyExistsException extends RuntimeException {

    private final CreateCardResponse card;

    public CardAlreadyExistsException(CreateCardResponse card) {
        super("Card already exists: " + card.numeroCartao());
        this.card = card;
    }
}
