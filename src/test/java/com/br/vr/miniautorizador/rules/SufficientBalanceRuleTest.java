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

class SufficientBalanceRuleTest {

    private static final String CARD_NUMBER      = "6549873025634501";
    private static final String PASSWORD         = "1234";
    private static final BigDecimal BALANCE      = new BigDecimal("100.00");
    private static final BigDecimal VALOR_OK     = new BigDecimal("100.00");
    private static final BigDecimal VALOR_EXCEDE = new BigDecimal("100.01");

    private final SufficientBalanceRule rule = new SufficientBalanceRule();

    private Card cardWith(BigDecimal balance) {
        return Card.builder().cardNumber(CARD_NUMBER).balance(balance).build();
    }

    @Test
    void evaluate_balanceEqual_doesNotThrow() {
        ThrowingCallable call = () -> rule.evaluate(cardWith(BALANCE), new TransactionRequest(CARD_NUMBER, PASSWORD, VALOR_OK));
        assertThatCode(call).doesNotThrowAnyException();
    }

    @Test
    void evaluate_balanceGreater_doesNotThrow() {
        ThrowingCallable call = () -> rule.evaluate(cardWith(new BigDecimal("200.00")), new TransactionRequest(CARD_NUMBER, PASSWORD, VALOR_OK));
        assertThatCode(call).doesNotThrowAnyException();
    }

    @Test
    void evaluate_balanceInsufficient_throwsSaldoInsuficiente() {
        ThrowingCallable call = () -> rule.evaluate(cardWith(BALANCE), new TransactionRequest(CARD_NUMBER, PASSWORD, VALOR_EXCEDE));
        assertThatThrownBy(call)
                .isInstanceOf(AuthorizationException.class)
                .extracting(e -> ((AuthorizationException) e).getError())
                .isEqualTo(TransactionError.SALDO_INSUFICIENTE);
    }
}
