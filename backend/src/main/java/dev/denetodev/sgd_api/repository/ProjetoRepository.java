package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Projeto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjetoRepository extends JpaRepository<Projeto, UUID> {
}