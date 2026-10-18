package com.thalesoliveira.cep.domain.model;

import com.thalesoliveira.cep.domain.exception.InvalidCepException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CepTest {

    @ParameterizedTest
    @ValueSource(strings = {"01001000", "01001-000", " 01001-000 ", "01.001-000"})
    void normalizesAcceptedFormatsToEightDigits(String raw) {
        assertThat(Cep.of(raw).value()).isEqualTo("01001000");
    }

    @Test
    void formatsWithHyphen() {
        assertThat(Cep.of("01001000").formatted()).isEqualTo("01001-000");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "0100100",                       // 7 dígitos
            "010010000",                     // 9 dígitos
            "abcde-fgh",
            "01001000' OR '1'='1",           // tentativa de SQL injection
            "01001000; DROP TABLE x",
            "０１００１０００"                  // dígitos Unicode de largura total
    })
    void rejectsInvalidInput(String raw) {
        assertThatThrownBy(() -> Cep.of(raw)).isInstanceOf(InvalidCepException.class);
    }

    @Test
    void errorMessageNeverEchoesTheInput() {
        assertThatThrownBy(() -> Cep.of("<script>"))
                .isInstanceOf(InvalidCepException.class)
                .hasMessageNotContaining("<script>");
    }
}
