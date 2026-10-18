package com.thalesoliveira.cep.infrastructure.persistence;

import com.thalesoliveira.cep.application.port.PriorityAddressRepository;
import com.thalesoliveira.cep.domain.model.Address;
import com.thalesoliveira.cep.domain.model.AddressSource;
import com.thalesoliveira.cep.domain.model.Cep;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

/** Adaptador JPA da porta {@link PriorityAddressRepository}: converte entre entidade e domínio. */
@Component
class PriorityAddressRepositoryAdapter implements PriorityAddressRepository {

    private final PriorityAddressJpaRepository jpa;
    private final Clock clock;

    PriorityAddressRepositoryAdapter(PriorityAddressJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public Optional<Address> findByCep(Cep cep) {
        return jpa.findByCep(cep.value()).map(PriorityAddressRepositoryAdapter::toDomain);
    }

    @Override
    public boolean existsByCep(Cep cep) {
        return jpa.existsByCep(cep.value());
    }

    @Override
    public Address save(Address address) {
        PriorityAddressEntity entity = new PriorityAddressEntity(
                address.cep().value(),
                address.street(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state(),
                clock.instant());
        return toDomain(jpa.save(entity));
    }

    @Override
    public List<Address> findAll() {
        return jpa.findAllByOrderByCityAscStreetAsc().stream()
                .map(PriorityAddressRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Address> findByCity(String city) {
        return jpa.findByCityIgnoreCase(city).stream()
                .map(PriorityAddressRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean deleteByCep(Cep cep) {
        return jpa.deleteByCep(cep.value()) > 0;
    }

    private static Address toDomain(PriorityAddressEntity entity) {
        return new Address(
                new Cep(entity.getCep()),
                entity.getStreet(),
                entity.getComplement(),
                entity.getNeighborhood(),
                entity.getCity(),
                entity.getState(),
                AddressSource.PRIORITY_BASE);
    }
}
