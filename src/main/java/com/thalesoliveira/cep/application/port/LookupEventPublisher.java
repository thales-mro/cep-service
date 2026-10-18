package com.thalesoliveira.cep.application.port;

import com.thalesoliveira.cep.domain.event.CepLookupEvent;

/** Porta de saída para emitir o evento de consulta de forma assíncrona. */
public interface LookupEventPublisher {

    void publish(CepLookupEvent event);
}
