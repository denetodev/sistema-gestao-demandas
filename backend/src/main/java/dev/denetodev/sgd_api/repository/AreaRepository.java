package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    List<Area> findByDiretoriaId(UUID diretoriaId);
}
