package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Atividade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {

    List<Atividade> findByDemandaId(UUID demandaId);

    /** Linhas [pessoaId, dataRealizacao, quantidade] das pessoas no período. */
    @Query("""
        select a.pessoa.id, a.dataRealizacao, count(a)
        from Atividade a
        where a.pessoa.id in :pessoaIds and a.dataRealizacao between :de and :ate
        group by a.pessoa.id, a.dataRealizacao
        """)
    List<Object[]> contarPorPessoaEDia(
            @Param("pessoaIds") Collection<UUID> pessoaIds,
            @Param("de") LocalDate de,
            @Param("ate") LocalDate ate);

    Page<Atividade> findByPessoa_Id(UUID pessoaId, Pageable pageable);
    Page<Atividade> findByPessoa_AreaId(UUID areaId, Pageable pageable);

    // atividade sem pessoa (avulsa) continua nunca aparecendo no escopo
    // DIRETORIA, só em TODAS — mesma limitação já registrada antes.
    @Query("select a from Atividade a where a.pessoa.area.diretoria.id = :diretoriaId")
    Page<Atividade> findByPessoaAreaDiretoriaId(@Param("diretoriaId") UUID diretoriaId, Pageable pageable);
}