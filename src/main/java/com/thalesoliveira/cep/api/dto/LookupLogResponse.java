package com.thalesoliveira.cep.api.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.thalesoliveira.cep.application.LookupLogEntry;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro do log exposto pela API. O campo response é o JSON gravado pelo próprio
 * serviço (não vem do usuário), por isso pode ser devolvido como valor bruto.
 */
public record LookupLogResponse(
        UUID eventId,
        String cep,
        Instant queriedAt,
        String source,
        boolean found,
        @JsonRawValue String response,
        Instant recordedAt) {

    public static LookupLogResponse from(LookupLogEntry entry) {
        return new LookupLogResponse(
                entry.eventId(),
                entry.cep(),
                entry.queriedAt(),
                entry.source(),
                entry.found(),
                entry.responseJson(),
                entry.recordedAt());
    }
}
