package com.br.vr.miniautorizador.exceptions;

import com.br.vr.miniautorizador.enums.TransactionError;
import lombok.Getter;

/**
 * Thrown by an {@code AuthorizationRule} when a transaction fails to satisfy
 * one of the authorization conditions.
 *
 * <p>Carries the {@link TransactionError} code that identifies the failing rule.
 * Mapped to HTTP 422 with the error code name as the plain-text response body
 * by {@code RestExceptionHandler}.
 *
 * <p>No-if design: each {@code AuthorizationRule} implementation simply throws
 * this exception instead of returning a boolean, which allows the rule chain
 * to be iterated with {@code forEach} without any conditional branching.
 */
@Getter
public class AuthorizationException extends RuntimeException {

    private final TransactionError error;

    public AuthorizationException(TransactionError error) {
        super("Transaction denied: " + error.name());
        this.error = error;
    }
}
