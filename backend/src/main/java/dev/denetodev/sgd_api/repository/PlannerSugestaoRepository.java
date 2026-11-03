package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.PlannerSugestao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlannerSugestaoRepository extends JpaRepository<PlannerSugestao, UUID> {

    @Query("select s from PlannerSugestao s join fetch s.pessoa where s.pessoa.id = :pessoaId and s.status = 'PENDENTE' order by s.criadaNoPlanner desc nulls last, s.createdAt desc")
    List<PlannerSugestao> pendentesDa(@Param("pessoaId") UUID pessoaId);

    long countByPessoaIdAndStatus(UUID pessoaId, PlannerSugestao.Status status);

    /** Os pares (tarefa, pessoa) que já têm sugestão, qualquer que seja a decisão: não se cria de novo. */
    @Query("select s.tarefaId || '|' || s.pessoa.id from PlannerSugestao s where s.tarefaId in :tarefaIds")
    List<String> chavesJaExistentes(@Param("tarefaIds") Collection<String> tarefaIds);
}
