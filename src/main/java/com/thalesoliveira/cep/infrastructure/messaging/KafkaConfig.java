package com.thalesoliveira.cep.infrastructure.messaging;

import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
class KafkaConfig {

    private static final String DLT_SUFFIX = "-dlt";

    @Bean
    NewTopic cepLookupsTopic(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic cepLookupsDeadLetterTopic(@Value("${app.kafka.topic}") String topic) {
        return TopicBuilder.name(topic + DLT_SUFFIX).partitions(3).replicas(1).build();
    }

    /**
     * Se a gravação do log falhar (ex.: banco fora do ar), tenta 3 vezes com intervalo de 1s.
     * Persistindo a falha, a mensagem vai para o tópico de dead letter, sem travar a fila
     * e sem perder o evento: ele pode ser reprocessado depois.
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, CepLookupEvent> template,
                                         @Value("${app.kafka.topic}") String topic) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(topic + DLT_SUFFIX, record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
    }
}
