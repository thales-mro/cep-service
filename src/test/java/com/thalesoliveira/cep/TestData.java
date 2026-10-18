package com.thalesoliveira.cep;

import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import com.thalesoliveira.cep.domain.model.Cep;

public final class TestData {

    public static final Cep SE_CEP = new Cep("01001000");

    private TestData() {
    }

    public static Address pracaDaSe(AddressSource source) {
        return new Address(SE_CEP, "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP", source);
    }
}
