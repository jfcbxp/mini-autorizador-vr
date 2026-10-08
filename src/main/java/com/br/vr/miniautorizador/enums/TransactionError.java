package com.br.vr.miniautorizador.enums;

/**
 * Authorization rule failure codes returned as the HTTP 422 response body.
 *
 * <p>Evaluation precedence (enforced by {@code @Order} on each rule bean):
 * <ol>
 *   <li>{@link #CARD_NOT_FOUND} — card does not exist</li>
 *   <li>{@link #INVALID_PASSWORD} — PIN does not match</li>
 *   <li>{@link #INSUFFICIENT_BALANCE} — balance is less than the requested amount</li>
 * </ol>
 *
 * {@link #INVALID_AMOUNT} is triggered before rule evaluation, at Bean Validation time.
 */
public enum TransactionError {

    /** The card number supplied in the request does not exist in the database. */
    CARTAO_INEXISTENTE,

    /** The PIN supplied does not match the stored BCrypt hash. */
    SENHA_INVALIDA,

    /** The card's available balance is less than the requested transaction amount. */
    SALDO_INSUFICIENTE,

    /** The transaction amount is {@code <= 0}; rejected before rule evaluation. */
    INVALID_AMOUNT
}
