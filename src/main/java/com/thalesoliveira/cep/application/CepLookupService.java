package com.thalesoliveira.cep.application;

import com.thalesoliveira.cep.application.port.CepProvider;
import com.thalesoliveira.cep.application.port.LookupEventPublisher;
import com.thalesoliveira.cep.application.port.LookupMetrics;
import com.thalesoliveira.cep.application.port.PriorityAddressRepository;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import com.thalesoliveira.cep.domain.exception.CepNotFoundException;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

/**
 * Caso de uso de busca de CEP.
 * 1. Procura na base prioritária; 2. se não achar, consulta o provedor externo;
 * 3. emite o evento de log de forma assíncrona, sem bloquear a resposta.
 */
@Service
public class CepLookupService {

    private static final Logger log = LoggerFactory.getLogger(CepLookupService.class);

    private final PriorityAddressRepository priorityAddresses;
    private final CepProvider cepProvider;
    private final LookupEventPublisher eventPublisher;
    private final LookupMetrics metrics;
    private final Clock clock;

    public CepLookupService(PriorityAddressRepository priorityAddresses,
                            CepProvider cepProvider,
                            LookupEventPublisher eventPublisher,
                            LookupMetrics metrics,
                            Clock clock) {
        this.priorityAddresses = priorityAddresses;
        this.cepProvider = cepProvider;
        this.eventPublisher = eventPublisher;
        this.metrics = metrics;
        this.clock = clock;
    }

    public Address lookup(String rawCep) {
        Cep cep = Cep.of(rawCep);
        Instant queriedAt = clock.instant();

        Optional<Address> result;
        try {
            result = priorityAddresses.findByCep(cep).or(() -> cepProvider.lookup(cep));
        } catch (ExternalServiceException e) {
            metrics.lookupCompleted("provider_error");
            throw e;
        }

        metrics.lookupCompleted(result
                .map(address -> address.source().name().toLowerCase(Locale.ROOT))
                .orElse("not_found"));
        publishSafely(CepLookupEvent.of(cep, queriedAt, result));

        return result.orElseThrow(() -> new CepNotFoundException(cep));
    }

    /** O log nunca derruba a consulta: se a publicação falhar, a resposta segue e a falha é registrada. */
    private void publishSafely(CepLookupEvent event) {
        try {
            eventPublisher.publish(event);
        } catch (RuntimeException e) {
            log.warn("Falha ao publicar o evento de consulta {}: {}", event.eventId(), e.getMessage());
        }
    }
}
