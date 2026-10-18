package com.thalesoliveira.cep.api;

import com.thalesoliveira.cep.application.CepLookupService;
import com.thalesoliveira.cep.application.NewAddressCommand;
import com.thalesoliveira.cep.application.PriorityAddressService;
import com.thalesoliveira.cep.domain.exception.InvalidCepException;
import com.thalesoliveira.cep.domain.model.AddressSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.thalesoliveira.cep.TestData.pracaDaSe;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante que entradas maliciosas são barradas e que erros não vazam detalhes internos. */
@WebMvcTest(controllers = {CepController.class, AddressController.class})
class ApiSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CepLookupService cepLookupService;

    @MockBean
    private PriorityAddressService priorityAddressService;

    @Test
    void sqlInjectionInCepIsRejectedWithoutReachingTheService() throws Exception {
        String body = """
                {"cep": "01001000'; DROP TABLE priority_address;--", "street": "Rua A",
                 "neighborhood": "Centro", "city": "São Paulo", "state": "SP"}
                """;

        mvc.perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cep"))
                .andExpect(content().string(not(containsString("DROP"))));

        verify(priorityAddressService, never()).register(any());
    }

    @Test
    void apostropheIsAcceptedAndPassedAsLiteralData() throws Exception {
        when(priorityAddressService.register(any())).thenReturn(pracaDaSe(AddressSource.PRIORITY_BASE));
        String body = """
                {"cep": "01001-000", "street": "Rua D'Ávila", "neighborhood": "Centro",
                 "city": "São Paulo", "state": "SP"}
                """;

        mvc.perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<NewAddressCommand> captor = ArgumentCaptor.forClass(NewAddressCommand.class);
        verify(priorityAddressService).register(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().street()).isEqualTo("Rua D'Ávila");
    }

    @Test
    void invalidCepReturns400WithFriendlyMessage() throws Exception {
        when(cepLookupService.lookup("abc")).thenThrow(new InvalidCepException());

        mvc.perform(get("/api/v1/ceps/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("CEP inválido")));
    }

    @Test
    void unexpectedErrorDoesNotLeakTechnicalDetails() throws Exception {
        when(cepLookupService.lookup("01001000"))
                .thenThrow(new IllegalStateException("jdbc:postgresql://db:5432 senha=segredo"));

        mvc.perform(get("/api/v1/ceps/01001000"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.reference").exists())
                .andExpect(content().string(not(containsString("jdbc"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))))
                .andExpect(content().string(not(containsString("segredo"))));
    }

    @Test
    void malformedJsonReturnsGenericMessage() throws Exception {
        mvc.perform(post("/api/v1/addresses").contentType(MediaType.APPLICATION_JSON).content("{\"cep\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato da requisição inválido."));
    }
}
