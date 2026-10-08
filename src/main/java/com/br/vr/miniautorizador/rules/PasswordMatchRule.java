package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PasswordMatchRule implements AuthorizationRule {

    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void evaluate(Card card, TransactionRequest request) {
        log.info("PasswordMatchRule.evaluate - cardNumber: {}", mask(request.numeroCartao()));

        boolean matches = passwordEncoder.matches(request.senhaCartao(), card.getPasswordHash());

        Optional.of(matches)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                        m -> log.info("PasswordMatchRule.evaluate - passed - cardNumber: {}", mask(request.numeroCartao())),
                        () -> {
                            log.info("PasswordMatchRule.evaluate - failed: SENHA_INVALIDA - cardNumber: {}", mask(request.numeroCartao()));
                            throw new AuthorizationException(TransactionError.SENHA_INVALIDA);
                        }
                );
    }

    private static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}
