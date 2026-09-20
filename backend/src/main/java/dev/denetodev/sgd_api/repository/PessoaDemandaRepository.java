package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.PessoaDemanda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaDemandaRepository extends JpaRepository<PessoaDemanda, UUID> {

    List<PessoaDemanda> findByDemandaId(UUID demandaId);

    Optional<PessoaDemanda> findByPessoaIdAndDemandaIdAndDataSaidaIsNull(UUID pessoaId, UUID demandaId);
}