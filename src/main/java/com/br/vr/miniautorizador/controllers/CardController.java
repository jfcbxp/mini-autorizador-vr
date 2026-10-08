package com.br.vr.miniautorizador.controllers;

import com.br.vr.miniautorizador.records.requests.CreateCardRequest;
import com.br.vr.miniautorizador.records.responses.CreateCardResponse;
import com.br.vr.miniautorizador.services.CardService;
import jakarta.validation.Valid;
import com.br.vr.miniautorizador.utils.CardMask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/cartoes")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @PostMapping
    public ResponseEntity<CreateCardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        log.info("CardController.createCard - Start - cardNumber: {}", CardMask.mask(request.numeroCartao()));

        CreateCardResponse response = cardService.createCard(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{cardNumber}")
                .buildAndExpand(response.numeroCartao())
                .toUri();

        log.info("CardController.createCard - End - cardNumber: {}, location: {}", CardMask.mask(response.numeroCartao()), location);

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{numeroCartao}")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String numeroCartao) {
        log.info("CardController.getBalance - Start - cardNumber: {}", CardMask.mask(numeroCartao));

        BigDecimal balance = cardService.getBalance(numeroCartao);

        log.info("CardController.getBalance - End - cardNumber: {}, balance: {}", CardMask.mask(numeroCartao), balance);

        return ResponseEntity.ok(balance);
    }

}
