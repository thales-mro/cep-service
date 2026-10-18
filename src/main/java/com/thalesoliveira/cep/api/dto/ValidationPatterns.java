package com.thalesoliveira.cep.api.dto;

/** Expressões regulares de validação (allowlist) usadas nos DTOs de entrada. */
final class ValidationPatterns {

    /** 8 dígitos, com hífen opcional: 01001000 ou 01001-000. */
    static final String CEP = "^\\d{5}-?\\d{3}$";

    /** Letras (inclusive acentuadas), números, espaço e pontuação comum de endereço. */
    static final String TEXT = "^[\\p{L}\\p{N} .,'ºª°/()-]+$";

    /** Duas letras (a lista fechada de UFs é validada no domínio). */
    static final String STATE = "^[A-Za-z]{2}$";

    private ValidationPatterns() {
    }
}
