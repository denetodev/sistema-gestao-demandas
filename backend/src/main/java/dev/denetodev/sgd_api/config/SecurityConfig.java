package dev.denetodev.sgd_api.config;

import dev.denetodev.sgd_api.security.PessoaAuthenticationConverter;
import dev.denetodev.sgd_api.security.RestAccessDeniedHandler;
import dev.denetodev.sgd_api.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final HttpMethod[] ESCRITA = {
            HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE };

    private static final String[] CADASTROS_BASE = {
            "/diretorias/**", "/nucleos/**", "/areas/**", "/cargos/**", "/tipos-atividade/**", "/demandantes/**" };

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            PessoaAuthenticationConverter pessoaAuthenticationConverter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/health").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/pessoas/me").authenticated();
                    auth.requestMatchers(HttpMethod.POST, "/pessoas/auto-cadastro").authenticated();
                    // listas usadas na tela de completar cadastro, antes de a conta ser vinculada (só leitura)
                    auth.requestMatchers(HttpMethod.GET, "/areas", "/cargos").authenticated();
                    // Visualizador é somente leitura, mas edita o próprio perfil
                    auth.requestMatchers(HttpMethod.PUT, "/pessoas/me").hasAuthority("VINCULADO");
                    // marcar os próprios avisos como vistos não é escrita de dados do sistema: vale para todo vinculado
                    auth.requestMatchers(HttpMethod.POST, "/notificacoes/vistas").hasAuthority("VINCULADO");

                    // gestão de pessoas (o serviço ainda barra o Gestor de mexer em ADMIN)
                    auth.requestMatchers(HttpMethod.PATCH, "/pessoas/*/vincular-auth").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.POST, "/pessoas").hasAnyRole("ADMIN", "GESTOR");
                    auth.requestMatchers(HttpMethod.PUT, "/pessoas/*").hasAnyRole("ADMIN", "GESTOR");
                    auth.requestMatchers(HttpMethod.DELETE, "/pessoas/*").hasAnyRole("ADMIN", "GESTOR");
                    auth.requestMatchers(HttpMethod.PATCH, "/pessoas/*/aprovar", "/pessoas/*/rejeitar")
                            .hasAnyRole("ADMIN", "GESTOR");
                    auth.requestMatchers(HttpMethod.POST, "/pessoas/*/vincular-a").hasAnyRole("ADMIN", "GESTOR");

                    // cadastros base: só Gestor e Admin escrevem
                    for (HttpMethod metodo : ESCRITA) {
                        auth.requestMatchers(metodo, CADASTROS_BASE).hasAnyRole("ADMIN", "GESTOR");
                    }
                    // projeto e campanha: criar e editar passa pela barreira para Admin, Gestor e Profissional, e o serviço só
                    // libera o Profissional que é Referência de equipe; excluir ou encerrar é só da gestão
                    auth.requestMatchers(HttpMethod.DELETE, "/campanhas/**", "/projetos/**").hasAnyRole("ADMIN", "GESTOR");

                    // demais escritas (demandas, atividades, peças, evidências, relatório, tipos de peça,
                    // campanhas): Visualizador é somente leitura. Regras finas por item ficam nos serviços.
                    for (HttpMethod metodo : ESCRITA) {
                        auth.requestMatchers(metodo, "/**").hasAnyRole("ADMIN", "GESTOR", "PROFISSIONAL");
                    }
                    auth.anyRequest().hasAuthority("VINCULADO");
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(pessoaAuthenticationConverter))
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(restAccessDeniedHandler)
                );

        return http.build();
    }
}