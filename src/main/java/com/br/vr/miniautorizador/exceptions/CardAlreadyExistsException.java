package com.br.vr.miniautorizador.exceptions;

import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import lombok.Getter;

@Getter
public class CardAlreadyExistsException extends RuntimeException {

    private final CreateCardResponse card;

    public CardAlreadyExistsException(CreateCardResponse card) {
        super("Card already exists: " + card.numeroCartao());
        this.card = card;
    }
}
