package dev.denetodev.sgd_api.security;

import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentPessoaResolver {

    private final PessoaRepository pessoaRepository;

    public CurrentPessoaResolver(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    public Pessoa resolver(Jwt jwt) {
        UUID authUserId = UUID.fromString(jwt.getSubject());
        return pessoaRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado sem Pessoa vinculada"));
    }
}