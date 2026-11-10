package dev.denetodev.sgd_api.repository;

import dev.denetodev.sgd_api.entity.StatusPessoa;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug: "Atualizar pelo Planner" dava erro 500. O Open-in-view está desligado e o serviço lê pessoa.area.diretoria fora
 * de transação; sem trazer equipe e diretoria junto, a leitura estoura LazyInitializationException.
 */
class PessoaRepositoryPlannerTest {

    @Test
    void findByStatusTrazEquipeEDiretoriaJunto() throws NoSuchMethodException {
        EntityGraph grafo = PessoaRepository.class.getMethod("findByStatus", StatusPessoa.class).getAnnotation(EntityGraph.class);

        assertNotNull(grafo, "sem @EntityGraph o PlannerSyncService quebra ao ler a diretoria da pessoa");
        assertTrue(List.of(grafo.attributePaths()).containsAll(List.of("area", "area.diretoria")));
    }
}
