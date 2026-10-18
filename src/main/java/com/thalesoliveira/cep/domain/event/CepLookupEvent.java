package com.thalesoliveira.cep.domain.event;

import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.Cep;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Evento emitido a cada consulta de CEP. É publicado no Kafka e gravado no banco
 * de forma assíncrona, para não atrasar a resposta ao cliente.
 *
 * @param eventId   identificador único, usado para garantir idempotência no consumidor
 * @param cep       CEP consultado (8 dígitos)
 * @param queriedAt horário da consulta
 * @param source    origem do resultado (PRIORITY_BASE, VIACEP, BRASILAPI) ou NOT_FOUND
 * @param found     se o CEP foi encontrado
 * @param response  dados retornados (nulo quando não encontrado)
 */
public record CepLookupEvent(
        UUID eventId,
        String cep,
        Instant queriedAt,
        String source,
        boolean found,
        AddressPayload response) {

    public static final String NOT_FOUND = "NOT_FOUND";

    public static CepLookupEvent of(Cep cep, Instant queriedAt, Optional<Address> result) {
        return new CepLookupEvent(
                UUID.randomUUID(),
                cep.value(),
                queriedAt,
                result.map(address -> address.source().name()).orElse(NOT_FOUND),
                result.isPresent(),
                result.map(AddressPayload::from).orElse(null));
    }

    public record AddressPayload(
            String cep,
            String street,
            String complement,
            String neighborhood,
            String city,
            String state) {

        public static AddressPayload from(Address address) {
            return new AddressPayload(
                    address.cep().formatted(),
                    address.street(),
                    address.complement(),
                    address.neighborhood(),
                    address.city(),
                    address.state());
        }
    }
}
