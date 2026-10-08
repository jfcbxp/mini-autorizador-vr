package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import com.br.vr.miniautorizador.repositories.CardRepository;
import com.br.vr.miniautorizador.rules.AuthorizationRule;
import com.br.vr.miniautorizador.services.impl.TransactionServiceImpl;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final String CARD_NUMBER  = "6549873025634501";
    private static final String PASSWORD     = "1234";
    private static final BigDecimal VALOR    = new BigDecimal("10.00");
    private static final BigDecimal BALANCE  = new BigDecimal("500.00");

    @Mock CardRepository cardRepository;
    @Mock AuthorizationRule rule;
    @InjectMocks TransactionServiceImpl service;

    private TransactionRequest request() {
        return new TransactionRequest(CARD_NUMBER, PASSWORD, VALOR);
    }

    private Card card() {
        return Card.builder().cardNumber(CARD_NUMBER).balance(BALANCE).version(0L).build();
    }

    @Test
    void authorize_allRulesPass_debitsAndSaves() {
        Card card = card();
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(card));
        doNothing().when(rule).evaluate(card, request());

        service = new TransactionServiceImpl(cardRepository, List.of(rule));
        service.authorize(request());

        assertThat(card.getBalance()).isEqualByComparingTo(BALANCE.subtract(VALOR));
        verify(cardRepository).save(card);
    }

    @Test
    void authorize_ruleFails_throwsAndDoesNotSave() {
        Card card = card();
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(card));
        service = new TransactionServiceImpl(cardRepository, List.of(rule));
        doThrow(new AuthorizationException(TransactionError.SALDO_INSUFICIENTE)).when(rule).evaluate(card, request());

        ThrowingCallable call = () -> service.authorize(request());

        assertThatThrownBy(call)
                .isInstanceOf(AuthorizationException.class)
                .extracting(e -> ((AuthorizationException) e).getError())
                .isEqualTo(TransactionError.SALDO_INSUFICIENTE);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void authorize_cardNotFound_doesNotEvaluateRules() {
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.empty());
        service = new TransactionServiceImpl(cardRepository, List.of(rule));

        ThrowingCallable call = () -> service.authorize(request());

        assertThatThrownBy(call)
                .isInstanceOf(AuthorizationException.class)
                .extracting(e -> ((AuthorizationException) e).getError())
                .isEqualTo(TransactionError.CARTAO_INEXISTENTE);

        verifyNoInteractions(rule);
        verify(cardRepository, never()).save(any());
    }
}
