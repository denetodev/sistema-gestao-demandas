package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PessoaRepository extends JpaRepository<Pessoa, UUID> {

    Optional<Pessoa> findByAuthUserId(UUID authUserId);

    Optional<Pessoa> findByChaveC(String chaveC);

    boolean existsByEmailIgnoreCase(String email);

    /** Alguém nesse status ainda trabalha nessa equipe (impede desativá-la). */
    boolean existsByAreaIdAndStatus(UUID areaId, StatusPessoa status);

    /** Alguém nesse status ainda ocupa esse cargo (impede desativá-lo). */
    boolean existsByCargoIdAndStatus(UUID cargoId, StatusPessoa status);

    List<Pessoa> findByAprovadoEmIsNullAndStatus(StatusPessoa status);

    /** Quem tem etiqueta no Planner: é com essas pessoas que as etiquetas das tarefas são comparadas. */
    List<Pessoa> findByEtiquetaPlannerIsNotNull();

    /** Quem está nesse status: é entre as pessoas ativas que o Planner procura nomes. */
    List<Pessoa> findByStatus(StatusPessoa status);

    boolean existsByEtiquetaPlannerIgnoreCaseAndIdNot(String etiquetaPlanner, UUID id);
}