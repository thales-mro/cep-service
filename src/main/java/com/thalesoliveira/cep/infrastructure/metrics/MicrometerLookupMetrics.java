package com.thalesoliveira.cep.infrastructure.metrics;

import com.thalesoliveira.cep.application.port.LookupMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** Métricas de negócio expostas em /actuator/prometheus e exibidas no Grafana. */
@Component
class MicrometerLookupMetrics implements LookupMetrics {

    private final MeterRegistry registry;
    private final Counter logsSaved;

    MicrometerLookupMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.logsSaved = Counter.builder("cep.lookup.logs.saved")
                .description("Logs de consulta gravados pelo consumidor")
                .register(registry);
    }

    @Override
    public void lookupCompleted(String outcome) {
        Counter.builder("cep.lookups")
                .description("Consultas de CEP por resultado")
                .tag("outcome", outcome)
                .register(registry)
                .increment();
    }

    @Override
    public void lookupLogged() {
        logsSaved.increment();
    }
}
