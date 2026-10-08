package com.br.vr.miniautorizador.services.impl;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.exceptions.CardAlreadyExistsException;
import com.br.vr.miniautorizador.exceptions.CardNotFoundException;
import com.br.vr.miniautorizador.records.requests.CreateCardRequest;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import com.br.vr.miniautorizador.repositories.CardRepository;
import com.br.vr.miniautorizador.services.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    static final BigDecimal INITIAL_BALANCE = new BigDecimal("500.00");

    private final CardRepository cardRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public CreateCardResponse createCard(CreateCardRequest request) {
        log.info("CardServiceImpl.createCard - Start - cardNumber: {}", mask(request.numeroCartao()));

        cardRepository.findById(request.numeroCartao()).ifPresent(c -> {
            log.info("CardServiceImpl.createCard - Card already exists - cardNumber: {}", mask(request.numeroCartao()));
            throw new CardAlreadyExistsException(new CreateCardResponse(request.numeroCartao(), request.senha()));
        });

        cardRepository.save(Card.builder()
                .cardNumber(request.numeroCartao())
                .passwordHash(passwordEncoder.encode(request.senha()))
                .balance(INITIAL_BALANCE)
                .build());

        log.info("CardServiceImpl.createCard - Card created - cardNumber: {}, balance: {}", mask(request.numeroCartao()), INITIAL_BALANCE);

        return new CreateCardResponse(request.numeroCartao(), request.senha());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String cardNumber) {
        log.info("CardServiceImpl.getBalance - Start - cardNumber: {}", mask(cardNumber));

        BigDecimal balance = cardRepository.findById(cardNumber)
                .map(Card::getBalance)
                .orElseThrow(() -> {
                    log.info("CardServiceImpl.getBalance - Card not found - cardNumber: {}", mask(cardNumber));
                    return new CardNotFoundException(cardNumber);
                });

        log.info("CardServiceImpl.getBalance - End - cardNumber: {}, balance: {}", mask(cardNumber), balance);

        return balance;
    }

    private static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}
