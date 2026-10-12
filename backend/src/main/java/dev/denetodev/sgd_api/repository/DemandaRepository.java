package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Demanda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DemandaRepository extends JpaRepository<Demanda, UUID>, JpaSpecificationExecutor<Demanda> {

    List<Demanda> findByDiretoriaId(UUID diretoriaId);
}
