package com.br.vr.miniautorizador.services.impl;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.exceptions.CardAlreadyExistsException;
import com.br.vr.miniautorizador.exceptions.CardNotFoundException;
import com.br.vr.miniautorizador.records.requests.CreateCardRequest;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import com.br.vr.miniautorizador.repositories.CardRepository;
import com.br.vr.miniautorizador.services.CardService;
import com.br.vr.miniautorizador.utils.CardMask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final BigDecimal initialBalance;

    public CardServiceImpl(
            CardRepository cardRepository,
            BCryptPasswordEncoder passwordEncoder,
            @Value("${mini-autorizador.card.initial-balance:500.00}") BigDecimal initialBalance
    ) {
        this.cardRepository = cardRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialBalance = initialBalance;
    }

    @Override
    @Transactional
    public CreateCardResponse createCard(CreateCardRequest request) {
        log.info("CardServiceImpl.createCard - Start - cardNumber: {}", CardMask.mask(request.numeroCartao()));

        cardRepository.findById(request.numeroCartao()).ifPresent(c -> {
            log.info("CardServiceImpl.createCard - Card already exists - cardNumber: {}", CardMask.mask(request.numeroCartao()));
            throw new CardAlreadyExistsException(new CreateCardResponse(request.numeroCartao(), request.senha()));
        });

        cardRepository.save(Card.builder()
                .cardNumber(request.numeroCartao())
                .passwordHash(passwordEncoder.encode(request.senha()))
                .balance(initialBalance)
                .build());

        log.info("CardServiceImpl.createCard - Card created - cardNumber: {}, balance: {}", CardMask.mask(request.numeroCartao()), initialBalance);

        return new CreateCardResponse(request.numeroCartao(), request.senha());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(String cardNumber) {
        log.info("CardServiceImpl.getBalance - Start - cardNumber: {}", CardMask.mask(cardNumber));

        BigDecimal balance = cardRepository.findById(cardNumber)
                .map(Card::getBalance)
                .orElseThrow(() -> {
                    log.info("CardServiceImpl.getBalance - Card not found - cardNumber: {}", CardMask.mask(cardNumber));
                    return new CardNotFoundException(cardNumber);
                });

        log.info("CardServiceImpl.getBalance - End - cardNumber: {}, balance: {}", CardMask.mask(cardNumber), balance);

        return balance;
    }
}
