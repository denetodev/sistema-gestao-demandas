package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Pessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {
    Optional<Pessoa> findByAuthUserId(UUID authUserId);

}

