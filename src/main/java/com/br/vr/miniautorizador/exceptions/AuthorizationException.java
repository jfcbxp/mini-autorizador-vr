package com.br.vr.miniautorizador.exceptions;

import com.br.vr.miniautorizador.enums.TransactionError;
import lombok.Getter;

@Getter
public class AuthorizationException extends RuntimeException {

    private final TransactionError error;

    public AuthorizationException(TransactionError error) {
        super("Transaction denied: " + error.name());
        this.error = error;
    }
}
