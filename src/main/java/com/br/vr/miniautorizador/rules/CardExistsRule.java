package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(1)
public class CardExistsRule implements AuthorizationRule {

    @Override
    public void evaluate(Card card, TransactionRequest request) {
        log.info("CardExistsRule.evaluate - cardNumber: {}", mask(request.numeroCartao()));

        Optional.ofNullable(card)
                .ifPresentOrElse(
                        c -> log.info("CardExistsRule.evaluate - passed - cardNumber: {}", mask(request.numeroCartao())),
                        () -> {
                            log.info("CardExistsRule.evaluate - failed: CARTAO_INEXISTENTE - cardNumber: {}", mask(request.numeroCartao()));
                            throw new AuthorizationException(TransactionError.CARTAO_INEXISTENTE);
                        }
                );
    }

    private static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}
