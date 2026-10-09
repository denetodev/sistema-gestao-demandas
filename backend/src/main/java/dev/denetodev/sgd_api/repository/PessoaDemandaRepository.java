package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.PessoaDemanda;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaDemandaRepository extends JpaRepository<PessoaDemanda, UUID> {

    List<PessoaDemanda> findByDemandaId(UUID demandaId);

    Optional<PessoaDemanda> findByPessoaIdAndDemandaIdAndDataSaidaIsNull(UUID pessoaId, UUID demandaId);

    List<PessoaDemanda> findByPessoa_IdAndDataSaidaIsNull(UUID pessoaId);

    List<PessoaDemanda> findByPessoa_AreaIdAndDataSaidaIsNull(UUID areaId);

    @Query("""
        select pd from PessoaDemanda pd
        join fetch pd.demanda
        where pd.pessoa.id in :pessoaIds and pd.dataSaida is null
        """)
    List<PessoaDemanda> findVigentesPorPessoas(@Param("pessoaIds") Collection<UUID> pessoaIds);
}