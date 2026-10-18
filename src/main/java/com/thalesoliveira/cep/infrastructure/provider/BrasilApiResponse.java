package com.thalesoliveira.cep.infrastructure.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import com.thalesoliveira.cep.domain.model.Cep;

/** Formato de resposta da BrasilAPI (/api/cep/v1/{cep}). CEP inexistente devolve 404. */
@JsonIgnoreProperties(ignoreUnknown = true)
record BrasilApiResponse(
        String cep,
        String state,
        String city,
        String neighborhood,
        String street) {

    Address toAddress(Cep requested) {
        return new Address(
                requested,
                blankToNull(street),
                null,
                blankToNull(neighborhood),
                blankToNull(city),
                blankToNull(state),
                AddressSource.BRASILAPI);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
