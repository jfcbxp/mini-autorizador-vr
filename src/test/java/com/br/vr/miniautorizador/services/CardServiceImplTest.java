package com.br.vr.miniautorizador.services;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.exceptions.CardAlreadyExistsException;
import com.br.vr.miniautorizador.exceptions.CardNotFoundException;
import com.br.vr.miniautorizador.records.requests.CreateCardRequest;
import com.br.vr.miniautorizador.repositories.CardRepository;
import com.br.vr.miniautorizador.services.impl.CardServiceImpl;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    private static final String CARD_NUMBER      = "6549873025634501";
    private static final String PASSWORD         = "1234";
    private static final String PASSWORD_HASH    = "$2a$10$hash";
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("500.00");

    @Mock CardRepository cardRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;
    private CardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CardServiceImpl(cardRepository, passwordEncoder, INITIAL_BALANCE);
    }

    private CreateCardRequest request() {
        return new CreateCardRequest(CARD_NUMBER, PASSWORD);
    }

    private Card existingCard() {
        return Card.builder().cardNumber(CARD_NUMBER).passwordHash(PASSWORD_HASH).balance(INITIAL_BALANCE).build();
    }

    @Test
    void createCard_newCard_returnsResponse() {
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);

        var response = service.createCard(request());

        assertThat(response.numeroCartao()).isEqualTo(CARD_NUMBER);
        assertThat(response.senha()).isEqualTo(PASSWORD);
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void createCard_duplicate_throwsCardAlreadyExists() {
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(existingCard()));

        ThrowingCallable call = () -> service.createCard(request());

        assertThatThrownBy(call)
                .isInstanceOf(CardAlreadyExistsException.class)
                .extracting(e -> ((CardAlreadyExistsException) e).getCard().numeroCartao())
                .isEqualTo(CARD_NUMBER);

        verify(cardRepository, never()).save(any());
    }

    @Test
    void getBalance_cardExists_returnsBalance() {
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.of(existingCard()));

        assertThat(service.getBalance(CARD_NUMBER)).isEqualByComparingTo(INITIAL_BALANCE);
    }

    @Test
    void getBalance_cardNotFound_throwsCardNotFound() {
        when(cardRepository.findById(CARD_NUMBER)).thenReturn(Optional.empty());

        ThrowingCallable call = () -> service.getBalance(CARD_NUMBER);

        assertThatThrownBy(call).isInstanceOf(CardNotFoundException.class);
    }
}
