package com.br.vr.miniautorizador.utils;

public final class CardMask {

    private CardMask() {}

    public static String mask(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4);
    }
}
