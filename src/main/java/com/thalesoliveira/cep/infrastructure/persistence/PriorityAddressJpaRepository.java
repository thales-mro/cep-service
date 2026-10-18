package com.thalesoliveira.cep.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Todas as consultas são parametrizadas: métodos derivados do Spring Data e JPQL com
 * parâmetros nomeados (:city). Nenhuma consulta é montada por concatenação de strings,
 * então a entrada do usuário nunca é interpretada como SQL.
 */
interface PriorityAddressJpaRepository extends JpaRepository<PriorityAddressEntity, Long> {

    Optional<PriorityAddressEntity> findByCep(String cep);

    boolean existsByCep(String cep);

    long deleteByCep(String cep);

    List<PriorityAddressEntity> findAllByOrderByCityAscStreetAsc();

    @Query("select a from PriorityAddressEntity a where lower(a.city) = lower(:city) order by a.street")
    List<PriorityAddressEntity> findByCityIgnoreCase(@Param("city") String city);
}
