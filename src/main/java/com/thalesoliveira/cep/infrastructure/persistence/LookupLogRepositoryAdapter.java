package com.thalesoliveira.cep.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thalesoliveira.cep.application.LookupLogEntry;
import com.thalesoliveira.cep.application.port.LookupLogRepository;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import com.thalesoliveira.cep.domain.model.Cep;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/** Adaptador JPA do log de consultas. Os dados retornados pela API ficam em JSON na coluna response_json. */
@Component
class LookupLogRepositoryAdapter implements LookupLogRepository {

    private final LookupLogJpaRepository jpa;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    LookupLogRepositoryAdapter(LookupLogJpaRepository jpa, ObjectMapper objectMapper, Clock clock) {
        this.jpa = jpa;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpa.existsByEventId(eventId);
    }

    @Override
    public void save(CepLookupEvent event) {
        LookupLogEntity entity = new LookupLogEntity(
                event.eventId(),
                event.cep(),
                event.queriedAt(),
                event.source(),
                event.found(),
                toJson(event.response()),
                clock.instant());
        jpa.saveAndFlush(entity);
    }

    @Override
    public List<LookupLogEntry> findRecent(int limit) {
        return jpa.findAllByOrderByQueriedAtDesc(PageRequest.of(0, limit)).stream()
                .map(LookupLogRepositoryAdapter::toEntry)
                .toList();
    }

    @Override
    public List<LookupLogEntry> findRecentByCep(Cep cep, int limit) {
        return jpa.findByCepOrderByQueriedAtDesc(cep.value(), PageRequest.of(0, limit)).stream()
                .map(LookupLogRepositoryAdapter::toEntry)
                .toList();
    }

    private String toJson(CepLookupEvent.AddressPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar a resposta da consulta.", e);
        }
    }

    private static LookupLogEntry toEntry(LookupLogEntity entity) {
        return new LookupLogEntry(
                entity.getEventId(),
                entity.getCep(),
                entity.getQueriedAt(),
                entity.getSource(),
                entity.isFound(),
                entity.getResponseJson(),
                entity.getRecordedAt());
    }
}
