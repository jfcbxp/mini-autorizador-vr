package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordMatchRuleTest {

    private static final String CARD_NUMBER    = "6549873025634501";
    private static final String CORRECT_PASSWORD = "1234";
    private static final String WRONG_PASSWORD   = "9999";
    private static final BigDecimal VALOR        = new BigDecimal("10.00");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final PasswordMatchRule rule = new PasswordMatchRule(encoder);
    private final Card card = Card.builder()
            .cardNumber(CARD_NUMBER)
            .passwordHash(encoder.encode(CORRECT_PASSWORD))
            .build();

    @Test
    void evaluate_correctPassword_doesNotThrow() {
        ThrowingCallable call = () -> rule.evaluate(card, new TransactionRequest(CARD_NUMBER, CORRECT_PASSWORD, VALOR));
        assertThatCode(call).doesNotThrowAnyException();
    }

    @Test
    void evaluate_wrongPassword_throwsSenhaInvalida() {
        ThrowingCallable call = () -> rule.evaluate(card, new TransactionRequest(CARD_NUMBER, WRONG_PASSWORD, VALOR));
        assertThatThrownBy(call)
                .isInstanceOf(AuthorizationException.class)
                .extracting(e -> ((AuthorizationException) e).getError())
                .isEqualTo(TransactionError.SENHA_INVALIDA);
    }
}
