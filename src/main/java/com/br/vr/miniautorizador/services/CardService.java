package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.records.requests.CreateCardRequest;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;

import java.math.BigDecimal;

public interface CardService {

    CreateCardResponse createCard(CreateCardRequest request);

    BigDecimal getBalance(String cardNumber);
}
