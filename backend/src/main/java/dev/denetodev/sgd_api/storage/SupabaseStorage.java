package dev.denetodev.sgd_api.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Supabase Storage via API REST, autenticado com a service role key (que só existe no backend).
 * Sem SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY, as operações falham com mensagem clara e o
 * resto da API continua funcionando.
 */
@Component
public class SupabaseStorage implements ArquivoStorage {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorage.class);
    static final String BUCKET = "evidencias";

    private final String baseUrl;
    private final String chave;
    /** Criado na primeira chamada: subir a API sem Storage configurado não deve montar cliente HTTP à toa. */
    private RestClient http;

    @Autowired
    public SupabaseStorage(
            @Value("${app.supabase.url:}") String url,
            @Value("${app.supabase.service-role-key:}") String chave
    ) {
        this(url, chave, null);
    }

    /** Para testes: permite trocar o cliente HTTP. */
    SupabaseStorage(String url, String chave, RestClient http) {
        this.baseUrl = url == null ? "" : url.replaceAll("/+$", "");
        this.chave = chave == null ? "" : chave;
        this.http = http;
    }

    @Override
    public void enviar(String caminho, byte[] conteudo, String contentType) {
        exigirConfigurado();
        try {
            http().post()
                    .uri(baseUrl + "/storage/v1/object/" + BUCKET + "/" + caminho)
                    .headers(this::autenticar)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(conteudo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ArmazenamentoIndisponivelException("Não foi possível guardar o arquivo no Storage", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String urlAssinada(String caminho, int validadeSegundos) {
        exigirConfigurado();
        try {
            Map<String, Object> resposta = http().post()
                    .uri(baseUrl + "/storage/v1/object/sign/" + BUCKET + "/" + caminho)
                    .headers(this::autenticar)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", validadeSegundos))
                    .retrieve()
                    .body(Map.class);
            Object assinada = resposta == null ? null : resposta.get("signedURL");
            if (assinada == null) {
                throw new ArmazenamentoIndisponivelException("O Storage não devolveu a URL assinada", null);
            }
            String relativa = assinada.toString();
            return baseUrl + "/storage/v1" + (relativa.startsWith("/") ? relativa : "/" + relativa);
        } catch (RestClientException e) {
            throw new ArmazenamentoIndisponivelException("Não foi possível gerar a URL do arquivo", e);
        }
    }

    @Override
    public void remover(String caminho) {
        if (!configurado()) {
            return;
        }
        try {
            http().delete()
                    .uri(baseUrl + "/storage/v1/object/" + BUCKET + "/" + caminho)
                    .headers(this::autenticar)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Falha ao remover {} do Storage; o arquivo ficou órfão", caminho, e);
        }
    }

    private synchronized RestClient http() {
        if (http == null) {
            http = RestClient.create();
        }
        return http;
    }

    private void autenticar(HttpHeaders headers) {
        headers.set("apikey", chave);
        headers.setBearerAuth(chave);
    }

    private boolean configurado() {
        return !baseUrl.isBlank() && !chave.isBlank();
    }

    private void exigirConfigurado() {
        if (!configurado()) {
            throw new ArmazenamentoIndisponivelException(
                    "Storage não configurado: defina SUPABASE_URL e SUPABASE_SERVICE_ROLE_KEY", null);
        }
    }
}
