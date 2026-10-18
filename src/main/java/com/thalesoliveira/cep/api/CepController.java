package com.thalesoliveira.cep.api;

import com.thalesoliveira.cep.api.dto.AddressResponse;
import com.thalesoliveira.cep.application.CepLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ceps")
@Tag(name = "Busca de CEP")
public class CepController {

    private final CepLookupService cepLookupService;

    public CepController(CepLookupService cepLookupService) {
        this.cepLookupService = cepLookupService;
    }

    @GetMapping("/{cep}")
    @Operation(summary = "Busca um CEP",
            description = "Procura primeiro na base prioritária e, se não encontrar, nos provedores externos.")
    public AddressResponse lookup(@PathVariable String cep) {
        return AddressResponse.from(cepLookupService.lookup(cep));
    }
}
