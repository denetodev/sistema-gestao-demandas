package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Demanda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface DemandaRepository extends JpaRepository<Demanda, UUID> {

    Page<Demanda> findByDiretoriaId(UUID diretoriaId, Pageable pageable);

    @Query("""
        select distinct d from Demanda d
        join PessoaDemanda pd on pd.demanda = d
        where pd.pessoa.id = :pessoaId and pd.dataSaida is null
        """)
    Page<Demanda> findMinhas(@Param("pessoaId") UUID pessoaId, Pageable pageable);

    @Query("""
        select distinct d from Demanda d
        join PessoaDemanda pd on pd.demanda = d
        where pd.pessoa.area.id = :areaId and pd.dataSaida is null
        """)
    Page<Demanda> findEquipe(@Param("areaId") UUID areaId, Pageable pageable);
}