package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AtividadeRepository extends JpaRepository<Atividade, UUID> {
    List<Atividade> findByDemandaId(UUID demandaId);
}