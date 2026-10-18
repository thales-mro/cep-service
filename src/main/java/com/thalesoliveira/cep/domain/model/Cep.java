package com.thalesoliveira.cep.domain.model;

import com.thalesoliveira.cep.domain.exception.InvalidCepException;

import java.util.regex.Pattern;

/**
 * Value object do CEP. Só existe em estado válido: exatamente 8 dígitos ASCII.
 * Qualquer outro caractere (aspas, ponto e vírgula, letras) é rejeitado na criação,
 * o que elimina entradas maliciosas antes de chegarem a qualquer camada de dados.
 */
public record Cep(String value) {

    private static final Pattern EIGHT_DIGITS = Pattern.compile("\\d{8}");
    private static final Pattern ALLOWED_SEPARATORS = Pattern.compile("[\\s.-]");
    private static final int MAX_RAW_LENGTH = 12;

    public Cep {
        if (value == null || !EIGHT_DIGITS.matcher(value).matches()) {
            throw new InvalidCepException();
        }
    }

    /** Aceita "01001-000", "01001000" ou "01.001-000" e normaliza para 8 dígitos. */
    public static Cep of(String raw) {
        if (raw == null || raw.length() > MAX_RAW_LENGTH) {
            throw new InvalidCepException();
        }
        return new Cep(ALLOWED_SEPARATORS.matcher(raw.strip()).replaceAll(""));
    }

    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }

    @Override
    public String toString() {
        return value;
    }
}
