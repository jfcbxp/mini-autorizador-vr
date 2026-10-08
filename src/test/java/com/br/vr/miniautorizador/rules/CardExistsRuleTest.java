package com.br.vr.miniautorizador.rules;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardExistsRuleTest {

    private static final String CARD_NUMBER = "6549873025634501";
    private static final String PASSWORD    = "1234";
    private static final BigDecimal VALOR   = new BigDecimal("10.00");

    private final CardExistsRule rule = new CardExistsRule();

    private TransactionRequest request() {
        return new TransactionRequest(CARD_NUMBER, PASSWORD, VALOR);
    }

    @Test
    void evaluate_cardExists_doesNotThrow() {
        Card card = Card.builder().cardNumber(CARD_NUMBER).build();
        ThrowingCallable call = () -> rule.evaluate(card, request());
        assertThatCode(call).doesNotThrowAnyException();
    }

    @Test
    void evaluate_cardNull_throwsCartaoInexistente() {
        ThrowingCallable call = () -> rule.evaluate(null, request());
        assertThatThrownBy(call)
                .isInstanceOf(AuthorizationException.class)
                .extracting(e -> ((AuthorizationException) e).getError())
                .isEqualTo(TransactionError.CARTAO_INEXISTENTE);
    }
}
