package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.enums.TransactionError;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import com.br.vr.miniautorizador.repositories.CardRepository;
import com.br.vr.miniautorizador.rules.AuthorizationRule;
import com.br.vr.miniautorizador.services.impl.TransactionServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionServiceRetryTest {

    private static final String CARD_NUMBER = "6549873025634501";

    @Test
    void authorize_retriesExhausted_returnsInsufficientBalance() {
        try (var context = new AnnotationConfigApplicationContext(RetryTestConfiguration.class)) {
            CardRepository cardRepository = context.getBean(CardRepository.class);
            TransactionService transactionService = context.getBean(TransactionService.class);
            Card card = Card.builder()
                    .cardNumber(CARD_NUMBER)
                    .passwordHash("hash")
                    .balance(new BigDecimal("500.00"))
                    .version(0L)
                    .build();
            when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(card));
            org.mockito.Mockito.doThrow(new ObjectOptimisticLockingFailureException(Card.class, CARD_NUMBER))
                    .when(cardRepository).save(any(Card.class));

            assertThatThrownBy(() -> transactionService.authorize(
                    new TransactionRequest(CARD_NUMBER, "1234", new BigDecimal("1.00"))))
                    .isInstanceOf(AuthorizationException.class)
                    .extracting(exception -> ((AuthorizationException) exception).getError())
                    .isEqualTo(TransactionError.SALDO_INSUFICIENTE);

            verify(cardRepository, times(3)).save(any(Card.class));
        }
    }

    @Test
    void authorize_authorizationFailurePreservesOriginalError() {
        try (var context = new AnnotationConfigApplicationContext(RetryTestConfiguration.class)) {
            CardRepository cardRepository = context.getBean(CardRepository.class);
            AuthorizationRule rule = context.getBean(AuthorizationRule.class);
            TransactionService transactionService = context.getBean(TransactionService.class);
            when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.authorize(
                    new TransactionRequest(CARD_NUMBER, "1234", new BigDecimal("1.00"))))
                    .isInstanceOf(AuthorizationException.class)
                    .extracting(exception -> ((AuthorizationException) exception).getError())
                    .isEqualTo(TransactionError.CARTAO_INEXISTENTE);

            verify(cardRepository).findById(CARD_NUMBER);
            verifyNoInteractions(rule);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRetry
    @Import(TransactionServiceImpl.class)
    static class RetryTestConfiguration {

        @Bean
        CardRepository cardRepository() {
            return mock(CardRepository.class);
        }

        @Bean
        AuthorizationRule authorizationRule() {
            return mock(AuthorizationRule.class);
        }
    }
}
