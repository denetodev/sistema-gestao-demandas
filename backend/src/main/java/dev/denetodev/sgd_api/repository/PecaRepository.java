package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Peca;
import org.springframework.data.jpa.repository.JpaRepository;
import dev.denetodev.sgd_api.entity.StatusDemanda;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PecaRepository extends JpaRepository<Peca, UUID> {
    List<Peca> findByDemandaId(UUID demandaId);

    /**
     * Peças produzidas pelas pessoas no período, fora de demandas canceladas.
     * Sem data de entrega, vale a data de criação do lançamento.
     */
    @Query("""
        select p from Peca p
        join fetch p.demanda d
        where p.pessoa.id in :pessoaIds
          and d.status <> :cancelada
          and ((p.dataEntrega between :de and :ate)
               or (p.dataEntrega is null and p.createdAt >= :deTs and p.createdAt < :ateTs))
        """)
    List<Peca> findProduzidasNoPeriodo(
            @Param("pessoaIds") Collection<UUID> pessoaIds,
            @Param("cancelada") StatusDemanda cancelada,
            @Param("de") LocalDate de,
            @Param("ate") LocalDate ate,
            @Param("deTs") OffsetDateTime deTs,
            @Param("ateTs") OffsetDateTime ateTs);
}