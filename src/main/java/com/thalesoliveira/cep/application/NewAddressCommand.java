package com.thalesoliveira.cep.application;

/** Dados de entrada para cadastrar um endereço prioritário, já validados na borda (API). */
public record NewAddressCommand(
        String cep,
        String street,
        String complement,
        String neighborhood,
        String city,
        String state) {
}
