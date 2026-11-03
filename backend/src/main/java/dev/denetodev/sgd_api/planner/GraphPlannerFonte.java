package dev.denetodev.sgd_api.planner;

import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê o Planner pelo Microsoft Graph com a permissão de aplicativo {@code Tasks.Read.All} (sem ninguém logado):
 * o token vem do Entra ID por client credentials e fica guardado até perto de vencer.
 * Só faz GET. Nenhum segredo vai para o log nem para as mensagens de erro.
 */
@Component
public class GraphPlannerFonte implements PlannerFonte {

    private static final String GRAPH = "https://graph.microsoft.com/v1.0";
    private static final String LOGIN = "https://login.microsoftonline.com/";
    /** Uma página de tarefas por vez; o limite evita um laço sem fim se o Graph repetir o link da próxima página. */
    private static final int MAXIMO_DE_PAGINAS = 50;

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final List<String> planos;
    private RestClient http;

    private String token;
    private Instant tokenVenceEm = Instant.EPOCH;

    public GraphPlannerFonte(
            @Value("${app.planner.tenant-id:}") String tenantId,
            @Value("${app.planner.client-id:}") String clientId,
            @Value("${app.planner.client-secret:}") String clientSecret,
            @Value("${app.planner.plan-ids:}") String planos
    ) {
        this.tenantId = limpo(tenantId);
        this.clientId = limpo(clientId);
        this.clientSecret = limpo(clientSecret);
        this.planos = Arrays.stream(planos == null ? new String[0] : planos.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static String limpo(String valor) {
        return valor == null ? "" : valor.replaceAll("\\s+", "");
    }

    @Override
    public boolean configurada() {
        return !tenantId.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty() && !planos.isEmpty();
    }

    @Override
    public List<String> planos() {
        return planos;
    }

    @Override
    public PlanoPlanner ler(String planId) {
        try {
            JsonNode detalhes = get(GRAPH + "/planner/plans/" + planId + "/details");
            Map<String, String> etiquetas = new LinkedHashMap<>();
            JsonNode descricoes = detalhes.path("categoryDescriptions");
            for (String chave : descricoes.propertyNames()) {
                String nome = descricoes.path(chave).asString(null);
                if (nome != null && !nome.isBlank()) {
                    etiquetas.put(chave, nome.trim());
                }
            }

            Map<String, String> buckets = new LinkedHashMap<>();
            for (JsonNode b : paginas(GRAPH + "/planner/plans/" + planId + "/buckets")) {
                buckets.put(b.path("id").asString(""), b.path("name").asString(""));
            }

            List<TarefaPlanner> tarefas = new ArrayList<>();
            for (JsonNode t : paginas(GRAPH + "/planner/plans/" + planId + "/tasks")) {
                List<String> aplicadas = new ArrayList<>();
                JsonNode categorias = t.path("appliedCategories");
                for (String chave : categorias.propertyNames()) {
                    if (categorias.path(chave).asBoolean(false)) {
                        aplicadas.add(chave);
                    }
                }
                String criada = t.path("createdDateTime").asString(null);
                tarefas.add(new TarefaPlanner(
                        t.path("id").asString(""), t.path("title").asString(""), t.path("bucketId").asString(null),
                        criada == null ? null : OffsetDateTime.parse(criada), t.path("percentComplete").asInt(0), aplicadas));
            }
            return new PlanoPlanner(planId, etiquetas, buckets, tarefas);
        } catch (RestClientException e) {
            // a mensagem do cliente HTTP pode trazer a URL; o plano e o status bastam para achar o problema
            throw new EstadoInvalidoException("Não foi possível ler o plano " + planId + " no Planner (" + resumo(e) + ")");
        }
    }

    private List<JsonNode> paginas(String urlInicial) {
        List<JsonNode> itens = new ArrayList<>();
        String url = urlInicial;
        for (int i = 0; url != null && i < MAXIMO_DE_PAGINAS; i++) {
            JsonNode pagina = get(url);
            pagina.path("value").forEach(itens::add);
            url = pagina.path("@odata.nextLink").asString(null);
        }
        return itens;
    }

    private JsonNode get(String url) {
        return http().get().uri(url).header("Authorization", "Bearer " + token()).retrieve().body(JsonNode.class);
    }

    private synchronized String token() {
        if (token != null && Instant.now().isBefore(tokenVenceEm.minusSeconds(60))) {
            return token;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");
        JsonNode resposta = http().post()
                .uri(LOGIN + tenantId + "/oauth2/v2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
        String novo = resposta == null ? null : resposta.path("access_token").asString(null);
        if (novo == null) {
            throw new EstadoInvalidoException("O Entra ID não devolveu o token de acesso ao Planner");
        }
        token = novo;
        tokenVenceEm = Instant.now().plusSeconds(resposta.path("expires_in").asLong(3000));
        return token;
    }

    private RestClient http() {
        if (http == null) {
            http = RestClient.create();
        }
        return http;
    }

    private static String resumo(RestClientException e) {
        String m = e.getMessage();
        // só o início: "401 Unauthorized", "403 Forbidden"...
        return m == null ? e.getClass().getSimpleName() : m.substring(0, Math.min(40, m.length()));
    }
}
