package com.thalesoliveira.cep.api.dto;

import com.thalesoliveira.cep.domain.model.Address;

public record AddressResponse(
        String cep,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state,
        String source) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.cep().formatted(),
                address.street(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state(),
                address.source().name());
    }
}
