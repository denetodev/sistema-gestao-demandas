package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PessoaFotoServiceTest {

    private static final String BASE = "https://projeto.supabase.co/storage/v1/object/public/avatares/";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};

    private ArquivoStorage storage;
    private CurrentPessoaResolver resolver;
    private PessoaFotoService service;
    private Pessoa pessoa;
    private final Jwt jwt = mock(Jwt.class);

    @BeforeEach
    void preparar() {
        storage = mock(ArquivoStorage.class);
        resolver = mock(CurrentPessoaResolver.class);
        PessoaService pessoaService = mock(PessoaService.class);
        service = new PessoaFotoService(resolver, storage, pessoaService);

        Area area = mock(Area.class);
        when(area.getDiretoria()).thenReturn(mock(Diretoria.class));
        pessoa = new Pessoa("Fulano", area);
        ReflectionTestUtils.setField(pessoa, "id", UUID.randomUUID());
        when(resolver.resolver(jwt)).thenReturn(pessoa);
        when(storage.enviarPublico(any(), any(), any())).thenAnswer(inv -> BASE + inv.getArgument(0));
        when(storage.caminhoPublico(any())).thenAnswer(inv -> {
            String url = inv.getArgument(0);
            return url != null && url.startsWith(BASE) ? url.substring(BASE.length()) : null;
        });
    }

    @Test
    void enviaAFotoParaOBucketPublicoComNomeNovoEGuardaAUrl() {
        service.enviar(jwt, new MockMultipartFile("arquivo", "eu.png", "image/png", PNG));

        ArgumentCaptor<String> caminho = ArgumentCaptor.forClass(String.class);
        verify(storage).enviarPublico(caminho.capture(), eq(PNG), eq("image/png"));
        assertTrue(caminho.getValue().startsWith("perfis/" + pessoa.getId() + "-"));
        assertTrue(caminho.getValue().endsWith(".png"));
        assertEquals(BASE + caminho.getValue(), pessoa.getFotoUrl());
        verify(storage, never()).removerPublico(any());
    }

    @Test
    void trocarAFotoApagaAAnteriorDoBucket() {
        pessoa.setFotoUrl(BASE + "perfis/antiga.jpg");

        service.enviar(jwt, new MockMultipartFile("arquivo", "eu.jpg", "image/jpeg", JPEG));

        verify(storage).removerPublico("perfis/antiga.jpg");
        assertNotEquals(BASE + "perfis/antiga.jpg", pessoa.getFotoUrl());
    }

    @Test
    void urlDeFora_deOutroLugar_naoEMexidaNoStorage() {
        pessoa.setFotoUrl("https://exemplo.com/foto.jpg");

        service.enviar(jwt, new MockMultipartFile("arquivo", "eu.png", "image/png", PNG));

        verify(storage, never()).removerPublico(any());
    }

    @Test
    void recusaArquivoQueNaoEImagemVaziaOuGrande() {
        assertThrows(IllegalArgumentException.class,
                () -> service.enviar(jwt, new MockMultipartFile("arquivo", "x.png", "image/png", new byte[0])));
        assertThrows(IllegalArgumentException.class,
                () -> service.enviar(jwt, new MockMultipartFile("arquivo", "x.png", "image/png", new byte[PessoaFotoService.TAMANHO_MAXIMO_BYTES + 1])));
        assertThrows(IllegalArgumentException.class,
                () -> service.enviar(jwt, new MockMultipartFile("arquivo", "x.txt", "text/plain", "texto qualquer".getBytes())));
        verify(storage, never()).enviarPublico(any(), any(), any());
        assertNull(pessoa.getFotoUrl());
    }

    @Test
    void removerLimpaAUrlEApagaOArquivo() {
        pessoa.setFotoUrl(BASE + "perfis/minha.png");

        service.remover(jwt);

        assertNull(pessoa.getFotoUrl());
        verify(storage).removerPublico("perfis/minha.png");
    }
}
