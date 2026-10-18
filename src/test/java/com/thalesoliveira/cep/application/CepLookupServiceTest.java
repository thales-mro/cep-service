package com.thalesoliveira.cep.application;

import com.thalesoliveira.cep.application.port.CepProvider;
import com.thalesoliveira.cep.application.port.LookupEventPublisher;
import com.thalesoliveira.cep.application.port.LookupMetrics;
import com.thalesoliveira.cep.application.port.PriorityAddressRepository;
import com.thalesoliveira.cep.domain.event.CepLookupEvent;
import com.thalesoliveira.cep.domain.exception.CepNotFoundException;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.exception.InvalidCepException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.thalesoliveira.cep.TestData.SE_CEP;
import static com.thalesoliveira.cep.TestData.pracaDaSe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CepLookupServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-19T12:00:00Z");

    private final PriorityAddressRepository priorityAddresses = mock(PriorityAddressRepository.class);
    private final CepProvider cepProvider = mock(CepProvider.class);
    private final LookupEventPublisher publisher = mock(LookupEventPublisher.class);
    private final LookupMetrics metrics = mock(LookupMetrics.class);
    private final CepLookupService service = new CepLookupService(
            priorityAddresses, cepProvider, publisher, metrics, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void returnsPriorityAddressWithoutCallingExternalProvider() {
        Address local = pracaDaSe(AddressSource.PRIORITY_BASE);
        when(priorityAddresses.findByCep(SE_CEP)).thenReturn(Optional.of(local));

        Address result = service.lookup("01001-000");

        assertThat(result).isEqualTo(local);
        verifyNoInteractions(cepProvider);
        assertThat(publishedEvent().source()).isEqualTo("PRIORITY_BASE");
    }

    @Test
    void fallsBackToExternalProviderWhenNotInPriorityBase() {
        Address external = pracaDaSe(AddressSource.VIACEP);
        when(priorityAddresses.findByCep(SE_CEP)).thenReturn(Optional.empty());
        when(cepProvider.lookup(SE_CEP)).thenReturn(Optional.of(external));

        Address result = service.lookup("01001000");

        assertThat(result.source()).isEqualTo(AddressSource.VIACEP);
        CepLookupEvent event = publishedEvent();
        assertThat(event.queriedAt()).isEqualTo(NOW);
        assertThat(event.found()).isTrue();
        assertThat(event.response().city()).isEqualTo("São Paulo");
        verify(metrics).lookupCompleted("viacep");
    }

    @Test
    void notFoundStillPublishesTheLookupEvent() {
        when(priorityAddresses.findByCep(SE_CEP)).thenReturn(Optional.empty());
        when(cepProvider.lookup(SE_CEP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookup("01001000")).isInstanceOf(CepNotFoundException.class);

        CepLookupEvent event = publishedEvent();
        assertThat(event.found()).isFalse();
        assertThat(event.source()).isEqualTo(CepLookupEvent.NOT_FOUND);
        assertThat(event.response()).isNull();
    }

    @Test
    void publishingFailureDoesNotBreakTheLookup() {
        when(priorityAddresses.findByCep(SE_CEP)).thenReturn(Optional.of(pracaDaSe(AddressSource.PRIORITY_BASE)));
        doThrow(new IllegalStateException("Kafka fora do ar")).when(publisher).publish(any());

        assertThat(service.lookup("01001000")).isNotNull();
    }

    @Test
    void providerFailureIsPropagatedAndCounted() {
        when(priorityAddresses.findByCep(SE_CEP)).thenReturn(Optional.empty());
        when(cepProvider.lookup(SE_CEP)).thenThrow(new ExternalServiceException("viacep", new RuntimeException()));

        assertThatThrownBy(() -> service.lookup("01001000")).isInstanceOf(ExternalServiceException.class);
        verify(metrics).lookupCompleted("provider_error");
        verify(publisher, never()).publish(any());
    }

    @Test
    void invalidCepIsRejectedBeforeTouchingAnyDependency() {
        assertThatThrownBy(() -> service.lookup("01001000' OR '1'='1")).isInstanceOf(InvalidCepException.class);
        verifyNoInteractions(priorityAddresses, cepProvider, publisher);
    }

    private CepLookupEvent publishedEvent() {
        ArgumentCaptor<CepLookupEvent> captor = ArgumentCaptor.forClass(CepLookupEvent.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }
}
