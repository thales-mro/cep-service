package com.thalesoliveira.cep.domain.model;

import com.thalesoliveira.cep.domain.exception.InvalidAddressException;

import java.util.Locale;

/** Unidades federativas válidas. Usado para validar a UF por lista fechada, e não por formato. */
public enum BrazilianState {
    AC, AL, AP, AM, BA, CE, DF, ES, GO, MA, MT, MS, MG, PA,
    PB, PR, PE, PI, RJ, RN, RS, RO, RR, SC, SP, SE, TO;

    public static BrazilianState parse(String raw) {
        if (raw == null) {
            throw new InvalidAddressException("UF é obrigatória.");
        }
        try {
            return valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidAddressException("UF inválida.");
        }
    }
}
