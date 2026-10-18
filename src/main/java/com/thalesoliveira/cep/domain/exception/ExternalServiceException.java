package com.thalesoliveira.cep.domain.exception;

/**
 * Falha técnica de um provedor externo (timeout, indisponibilidade, resposta inesperada).
 * A causa fica registrada no log; o cliente recebe só uma mensagem genérica.
 */
public class ExternalServiceException extends DomainException {

    private final String provider;

    public ExternalServiceException(String provider, Throwable cause) {
        super("Falha ao consultar o provedor " + provider + ".", cause);
        this.provider = provider;
    }

    public String provider() {
        return provider;
    }
}
