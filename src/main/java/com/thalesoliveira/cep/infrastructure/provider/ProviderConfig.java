package com.thalesoliveira.cep.infrastructure.provider;

import com.thalesoliveira.cep.application.port.CepProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Monta os provedores externos. O RestClient.Builder do Spring Boot já vem com
 * observabilidade: cada chamada gera a métrica http.client.requests (latência por provedor).
 */
@Configuration
class ProviderConfig {

    @Bean
    ViaCepProvider viaCepProvider(RestClient.Builder builder, ProviderProperties properties) {
        return new ViaCepProvider(restClient(builder, properties.viacep()));
    }

    @Bean
    BrasilApiProvider brasilApiProvider(RestClient.Builder builder, ProviderProperties properties) {
        return new BrasilApiProvider(restClient(builder, properties.brasilapi()));
    }

    /** Provedor usado pelo caso de uso: ViaCEP primeiro, BrasilAPI como contingência. */
    @Bean
    @Primary
    CepProvider cepProvider(ViaCepProvider viaCep, BrasilApiProvider brasilApi) {
        return new FallbackCepProvider(List.of(viaCep, brasilApi));
    }

    private static RestClient restClient(RestClient.Builder builder, ProviderProperties.Endpoint endpoint) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(endpoint.connectTimeout());
        requestFactory.setReadTimeout(endpoint.readTimeout());
        return builder.clone()
                .baseUrl(endpoint.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
