package dev.denetodev.sgd_api.security;

import dev.denetodev.sgd_api.repository.PessoaRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class PessoaAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final PessoaRepository pessoaRepository;

    public PessoaAuthenticationConverter(PessoaRepository pessoaRepository) {
        this.pessoaRepository = pessoaRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        UUID authUserId;
        try {
            authUserId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            return new JwtAuthenticationToken(jwt, authorities);
        }

        pessoaRepository.findByAuthUserId(authUserId).ifPresent(pessoa -> {
            if (pessoa.getAprovadoEm() != null) {
                authorities.add(new SimpleGrantedAuthority("VINCULADO"));
                authorities.add(new SimpleGrantedAuthority("ROLE_" + pessoa.getPerfil().name()));
            }
        });

        return new JwtAuthenticationToken(jwt, authorities);
    }
}