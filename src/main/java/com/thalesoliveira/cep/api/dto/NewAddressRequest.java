package com.thalesoliveira.cep.api.dto;

import com.thalesoliveira.cep.application.NewAddressCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo da requisição de cadastro. Validação por lista de caracteres permitidos (allowlist):
 * letras (inclusive acentuadas), números, espaço e pontuação comum de endereço.
 * O apóstrofo é permitido de propósito ("Rua D'Ávila"): ele é seguro porque a gravação
 * usa consultas parametrizadas, e não porque o caractere foi bloqueado.
 */
public record NewAddressRequest(
        @NotBlank(message = "é obrigatório")
        @Pattern(regexp = ValidationPatterns.CEP, message = "deve ter 8 dígitos, com ou sem hífen")
        String cep,

        @NotBlank(message = "é obrigatório")
        @Size(max = 120, message = "deve ter no máximo 120 caracteres")
        @Pattern(regexp = ValidationPatterns.TEXT, message = "contém caracteres não permitidos")
        String street,

        @Size(max = 60, message = "deve ter no máximo 60 caracteres")
        @Pattern(regexp = ValidationPatterns.TEXT, message = "contém caracteres não permitidos")
        String complement,

        @NotBlank(message = "é obrigatório")
        @Size(max = 80, message = "deve ter no máximo 80 caracteres")
        @Pattern(regexp = ValidationPatterns.TEXT, message = "contém caracteres não permitidos")
        String neighborhood,

        @NotBlank(message = "é obrigatório")
        @Size(max = 80, message = "deve ter no máximo 80 caracteres")
        @Pattern(regexp = ValidationPatterns.TEXT, message = "contém caracteres não permitidos")
        String city,

        @NotBlank(message = "é obrigatório")
        @Pattern(regexp = ValidationPatterns.STATE, message = "deve ter 2 letras")
        String state) {

    public NewAddressCommand toCommand() {
        return new NewAddressCommand(cep, street, complement, neighborhood, city, state);
    }
}
