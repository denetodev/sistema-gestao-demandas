package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public final class Resolvers {

    private Resolvers() {
    }

    public static <T> T resolverOuNulo(UUID id, JpaRepository<T, UUID> repository, String nomeEntidade) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeEntidade + " não encontrado(a): " + id));
    }

    public static <T> T resolverObrigatorio(UUID id, JpaRepository<T, UUID> repository, String nomeEntidade) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeEntidade + " não encontrado(a): " + id));
    }
}