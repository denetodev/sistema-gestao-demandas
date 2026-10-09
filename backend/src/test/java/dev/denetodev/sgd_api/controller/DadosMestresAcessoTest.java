package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.config.CorsConfig;
import dev.denetodev.sgd_api.config.SecurityConfig;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.PessoaAuthenticationConverter;
import dev.denetodev.sgd_api.security.RestAccessDeniedHandler;
import dev.denetodev.sgd_api.security.RestAuthenticationEntryPoint;
import dev.denetodev.sgd_api.service.AreaService;
import dev.denetodev.sgd_api.service.CargoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Quem acabou de criar a conta (token válido, ainda sem Pessoa aprovada) precisa listar áreas e
 * cargos para completar o cadastro, mas só leitura: o resto de /areas e /cargos exige VINCULADO.
 */
@WebMvcTest(controllers = {AreaController.class, CargoController.class})
@Import({SecurityConfig.class, CorsConfig.class, PessoaAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class DadosMestresAcessoTest {

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean PessoaRepository pessoaRepository;
    @MockitoBean AreaService areaService;
    @MockitoBean CargoService cargoService;

    @BeforeEach
    void tokenDeContaNovaSemPessoaAprovada() {
        UUID authUserId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(authUserId.toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(jwtDecoder.decode(any())).thenReturn(jwt);
        when(pessoaRepository.findByAuthUserId(authUserId)).thenReturn(Optional.empty());
        when(areaService.listarTodas()).thenReturn(List.of());
        when(cargoService.listarTodas()).thenReturn(List.of());
    }

    @Test
    void contaNaoVinculadaListaAreas() throws Exception {
        mvc.perform(get("/areas").header("Authorization", "Bearer t")).andExpect(status().isOk());
    }

    @Test
    void contaNaoVinculadaListaCargos() throws Exception {
        mvc.perform(get("/cargos").header("Authorization", "Bearer t")).andExpect(status().isOk());
    }

    @Test
    void semTokenAreasDa401() throws Exception {
        mvc.perform(get("/areas")).andExpect(status().isUnauthorized());
    }

    @Test
    void contaNaoVinculadaNaoCriaArea() throws Exception {
        mvc.perform(post("/areas").header("Authorization", "Bearer t")
                        .contentType("application/json").content("{\"nome\":\"x\"}"))
                .andExpect(status().isForbidden());
    }
}
