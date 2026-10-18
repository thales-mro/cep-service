package com.thalesoliveira.cep.domain.exception;

/**
 * Base das exceções de negócio. As mensagens são escritas para o cliente da API
 * e nunca incluem o valor recebido, detalhes técnicos ou stack trace.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
