package com.thalesoliveira.cep.application.port;

import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;

import java.util.Optional;

/**
 * Porta para provedores externos de CEP (ViaCEP, BrasilAPI...).
 * Contrato: devolve vazio quando o CEP não existe e lança
 * {@link com.thalesoliveira.cep.domain.exception.ExternalServiceException} em falha técnica.
 */
public interface CepProvider {

    Optional<Address> lookup(Cep cep);

    String name();
}
