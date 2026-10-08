package com.br.vr.miniautorizador.records.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String numeroCartao,
        @NotBlank String senhaCartao,
        @NotNull @DecimalMin(value = "0.01", message = "valor must be greater than zero") BigDecimal valor
) {
}
