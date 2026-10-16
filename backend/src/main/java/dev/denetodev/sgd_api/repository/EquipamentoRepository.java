package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Equipamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipamentoRepository extends JpaRepository<Equipamento, UUID> {

    List<Equipamento> findAllByOrderByNomeAsc();

    Optional<Equipamento> findByPatrimonioIgnoreCase(String patrimonio);

    /** Trava as linhas enquanto o empréstimo confere a disponibilidade, para dois pedidos não levarem a mesma peça. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipamento e where e.id in :ids")
    List<Equipamento> travarPorIds(@Param("ids") Collection<UUID> ids);
}
