package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Campanha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampanhaRepository extends JpaRepository<Campanha, UUID> {
}