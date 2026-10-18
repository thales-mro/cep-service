package com.thalesoliveira.cep.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface LookupLogJpaRepository extends JpaRepository<LookupLogEntity, Long> {

    boolean existsByEventId(UUID eventId);

    List<LookupLogEntity> findAllByOrderByQueriedAtDesc(Pageable pageable);

    List<LookupLogEntity> findByCepOrderByQueriedAtDesc(String cep, Pageable pageable);
}
