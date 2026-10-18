package com.thalesoliveira.cep.domain.model;

import java.util.Objects;

/** Endereço retornado pela busca, com a origem do dado (base prioritária ou provedor externo). */
public record Address(
        Cep cep,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state,
        AddressSource source) {

    public Address {
        Objects.requireNonNull(cep, "cep");
        Objects.requireNonNull(source, "source");
    }
}
