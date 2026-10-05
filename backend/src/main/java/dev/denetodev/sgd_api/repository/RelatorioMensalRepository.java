package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.RelatorioMensal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelatorioMensalRepository extends JpaRepository<RelatorioMensal, UUID> {

    Optional<RelatorioMensal> findByPessoaIdAndMes(UUID pessoaId, LocalDate mes);

    List<RelatorioMensal> findByMesAndPessoaIdIn(LocalDate mes, Collection<UUID> pessoaIds);
}
