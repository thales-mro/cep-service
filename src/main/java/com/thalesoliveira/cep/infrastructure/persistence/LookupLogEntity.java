package com.thalesoliveira.cep.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cep_lookup_log")
public class LookupLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false, length = 8)
    private String cep;

    @Column(name = "queried_at", nullable = false)
    private Instant queriedAt;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false)
    private boolean found;

    @Column(name = "response_json", columnDefinition = "text")
    private String responseJson;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected LookupLogEntity() {
        // exigido pelo JPA
    }

    public LookupLogEntity(UUID eventId, String cep, Instant queriedAt, String source, boolean found,
                           String responseJson, Instant recordedAt) {
        this.eventId = eventId;
        this.cep = cep;
        this.queriedAt = queriedAt;
        this.source = source;
        this.found = found;
        this.responseJson = responseJson;
        this.recordedAt = recordedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getCep() {
        return cep;
    }

    public Instant getQueriedAt() {
        return queriedAt;
    }

    public String getSource() {
        return source;
    }

    public boolean isFound() {
        return found;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
