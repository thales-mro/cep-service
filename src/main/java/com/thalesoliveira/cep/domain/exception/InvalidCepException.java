package com.thalesoliveira.cep.domain.exception;

public class InvalidCepException extends DomainException {

    public InvalidCepException() {
        super("CEP inválido. Informe 8 dígitos, com ou sem hífen.");
    }
}
