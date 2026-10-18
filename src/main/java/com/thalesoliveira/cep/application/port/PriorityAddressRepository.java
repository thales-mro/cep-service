package com.thalesoliveira.cep.application.port;

import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;

import java.util.List;
import java.util.Optional;

/** Porta da base de endereços prioritários, consultada antes dos provedores externos. */
public interface PriorityAddressRepository {

    Optional<Address> findByCep(Cep cep);

    boolean existsByCep(Cep cep);

    Address save(Address address);

    List<Address> findAll();

    List<Address> findByCity(String city);

    /** @return true se havia um endereço para o CEP e ele foi removido */
    boolean deleteByCep(Cep cep);
}
