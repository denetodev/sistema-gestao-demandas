package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.CategoriaEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoriaEquipamentoRepository extends JpaRepository<CategoriaEquipamento, UUID> {

    List<CategoriaEquipamento> findAllByOrderByNomeAsc();

    boolean existsByNomeIgnoreCase(String nome);
}
