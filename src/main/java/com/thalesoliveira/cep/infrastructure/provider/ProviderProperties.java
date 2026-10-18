package com.thalesoliveira.cep.infrastructure.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Endereços e timeouts dos provedores externos, definidos no application.yml. */
@ConfigurationProperties(prefix = "app.providers")
public record ProviderProperties(Endpoint viacep, Endpoint brasilapi) {

    public record Endpoint(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }
}
