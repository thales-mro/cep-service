package com.thalesoliveira.cep.application;

import com.thalesoliveira.cep.application.port.PriorityAddressRepository;
import com.thalesoliveira.cep.domain.exception.AddressAlreadyExistsException;
import com.thalesoliveira.cep.domain.exception.CepNotFoundException;
import com.thalesoliveira.cep.domain.exception.InvalidAddressException;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import com.thalesoliveira.cep.domain.model.BrazilianState;
import com.thalesoliveira.cep.domain.model.Cep;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Casos de uso da base de endereços prioritários (cadastro, consulta e remoção). */
@Service
public class PriorityAddressService {

    private static final int MAX_CITY_FILTER_LENGTH = 80;

    private final PriorityAddressRepository repository;

    public PriorityAddressService(PriorityAddressRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Address register(NewAddressCommand command) {
        Cep cep = Cep.of(command.cep());
        if (repository.existsByCep(cep)) {
            throw new AddressAlreadyExistsException(cep);
        }
        Address address = new Address(
                cep,
                required(TextSanitizer.clean(command.street()), "Logradouro"),
                TextSanitizer.clean(command.complement()),
                required(TextSanitizer.clean(command.neighborhood()), "Bairro"),
                required(TextSanitizer.clean(command.city()), "Cidade"),
                BrazilianState.parse(command.state()).name(),
                AddressSource.PRIORITY_BASE);
        return repository.save(address);
    }

    @Transactional(readOnly = true)
    public Address find(String rawCep) {
        Cep cep = Cep.of(rawCep);
        return repository.findByCep(cep).orElseThrow(() -> new CepNotFoundException(cep));
    }

    @Transactional(readOnly = true)
    public List<Address> list(String city) {
        String cleanCity = TextSanitizer.clean(city);
        if (cleanCity == null) {
            return repository.findAll();
        }
        if (cleanCity.length() > MAX_CITY_FILTER_LENGTH) {
            throw new InvalidAddressException("Filtro de cidade muito longo.");
        }
        return repository.findByCity(cleanCity);
    }

    @Transactional
    public void remove(String rawCep) {
        Cep cep = Cep.of(rawCep);
        if (!repository.deleteByCep(cep)) {
            throw new CepNotFoundException(cep);
        }
    }

    private static String required(String value, String field) {
        if (value == null) {
            throw new InvalidAddressException(field + " é obrigatório.");
        }
        return value;
    }
}
