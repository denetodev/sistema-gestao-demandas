package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.TipoAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TipoAtividadeRepository extends JpaRepository<TipoAtividade, UUID> {
}
