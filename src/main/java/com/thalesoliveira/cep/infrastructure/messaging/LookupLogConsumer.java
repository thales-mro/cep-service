package com.thalesoliveira.cep.infrastructure.messaging;

import com.thalesoliveira.cep.application.LookupLogService;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consome os eventos de consulta e grava o log no banco, fora do caminho da requisição. */
@Component
public class LookupLogConsumer {

    private final LookupLogService lookupLogService;

    public LookupLogConsumer(LookupLogService lookupLogService) {
        this.lookupLogService = lookupLogService;
    }

    @KafkaListener(topics = "${app.kafka.topic}")
    public void onLookup(CepLookupEvent event) {
        lookupLogService.record(event);
    }
}
