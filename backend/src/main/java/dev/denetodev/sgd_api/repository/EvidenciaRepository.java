package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EvidenciaRepository extends JpaRepository<Evidencia, UUID> {

    List<Evidencia> findByAtividadeIdIn(Collection<UUID> atividadeIds);

    List<Evidencia> findByPecaIdIn(Collection<UUID> pecaIds);
}