package com.thalesoliveira.cep.infrastructure.provider;

import com.thalesoliveira.cep.application.port.CepProvider;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.model.AddressSource;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.thalesoliveira.cep.TestData.SE_CEP;
import static com.thalesoliveira.cep.TestData.pracaDaSe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FallbackCepProviderTest {

    private final CepProvider primary = mock(CepProvider.class);
    private final CepProvider secondary = mock(CepProvider.class);
    private final FallbackCepProvider fallback = new FallbackCepProvider(List.of(primary, secondary));

    @Test
    void usesPrimaryWhenItFindsTheCep() {
        when(primary.lookup(SE_CEP)).thenReturn(Optional.of(pracaDaSe(AddressSource.VIACEP)));

        assertThat(fallback.lookup(SE_CEP)).isPresent();
        verifyNoInteractions(secondary);
    }

    @Test
    void usesSecondaryWhenPrimaryIsDown() {
        when(primary.lookup(SE_CEP)).thenThrow(new ExternalServiceException("viacep", new RuntimeException()));
        when(secondary.lookup(SE_CEP)).thenReturn(Optional.of(pracaDaSe(AddressSource.BRASILAPI)));

        assertThat(fallback.lookup(SE_CEP)).hasValueSatisfying(
                address -> assertThat(address.source()).isEqualTo(AddressSource.BRASILAPI));
    }

    @Test
    void returnsEmptyWhenProvidersAnswerNotFound() {
        when(primary.lookup(SE_CEP)).thenReturn(Optional.empty());
        when(secondary.lookup(SE_CEP)).thenThrow(new ExternalServiceException("brasilapi", new RuntimeException()));

        assertThat(fallback.lookup(SE_CEP)).isEmpty();
    }

    @Test
    void failsWhenAllProvidersAreDown() {
        when(primary.lookup(SE_CEP)).thenThrow(new ExternalServiceException("viacep", new RuntimeException()));
        when(secondary.lookup(SE_CEP)).thenThrow(new ExternalServiceException("brasilapi", new RuntimeException()));

        assertThatThrownBy(() -> fallback.lookup(SE_CEP)).isInstanceOf(ExternalServiceException.class);
    }
}
