package com.thalesoliveira.cep.domain.event;

import com.thalesoliveira.cep.TestData;
import com.thalesoliveira.cep.domain.model.AddressSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** O banco recusa found, source e response incoerentes (ck_cep_lookup_log_found); o evento nunca os gera. */
class CepLookupEventTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    @Test
    void foundEventHasSourceAndResponse() {
        CepLookupEvent event = CepLookupEvent.of(TestData.SE_CEP, NOW,
                Optional.of(TestData.pracaDaSe(AddressSource.VIACEP)));

        assertThat(event.found()).isTrue();
        assertThat(event.source()).isEqualTo("VIACEP");
        assertThat(event.response()).isNotNull();
    }

    @Test
    void notFoundEventHasNotFoundSourceAndNoResponse() {
        CepLookupEvent event = CepLookupEvent.of(TestData.SE_CEP, NOW, Optional.empty());

        assertThat(event.found()).isFalse();
        assertThat(event.source()).isEqualTo(CepLookupEvent.NOT_FOUND);
        assertThat(event.response()).isNull();
    }
}
