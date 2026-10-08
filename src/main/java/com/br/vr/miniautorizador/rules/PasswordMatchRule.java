package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import com.br.vr.miniautorizador.utils.CardMask;
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
        log.info("PasswordMatchRule.evaluate - cardNumber: {}", CardMask.mask(request.numeroCartao()));

        boolean matches = passwordEncoder.matches(request.senhaCartao(), card.getPasswordHash());

        Optional.of(matches)
                .filter(Boolean::booleanValue)
                .ifPresentOrElse(
                        m -> log.info("PasswordMatchRule.evaluate - passed - cardNumber: {}", CardMask.mask(request.numeroCartao())),
                        () -> {
                            log.info("PasswordMatchRule.evaluate - failed: SENHA_INVALIDA - cardNumber: {}", CardMask.mask(request.numeroCartao()));
                            throw new AuthorizationException(TransactionError.SENHA_INVALIDA);
                        }
                );
    }
}
