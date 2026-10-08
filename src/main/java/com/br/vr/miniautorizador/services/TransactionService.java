package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.records.requests.TransactionRequest;

public interface TransactionService {

    void authorize(TransactionRequest request);
}
