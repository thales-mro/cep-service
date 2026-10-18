package com.thalesoliveira.cep.application.port;

/** Porta de métricas de negócio, para não acoplar os casos de uso a uma biblioteca de monitoramento. */
public interface LookupMetrics {

    /** @param outcome priority_base, viacep, brasilapi, not_found ou provider_error */
    void lookupCompleted(String outcome);

    void lookupLogged();
}
