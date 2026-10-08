package com.br.vr.miniautorizador.records.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateCardRequest(
        @NotBlank String numeroCartao,
        @NotBlank String senha
) {
}
