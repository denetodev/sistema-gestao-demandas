package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.EmprestimoItem;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EmprestimoItemRepository extends JpaRepository<EmprestimoItem, UUID> {

    /** Itens ainda não devolvidos de empréstimos que ocupam os equipamentos (aguardando, autorizado ou retirado). */
    @Query("select i from EmprestimoItem i join fetch i.emprestimo e where i.equipamento.id in :ids "
            + "and i.devolvidoEm is null and e.status in :status")
    List<EmprestimoItem> ocupacao(@Param("ids") Collection<UUID> equipamentoIds,
                                  @Param("status") Collection<StatusEmprestimo> status);

    @Query("select i from EmprestimoItem i join fetch i.emprestimo e where i.devolvidoEm is null and e.status in :status")
    List<EmprestimoItem> ocupacaoGeral(@Param("status") Collection<StatusEmprestimo> status);
}
