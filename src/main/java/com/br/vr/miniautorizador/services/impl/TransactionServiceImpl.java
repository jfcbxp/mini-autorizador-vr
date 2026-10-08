package com.br.vr.miniautorizador.services.impl;

import com.br.vr.miniautorizador.domains.Card;
import com.br.vr.miniautorizador.exceptions.AuthorizationException;
import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import com.br.vr.miniautorizador.repositories.CardRepository;
import com.br.vr.miniautorizador.rules.AuthorizationRule;
import com.br.vr.miniautorizador.services.TransactionService;
import com.br.vr.miniautorizador.utils.CardMask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final CardRepository cardRepository;
    private final List<AuthorizationRule> rules;

    @Override
    @Transactional
    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            noRetryFor = AuthorizationException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 50)
    )
    public void authorize(TransactionRequest request) {
        log.info("TransactionServiceImpl.authorize - Start - cardNumber: {}, amount: {}", CardMask.mask(request.numeroCartao()), request.valor());

        Card card = cardRepository.findById(request.numeroCartao()).orElse(null);

        rules.forEach(rule -> rule.evaluate(card, request));

        card.setBalance(card.getBalance().subtract(request.valor()));
        cardRepository.save(card);

        log.info("TransactionServiceImpl.authorize - Authorized - cardNumber: {}, amount: {}, newBalance: {}",
                CardMask.mask(request.numeroCartao()), request.valor(), card.getBalance());
    }
}
