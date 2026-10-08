package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;

@FunctionalInterface
public interface AuthorizationRule {
    void evaluate(Card card, TransactionRequest request);
}
