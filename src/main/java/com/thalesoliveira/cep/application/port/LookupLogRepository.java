package com.thalesoliveira.cep.application.port;

import com.thalesoliveira.cep.application.LookupLogEntry;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import com.thalesoliveira.cep.domain.model.Cep;

import java.util.List;
import java.util.UUID;

/** Porta da tabela de log das consultas. */
public interface LookupLogRepository {

    boolean existsByEventId(UUID eventId);

    void save(CepLookupEvent event);

    List<LookupLogEntry> findRecent(int limit);

    List<LookupLogEntry> findRecentByCep(Cep cep, int limit);
}
