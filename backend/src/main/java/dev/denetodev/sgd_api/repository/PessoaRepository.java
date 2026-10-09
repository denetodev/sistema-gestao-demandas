package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

    Optional<Pessoa> findByAuthUserId(UUID authUserId);

    Optional<Pessoa> findByCpf(String cpf);

    List<Pessoa> findByAprovadoEmIsNullAndStatus(StatusPessoa status);
}