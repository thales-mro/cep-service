package com.thalesoliveira.cep.domain.exception;

import com.thalesoliveira.cep.domain.model.Cep;

public class AddressAlreadyExistsException extends DomainException {

    public AddressAlreadyExistsException(Cep cep) {
        super("Já existe um endereço prioritário para o CEP " + cep.formatted() + ".");
    }
}
