package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.EmprestimoFoto;
import dev.denetodev.sgd_api.entity.MomentoFoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmprestimoFotoRepository extends JpaRepository<EmprestimoFoto, UUID> {

    List<EmprestimoFoto> findByEmprestimoIdOrderByCreatedAtAsc(UUID emprestimoId);

    long countByEmprestimoIdAndMomento(UUID emprestimoId, MomentoFoto momento);
}
