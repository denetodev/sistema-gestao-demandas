package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    List<Auditoria> findByTabelaAndRegistroIdAndCampoOrderByCreatedAtDesc(String tabela, UUID registroId, String campo);

    /** Passa para outra pessoa a autoria das linhas de uma que vai ser apagada (a FK alterado_por impediria o delete). */
    @Modifying
    @Query("update Auditoria a set a.alteradoPor = :novo where a.alteradoPor = :antigo")
    int transferirAutoria(@Param("antigo") UUID antigo, @Param("novo") UUID novo);
}