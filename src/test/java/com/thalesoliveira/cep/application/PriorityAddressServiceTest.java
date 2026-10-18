package com.thalesoliveira.cep.application;

import com.thalesoliveira.cep.application.port.PriorityAddressRepository;
import com.thalesoliveira.cep.domain.exception.AddressAlreadyExistsException;
import com.thalesoliveira.cep.domain.exception.CepNotFoundException;
import com.thalesoliveira.cep.domain.exception.InvalidAddressException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import org.junit.jupiter.api.Test;

import static com.thalesoliveira.cep.TestData.SE_CEP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PriorityAddressServiceTest {

    private final PriorityAddressRepository repository = mock(PriorityAddressRepository.class);
    private final PriorityAddressService service = new PriorityAddressService(repository);

    @Test
    void registersSanitizedAddress() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Address saved = service.register(new NewAddressCommand(
                "01001-000", "  Praça   da Sé ", null, "Sé", "São\tPaulo", "sp"));

        assertThat(saved.cep()).isEqualTo(SE_CEP);
        assertThat(saved.street()).isEqualTo("Praça da Sé");
        assertThat(saved.city()).isEqualTo("São Paulo");
        assertThat(saved.state()).isEqualTo("SP");
        assertThat(saved.source()).isEqualTo(AddressSource.PRIORITY_BASE);
    }

    @Test
    void keepsApostropheAsLiteralText() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Address saved = service.register(new NewAddressCommand(
                "01001000", "Rua D'Ávila", null, "Centro", "São Paulo", "SP"));

        assertThat(saved.street()).isEqualTo("Rua D'Ávila");
    }

    @Test
    void rejectsDuplicateCep() {
        when(repository.existsByCep(SE_CEP)).thenReturn(true);

        assertThatThrownBy(() -> service.register(new NewAddressCommand(
                "01001000", "Praça da Sé", null, "Sé", "São Paulo", "SP")))
                .isInstanceOf(AddressAlreadyExistsException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsUnknownState() {
        assertThatThrownBy(() -> service.register(new NewAddressCommand(
                "01001000", "Praça da Sé", null, "Sé", "São Paulo", "XX")))
                .isInstanceOf(InvalidAddressException.class);
    }

    @Test
    void removingUnknownCepFails() {
        when(repository.deleteByCep(SE_CEP)).thenReturn(false);

        assertThatThrownBy(() -> service.remove("01001000")).isInstanceOf(CepNotFoundException.class);
    }
}
