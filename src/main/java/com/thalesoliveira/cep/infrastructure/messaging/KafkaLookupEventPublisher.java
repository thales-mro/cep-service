package com.thalesoliveira.cep.infrastructure.messaging;

import com.thalesoliveira.cep.application.port.LookupEventPublisher;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publica o evento de consulta no Kafka sem esperar a confirmação do broker:
 * o send() devolve um CompletableFuture e o resultado é tratado em callback.
 * A chave é o CEP, então eventos do mesmo CEP caem na mesma partição (ordem preservada).
 */
@Component
class KafkaLookupEventPublisher implements LookupEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaLookupEventPublisher.class);

    private final KafkaTemplate<String, CepLookupEvent> kafkaTemplate;
    private final String topic;
    private final Counter publishFailures;

    KafkaLookupEventPublisher(KafkaTemplate<String, CepLookupEvent> kafkaTemplate,
                              @Value("${app.kafka.topic}") String topic,
                              MeterRegistry registry) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishFailures = Counter.builder("cep.lookup.events.publish.failures")
                .description("Eventos de consulta que não chegaram ao Kafka")
                .register(registry);
    }

    @Override
    public void publish(CepLookupEvent event) {
        kafkaTemplate.send(topic, event.cep(), event).whenComplete((result, exception) -> {
            if (exception != null) {
                publishFailures.increment();
                log.error("Evento {} não publicado no Kafka: {}", event.eventId(), exception.getMessage());
            }
        });
    }
}
