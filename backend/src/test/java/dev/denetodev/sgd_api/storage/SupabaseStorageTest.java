package dev.denetodev.sgd_api.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Confere o formato das chamadas ao Storage (URL, cabeçalhos e corpo) sem sair da máquina. */
class SupabaseStorageTest {

    private static final String BASE = "https://projeto.supabase.co";

    private MockRestServiceServer servidor;
    private SupabaseStorage storage;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        storage = new SupabaseStorage(BASE + "/", "chave-secreta", builder.build());
    }

    @Test
    void enviaOArquivoNoBucketComAChaveEOTipo() {
        servidor.expect(requestTo(BASE + "/storage/v1/object/evidencias/evidencias/abc.png"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-secreta"))
                .andExpect(header("apikey", "chave-secreta"))
                .andExpect(header("Content-Type", "image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        storage.enviar("evidencias/abc.png", new byte[]{1, 2, 3}, "image/png");

        servidor.verify();
    }

    @Test
    void geraUrlAssinadaCompletaAPartirDaRespostaRelativa() {
        servidor.expect(requestTo(BASE + "/storage/v1/object/sign/evidencias/evidencias/abc.png"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer chave-secreta"))
                .andExpect(jsonPath("$.expiresIn").value(300))
                .andRespond(withSuccess(
                        "{\"signedURL\":\"/object/sign/evidencias/evidencias/abc.png?token=xyz\"}", MediaType.APPLICATION_JSON));

        String url = storage.urlAssinada("evidencias/abc.png", 300);

        assertEquals(BASE + "/storage/v1/object/sign/evidencias/evidencias/abc.png?token=xyz", url);
        servidor.verify();
    }

    @Test
    void removeComDelete() {
        servidor.expect(requestTo(BASE + "/storage/v1/object/evidencias/evidencias/abc.png"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("Authorization", "Bearer chave-secreta"))
                .andRespond(withSuccess());

        storage.remover("evidencias/abc.png");

        servidor.verify();
    }

    @Test
    void erroDoStorageViraIndisponivelEFalhaDeLimpezaNaoPropaga() {
        servidor.expect(requestTo(BASE + "/storage/v1/object/evidencias/x.png")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThrows(ArmazenamentoIndisponivelException.class, () -> storage.enviar("x.png", new byte[]{1}, "image/png"));

        servidor.verify();
        servidor.reset();
        servidor.expect(requestTo(BASE + "/storage/v1/object/sign/evidencias/x.png")).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThrows(ArmazenamentoIndisponivelException.class, () -> storage.urlAssinada("x.png", 60));

        servidor.verify();
        servidor.reset();
        servidor.expect(requestTo(BASE + "/storage/v1/object/evidencias/x.png")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertDoesNotThrow(() -> storage.remover("x.png"));
    }

    @Test
    void semConfiguracaoOsEnviosFalhamComMensagemClara() {
        SupabaseStorage semConfig = new SupabaseStorage("", "", null);

        ArmazenamentoIndisponivelException e = assertThrows(ArmazenamentoIndisponivelException.class,
                () -> semConfig.enviar("x.png", new byte[]{1}, "image/png"));
        assertTrue(e.getMessage().contains("SUPABASE_URL"));
        assertDoesNotThrow(() -> semConfig.remover("x.png"));
    }
}
