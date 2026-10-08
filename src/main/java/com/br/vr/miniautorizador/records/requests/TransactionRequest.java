package com.br.vr.miniautorizador.records.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request payload for transaction authorization (POST /transacoes).
 *
 * @param numeroCartao  card number — must not be blank
 * @param senhaCartao   card PIN in plain text — must not be blank
 * @param valor         transaction amount — must be &gt; 0.00 (validated by {@code @DecimalMin})
 */
public record TransactionRequest(
        @NotBlank String numeroCartao,
        @NotBlank String senhaCartao,
        @NotNull @DecimalMin(value = "0.01", message = "valor must be greater than zero") BigDecimal valor
) {
}
