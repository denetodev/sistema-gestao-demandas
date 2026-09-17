package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CargoRepository extends JpaRepository<Cargo, UUID> {
}