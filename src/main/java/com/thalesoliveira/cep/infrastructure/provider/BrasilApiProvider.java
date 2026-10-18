package com.thalesoliveira.cep.infrastructure.provider;

import com.thalesoliveira.cep.application.port.CepProvider;
import com.thalesoliveira.cep.domain.exception.ExternalServiceException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/** Provedor de contingência: BrasilAPI (https://brasilapi.com.br). */
public class BrasilApiProvider implements CepProvider {

    private static final int NOT_FOUND = 404;

    private final RestClient client;

    public BrasilApiProvider(RestClient client) {
        this.client = client;
    }

    @Override
    public Optional<Address> lookup(Cep cep) {
        try {
            return client.get()
                    .uri("/api/cep/v1/{cep}", cep.value())
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() == NOT_FOUND) {
                            return Optional.<Address>empty();
                        }
                        if (response.getStatusCode().isError()) {
                            throw new ExternalServiceException(name(),
                                    new IllegalStateException("HTTP " + response.getStatusCode().value()));
                        }
                        BrasilApiResponse body = response.bodyTo(BrasilApiResponse.class);
                        return Optional.ofNullable(body).map(b -> b.toAddress(cep));
                    });
        } catch (RestClientException e) {
            throw new ExternalServiceException(name(), e);
        }
    }

    @Override
    public String name() {
        return "brasilapi";
    }
}
