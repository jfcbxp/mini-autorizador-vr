package com.br.vr.miniautorizador.records.responses;

/**
 * Response payload for card creation (201 Created and 422 Unprocessable Entity).
 *
 * <p>The spec requires that both success and duplicate-card responses return the same
 * body shape: {@code {"numeroCartao": "...", "senha": "..."}}.
 * The {@code senha} returned is the <strong>raw</strong> value supplied in the request,
 * never the stored BCrypt hash.
 *
 * @param numeroCartao card number as provided by the caller
 * @param senha        card PIN as provided by the caller (plain text echo)
 */
public record CreateCardResponse(
        String numeroCartao,
        String senha
) {
}
