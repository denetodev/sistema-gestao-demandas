package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Evidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EvidenciaRepository extends JpaRepository<Evidencia, UUID> {
}