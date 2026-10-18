package com.thalesoliveira.cep.infrastructure.provider;

import com.thalesoliveira.cep.application.port.CepProvider;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Provedor principal: ViaCEP (https://viacep.com.br). */
public class ViaCepProvider implements CepProvider {

    private final RestClient client;

    public ViaCepProvider(RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<Address> lookup(Cep cep) {
        try {
            ViaCepResponse response = client.get()
                    .uri("/ws/{cep}/json/", cep.value())
                    .retrieve()
                    .body(ViaCepResponse.class);
            if (response == null || response.notFound()) {
                return Optional.empty();
            }
            return Optional.of(response.toAddress(cep));
        } catch (RestClientException e) {
            throw new ExternalServiceException(name(), e);
        }
    }

    @Override
    public String name() {
        return "viacep";
    }
}
