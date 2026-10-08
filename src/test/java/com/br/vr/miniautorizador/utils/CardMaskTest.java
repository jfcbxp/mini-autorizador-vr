package com.br.vr.miniautorizador.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardMaskTest {

    @Test
    void mask_cardNumber_masksAllButLastFourDigits() {
        assertThat(CardMask.mask("6549873025634501")).isEqualTo("************4501");
    }

    @Test
    void mask_shortCardNumber_returnsFallback() {
        assertThat(CardMask.mask("123")).isEqualTo("****");
    }

    @Test
    void mask_nullCardNumber_returnsFallback() {
        assertThat(CardMask.mask(null)).isEqualTo("****");
    }
}
