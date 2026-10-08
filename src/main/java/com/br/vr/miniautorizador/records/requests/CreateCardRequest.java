package com.br.vr.miniautorizador.records.requests;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for card creation (POST /cartoes).
 *
 * @param numeroCartao card number — must not be blank
 * @param senha        card PIN in plain text — must not be blank; stored as BCrypt hash
 */
public record CreateCardRequest(
        @NotBlank String numeroCartao,
        @NotBlank String senha
) {
}
