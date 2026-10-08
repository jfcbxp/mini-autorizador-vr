package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import com.br.vr.miniautorizador.utils.CardMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(3)
public class SufficientBalanceRule implements AuthorizationRule {

    @Override
    public void evaluate(Card card, TransactionRequest request) {
        log.info("SufficientBalanceRule.evaluate - cardNumber: {}, balance: {}, amount: {}",
                CardMask.mask(request.numeroCartao()), card.getBalance(), request.valor());

        Optional.of(card.getBalance().compareTo(request.valor()) >= 0)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                        s -> log.info("SufficientBalanceRule.evaluate - passed - cardNumber: {}", CardMask.mask(request.numeroCartao())),
                        () -> {
                            log.info("SufficientBalanceRule.evaluate - failed: SALDO_INSUFICIENTE - cardNumber: {}", CardMask.mask(request.numeroCartao()));
                            throw new AuthorizationException(TransactionError.SALDO_INSUFICIENTE);
                        }
                );
    }
}
