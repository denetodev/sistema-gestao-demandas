package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.TipoPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TipoPecaRepository extends JpaRepository<TipoPeca, UUID> {
}