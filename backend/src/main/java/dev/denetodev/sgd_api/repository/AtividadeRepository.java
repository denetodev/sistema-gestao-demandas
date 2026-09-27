package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Atividade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {

    List<Atividade> findByDemandaId(UUID demandaId);

    Page<Atividade> findByPessoa_Id(UUID pessoaId, Pageable pageable);
    Page<Atividade> findByPessoa_AreaId(UUID areaId, Pageable pageable);

    // atividade sem pessoa (avulsa) continua nunca aparecendo no escopo
    // DIRETORIA, só em TODAS — mesma limitação já registrada antes.
    @Query("select a from Atividade a where a.pessoa.area.diretoria.id = :diretoriaId")
    Page<Atividade> findByPessoaAreaDiretoriaId(@Param("diretoriaId") UUID diretoriaId, Pageable pageable);
}