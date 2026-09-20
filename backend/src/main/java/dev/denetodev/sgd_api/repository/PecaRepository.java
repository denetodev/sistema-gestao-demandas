package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {
    List<Peca> findByDemandaId(UUID demandaId);
}