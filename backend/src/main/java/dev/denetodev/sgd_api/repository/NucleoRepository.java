package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Nucleo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NucleoRepository extends JpaRepository<Nucleo, UUID> {

    boolean existsByDiretoriaIdAndNomeIgnoreCaseAndAtivoTrue(UUID diretoriaId, String nome);

    boolean existsByDiretoriaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(UUID diretoriaId, String nome, UUID id);
}
