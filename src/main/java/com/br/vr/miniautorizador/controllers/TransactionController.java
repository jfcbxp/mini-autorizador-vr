package com.br.vr.miniautorizador.controllers;

import com.br.vr.miniautorizador.records.requests.TransactionRequest;
import com.br.vr.miniautorizador.services.TransactionService;
import jakarta.validation.Valid;
import com.br.vr.miniautorizador.utils.CardMask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/transacoes")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<String> authorize(@Valid @RequestBody TransactionRequest request) {
        log.info("TransactionController.authorize - Start - cardNumber: {}, amount: {}", CardMask.mask(request.numeroCartao()), request.valor());

        transactionService.authorize(request);

        log.info("TransactionController.authorize - End - cardNumber: {} - authorized", CardMask.mask(request.numeroCartao()));

        return ResponseEntity.status(201).body("OK");
    }
}
