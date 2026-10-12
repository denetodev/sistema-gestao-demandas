package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Consultas de leitura que alimentam o resumo por campanha (uma ida ao banco por tipo, sem N+1). */
public interface CampanhaResumoRepository extends Repository<Demanda, UUID> {

    @Query("select d from Demanda d join fetch d.campanha where d.campanha is not null")
    List<Demanda> demandasComCampanha();

    @Query("select d from Demanda d where d.campanha.id = :campanhaId")
    List<Demanda> demandasDaCampanha(@Param("campanhaId") UUID campanhaId);

    @Query("select p from Peca p join fetch p.demanda where p.demanda.id in :ids")
    List<Peca> pecasDasDemandas(@Param("ids") Collection<UUID> ids);

    @Query("select pd from PessoaDemanda pd join fetch pd.pessoa join fetch pd.demanda where pd.demanda.id in :ids")
    List<PessoaDemanda> vinculosDasDemandas(@Param("ids") Collection<UUID> ids);

    @Query("select a from Atividade a join fetch a.demanda left join fetch a.pessoa where a.demanda.id in :ids")
    List<Atividade> atividadesDasDemandas(@Param("ids") Collection<UUID> ids);
}
