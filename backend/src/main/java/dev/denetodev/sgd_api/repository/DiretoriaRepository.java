package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Diretoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiretoriaRepository extends JpaRepository<Diretoria, UUID> {

    Optional<Diretoria> findByNomeIgnoreCase(String nome);
}