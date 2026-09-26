package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Demanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DemandaRepository extends JpaRepository<Demanda, UUID> {
    List<Demanda> findByDiretoriaId(UUID diretoriaId);
}
