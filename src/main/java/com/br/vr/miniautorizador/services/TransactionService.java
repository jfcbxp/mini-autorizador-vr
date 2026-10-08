package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.records.requests.TransactionRequest;

/**
 * Business operations for transaction authorization.
 */
public interface TransactionService {

    /**
     * Authorizes a debit transaction against the specified card.
     *
     * <p>Evaluates all authorization rules in precedence order.
     * On success, atomically debits the transaction amount from the card balance.
     *
     * @param request contains card number, PIN, and transaction amount
     * @throws com.br.vr.miniautorizador.exceptions.AuthorizationException
     *         if any authorization rule fails
     */
    void authorize(TransactionRequest request);
}
