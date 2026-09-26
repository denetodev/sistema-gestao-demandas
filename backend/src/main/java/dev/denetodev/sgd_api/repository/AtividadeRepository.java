package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {

    List<Atividade> findByDemandaId(UUID demandaId);

    List<Atividade> findByPessoa_Id(UUID pessoaId);

    List<Atividade> findByPessoa_AreaId(UUID areaId);

    // atividade sem pessoa (avulsa, sem responsável) nunca aparece no
    // escopo DIRETORIA — só em TODAS. Aceito por ora.
    @Query("select a from Atividade a where a.pessoa.area.diretoria.id = :diretoriaId")
    List<Atividade> findByPessoaAreaDiretoriaId(@Param("diretoriaId") UUID diretoriaId);
}