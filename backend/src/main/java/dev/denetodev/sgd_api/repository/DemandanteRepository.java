package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Demandante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DemandanteRepository extends JpaRepository<Demandante, UUID> {
}