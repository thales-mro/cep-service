package com.thalesoliveira.cep.application;

import java.time.Instant;
import java.util.UUID;

/** Registro do log de consultas, como lido do banco. */
public record LookupLogEntry(
        UUID eventId,
        String cep,
        Instant queriedAt,
        String source,
        boolean found,
        String responseJson,
        Instant recordedAt) {
}
