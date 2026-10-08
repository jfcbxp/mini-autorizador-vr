package com.br.vr.miniautorizador.utils;

import java.util.Optional;

public final class CardMask {

    private CardMask() {}

    public static String mask(String cardNumber) {
        return Optional.ofNullable(cardNumber)
                .filter(number -> number.length() >= 4)
                .map(number -> "*".repeat(number.length() - 4) + number.substring(number.length() - 4))
                .orElse("****");
    }
}
