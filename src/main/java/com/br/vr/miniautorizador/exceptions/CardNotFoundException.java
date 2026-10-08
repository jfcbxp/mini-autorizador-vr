package com.br.vr.miniautorizador.exceptions;

public class CardNotFoundException extends RuntimeException {

    public CardNotFoundException(String cardNumber) {
        super("Card not found: " + cardNumber);
    }
}
