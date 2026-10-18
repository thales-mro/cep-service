package com.thalesoliveira.cep.application;

import com.thalesoliveira.cep.application.port.LookupLogRepository;
import com.thalesoliveira.cep.application.port.LookupMetrics;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import com.thalesoliveira.cep.domain.model.Cep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Grava o log das consultas a partir dos eventos do Kafka.
 * Idempotente: o Kafka entrega "pelo menos uma vez", então o mesmo evento pode chegar
 * repetido. O eventId (único no banco) garante que cada consulta seja gravada uma só vez.
 */
@Service
public class LookupLogService {

    private static final Logger log = LoggerFactory.getLogger(LookupLogService.class);
    private static final int DEFAULT_LIMIT = 50;

    private final LookupLogRepository repository;
    private final LookupMetrics metrics;

    public LookupLogService(LookupLogRepository repository, LookupMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    public void record(CepLookupEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            log.debug("Evento {} já gravado; ignorando a reentrega.", event.eventId());
            return;
        }
        try {
            repository.save(event);
            metrics.lookupLogged();
        } catch (DataIntegrityViolationException e) {
            // Duas entregas do mesmo evento processadas ao mesmo tempo: a restrição única protege.
            log.debug("Evento {} gravado por outra entrega concorrente.", event.eventId());
        }
    }

    public List<LookupLogEntry> recent(String rawCep) {
        if (rawCep == null || rawCep.isBlank()) {
            return repository.findRecent(DEFAULT_LIMIT);
        }
        return repository.findRecentByCep(Cep.of(rawCep), DEFAULT_LIMIT);
    }
}
