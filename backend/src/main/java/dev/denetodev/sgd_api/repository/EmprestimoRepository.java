package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Emprestimo;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EmprestimoRepository extends JpaRepository<Emprestimo, UUID> {

    List<Emprestimo> findByStatusIn(Collection<StatusEmprestimo> status);
}
