package com.thalesoliveira.cep.domain.exception;

import com.thalesoliveira.cep.domain.model.Cep;

public class CepNotFoundException extends DomainException {

    public CepNotFoundException(Cep cep) {
        super("CEP " + cep.formatted() + " não encontrado.");
    }
}
