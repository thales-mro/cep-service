package com.thalesoliveira.cep.application;

import com.thalesoliveira.cep.application.port.LookupLogRepository;
import com.thalesoliveira.cep.application.port.LookupMetrics;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import com.thalesoliveira.cep.domain.model.AddressSource;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;

import static com.thalesoliveira.cep.TestData.SE_CEP;
import static com.thalesoliveira.cep.TestData.pracaDaSe;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LookupLogServiceTest {

    private final LookupLogRepository repository = mock(LookupLogRepository.class);
    private final LookupMetrics metrics = mock(LookupMetrics.class);
    private final LookupLogService service = new LookupLogService(repository, metrics);

    private final CepLookupEvent event = CepLookupEvent.of(
            SE_CEP, Instant.parse("2026-10-19T12:00:00Z"), Optional.of(pracaDaSe(AddressSource.VIACEP)));

    @Test
    void savesNewEvent() {
        service.record(event);

        verify(repository).save(event);
        verify(metrics).lookupLogged();
    }

    @Test
    void ignoresRedeliveredEvent() {
        when(repository.existsByEventId(event.eventId())).thenReturn(true);

        service.record(event);

        verify(repository, never()).save(any());
    }

    @Test
    void concurrentDuplicateIsIgnored() {
        doThrow(new DataIntegrityViolationException("duplicate key")).when(repository).save(event);

        assertThatCode(() -> service.record(event)).doesNotThrowAnyException();
        verify(metrics, never()).lookupLogged();
    }
}
