package com.thalesoliveira.cep.api;

import com.thalesoliveira.cep.api.dto.AddressResponse;
import com.thalesoliveira.cep.api.dto.NewAddressRequest;
import com.thalesoliveira.cep.application.PriorityAddressService;
import com.thalesoliveira.cep.domain.model.Address;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@Tag(name = "Endereços prioritários")
public class AddressController {

    private final PriorityAddressService priorityAddressService;

    public AddressController(PriorityAddressService priorityAddressService) {
        this.priorityAddressService = priorityAddressService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um endereço prioritário",
            description = "O endereço cadastrado passa a ser devolvido na busca antes dos provedores externos.")
    public ResponseEntity<AddressResponse> create(@Valid @RequestBody NewAddressRequest request) {
        Address address = priorityAddressService.register(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{cep}")
                .buildAndExpand(address.cep().value())
                .toUri();
        return ResponseEntity.created(location).body(AddressResponse.from(address));
    }

    @GetMapping("/{cep}")
    @Operation(summary = "Consulta um endereço prioritário pelo CEP")
    public AddressResponse find(@PathVariable String cep) {
        return AddressResponse.from(priorityAddressService.find(cep));
    }

    @GetMapping
    @Operation(summary = "Lista os endereços prioritários, com filtro opcional por cidade")
    public List<AddressResponse> list(@RequestParam(required = false) String city) {
        return priorityAddressService.list(city).stream().map(AddressResponse::from).toList();
    }

    @DeleteMapping("/{cep}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um endereço prioritário")
    public void delete(@PathVariable String cep) {
        priorityAddressService.remove(cep);
    }
}
