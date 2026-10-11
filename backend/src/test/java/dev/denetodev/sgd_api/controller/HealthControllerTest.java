package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.config.CorsConfig;
import dev.denetodev.sgd_api.config.SecurityConfig;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.PessoaAuthenticationConverter;
import dev.denetodev.sgd_api.security.RestAccessDeniedHandler;
import dev.denetodev.sgd_api.security.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O healthcheck do Railway chama /health sem token; nada além dele fica público. */
@WebMvcTest(controllers = HealthController.class)
@Import({SecurityConfig.class, CorsConfig.class, PessoaAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class HealthControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean PessoaRepository pessoaRepository;

    @Test
    void healthSemTokenResponde200ComStatusUp() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthSoAceitaGet() throws Exception {
        mvc.perform(post("/health")).andExpect(status().isUnauthorized());
    }
}
