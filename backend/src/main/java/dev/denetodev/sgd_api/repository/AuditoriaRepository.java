package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

    List<Auditoria> findByTabelaAndRegistroIdAndCampoOrderByCreatedAtDesc(String tabela, UUID registroId, String campo);
}