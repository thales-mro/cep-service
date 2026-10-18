package com.thalesoliveira.cep.application;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Higienização de texto livre antes de persistir: normaliza Unicode, remove caracteres
 * de controle e espaços repetidos.
 *
 * Observação importante: a proteção contra SQL Injection NÃO depende deste sanitizador.
 * Ela vem das consultas parametrizadas (JPA/Spring Data), que tratam toda entrada como dado.
 * O sanitizador melhora a qualidade do dado e reduz superfície para outros ataques.
 */
public final class TextSanitizer {

    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern REPEATED_WHITESPACE = Pattern.compile("\\s+");

    private TextSanitizer() {
    }

    /** @return o texto limpo, ou null se a entrada for nula ou ficar vazia */
    public static String clean(String input) {
        if (input == null) {
            return null;
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
        String withoutControl = CONTROL_CHARS.matcher(normalized).replaceAll(" ");
        String collapsed = REPEATED_WHITESPACE.matcher(withoutControl).replaceAll(" ").strip();
        return collapsed.isEmpty() ? null : collapsed;
    }
}
