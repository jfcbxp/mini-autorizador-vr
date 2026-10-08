package com.br.vr.miniautorizador.exceptions;

/**
 * Thrown when a GET /cartoes/{numeroCartao} request references a card
 * that does not exist in the database.
 *
 * <p>Mapped to HTTP 404 with no response body by {@code RestExceptionHandler}.
 */
public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String cardNumber) {
        super("Card not found: " + cardNumber);
    }
}
