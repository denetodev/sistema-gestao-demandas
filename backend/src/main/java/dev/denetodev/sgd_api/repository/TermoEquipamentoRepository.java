package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.TermoEquipamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TermoEquipamentoRepository extends JpaRepository<TermoEquipamento, Integer> {

    Optional<TermoEquipamento> findTopByOrderByVersaoDesc();
}
