package com.thalesoliveira.cep.infrastructure.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import com.thalesoliveira.cep.domain.model.Cep;

/** Formato de resposta da ViaCEP. Para CEP inexistente, ela devolve 200 com {"erro": true}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record ViaCepResponse(
        String cep,
        String logradouro,
        String complemento,
        String bairro,
        String localidade,
        String uf,
        Boolean erro) {

    boolean notFound() {
        return Boolean.TRUE.equals(erro) || localidade == null;
    }

    Address toAddress(Cep requested) {
        return new Address(
                requested,
                blankToNull(logradouro),
                blankToNull(complemento),
                blankToNull(bairro),
                blankToNull(localidade),
                blankToNull(uf),
                AddressSource.VIACEP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
