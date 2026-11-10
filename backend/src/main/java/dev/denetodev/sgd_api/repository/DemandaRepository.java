package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Demanda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface DemandaRepository extends JpaRepository<Demanda, UUID>, JpaSpecificationExecutor<Demanda> {

    /** A listagem usa tudo isso por demanda: traz junto, em vez de uma consulta para cada uma. */
    @Override
    @EntityGraph(attributePaths = {"diretoria", "demandante", "projeto", "campanha"})
    Page<Demanda> findAll(Specification<Demanda> spec, Pageable pageable);

    List<Demanda> findByDiretoriaId(UUID diretoriaId);

    /** Demandas com esse código de controle (BC2610FN), sem diferenciar maiúsculas. */
    List<Demanda> findByCodigoIgnoreCase(String codigo);
}
