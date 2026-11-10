package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PlannerSincronizacaoResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PlannerSugestao;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.planner.PlannerFonte;
import dev.denetodev.sgd_api.planner.PlannerFonte.PlanoPlanner;
import dev.denetodev.sgd_api.planner.PlannerFonte.TarefaPlanner;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.repository.PlannerSugestaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê o Planner (pelo Graph ou pela planilha exportada) e abre uma sugestão para cada pessoa da equipe que está numa
 * tarefa: pela etiqueta ("CRIATIVO - Lucas") ou por estar atribuída a ela. Uma sugestão por pessoa e por tarefa. Só cria;
 * nunca remove nem refaz: a pessoa que recusou não é incomodada de novo, e tirar a etiqueta depois não mexe no que já
 * foi aceito.
 */
@Service
public class PlannerSyncService {

    private static final Logger log = LoggerFactory.getLogger(PlannerSyncService.class);

    /** Código de controle no começo do título: BC2610FN_email..., BC2610FO - Email... */
    private static final Pattern CODIGO = Pattern.compile("^\\s*(BC\\d{4}[A-Za-z0-9]{2})(?![A-Za-z0-9])", Pattern.CASE_INSENSITIVE);
    /** Etiquetas de gente: "CRIATIVO - Thais", "HTML - Carol". As demais (URGENTE, HTML ENTRADA...) não são pessoas. */
    private static final Pattern ETIQUETA_DE_PESSOA = Pattern.compile("^(CRIATIVO|HTML)\\s*-\\s*(\\S.*)$", Pattern.CASE_INSENSITIVE);
    /** O que aparece como etiqueta quando a pessoa entrou na sugestão só por estar atribuída à tarefa. */
    static final String ETIQUETA_DA_ATRIBUICAO = "Atribuída a você";

    private final PlannerFonte fonte;
    private final PessoaRepository pessoaRepository;
    private final DemandaRepository demandaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final PlannerSugestaoRepository sugestaoRepository;
    private final Clock clock;
    private final boolean ligado;
    private final int janelaDias;
    private final String diretoria;
    private final Set<String> bucketsIgnorados;

    public PlannerSyncService(PlannerFonte fonte, PessoaRepository pessoaRepository, DemandaRepository demandaRepository,
                              PessoaDemandaRepository pessoaDemandaRepository, PlannerSugestaoRepository sugestaoRepository,
                              Clock clock,
                              @Value("${app.planner.enabled:false}") boolean ligado,
                              @Value("${app.planner.janela-dias:30}") int janelaDias,
                              @Value("${app.planner.diretoria:COE/CRM}") String diretoria,
                              @Value("${app.planner.buckets-ignorados:Implementado Martech}") String bucketsIgnorados) {
        this.fonte = fonte;
        this.pessoaRepository = pessoaRepository;
        this.demandaRepository = demandaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.clock = clock;
        this.ligado = ligado;
        this.janelaDias = janelaDias;
        this.diretoria = diretoria;
        this.bucketsIgnorados = new HashSet<>();
        for (String b : bucketsIgnorados.split(",")) {
            if (!b.isBlank()) {
                this.bucketsIgnorados.add(normalizar(b));
            }
        }
    }

    /** O ritmo vem de app.planner.intervalo-ms (padrão 10 minutos). Desligado ou sem configuração, não faz nada. */
    @Scheduled(fixedDelayString = "${app.planner.intervalo-ms:600000}", initialDelayString = "${app.planner.atraso-inicial-ms:60000}")
    void sincronizarAgendado() {
        if (!ligado || !fonte.configurada()) {
            return;
        }
        try {
            PlannerSincronizacaoResponse r = sincronizar();
            if (r.sugestoesNovas() > 0) {
                log.info("Planner: {} sugestões novas ({} tarefas na janela)", r.sugestoesNovas(), r.tarefasNaJanela());
            }
        } catch (RuntimeException e) {
            // o motivo já vem sem segredos; a próxima rodada tenta de novo
            log.warn("Planner: a sincronização falhou: {}", e.getMessage());
        }
    }

    /** Roda uma sincronização agora (a mesma do agendamento); é o que o botão "Buscar agora" chama. */
    public PlannerSincronizacaoResponse sincronizar() {
        if (!fonte.configurada()) {
            throw new EstadoInvalidoException("A integração com o Planner não está configurada neste ambiente");
        }
        int lidas = 0;
        int naJanela = 0;
        int novas = 0;
        Set<String> semCadastro = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> ambiguos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String planId : fonte.planos()) {
            PlanoPlanner plano = fonte.ler(planId);
            lidas += plano.tarefas().size();
            Resultado r = processar(plano);
            naJanela += r.naJanela;
            novas += r.novas;
            semCadastro.addAll(r.semCadastro);
            ambiguos.addAll(r.ambiguos);
        }
        return new PlannerSincronizacaoResponse(lidas, naJanela, novas, new ArrayList<>(semCadastro), new ArrayList<>(ambiguos));
    }

    /** Processa um plano que já está em mãos (a planilha exportada do Planner). Não depende da integração com o Graph. */
    public PlannerSincronizacaoResponse importar(PlanoPlanner plano) {
        Resultado r = processar(plano);
        return new PlannerSincronizacaoResponse(plano.tarefas().size(), r.naJanela, r.novas,
                new ArrayList<>(r.semCadastro), new ArrayList<>(r.ambiguos));
    }

    record Resultado(int naJanela, int novas, Set<String> semCadastro, Set<String> ambiguos) {
    }

    private record Alvo(Pessoa pessoa, String etiqueta) {
    }

    Resultado processar(PlanoPlanner plano) {
        // etiqueta cadastrada à mão na pessoa: vale para qualquer pessoa e tem prioridade
        Map<String, Pessoa> porEtiqueta = new HashMap<>();
        for (Pessoa p : pessoaRepository.findByEtiquetaPlannerIsNotNull()) {
            if (p.getAprovadoEm() != null && !p.getEtiquetaPlanner().isBlank()) {
                porEtiqueta.put(normalizar(p.getEtiquetaPlanner()), p);
            }
        }
        // o resto do CRM usa o mesmo plano: só a equipe da diretoria configurada entra pelo nome
        Map<String, List<Pessoa>> porNomeExibido = new HashMap<>();
        Map<String, List<Pessoa>> porNomeCompleto = new HashMap<>();
        for (Pessoa p : pessoaRepository.findByStatus(StatusPessoa.ATIVO)) {
            if (p.getAprovadoEm() == null || !daDiretoria(p)) {
                continue;
            }
            porNomeExibido.computeIfAbsent(normalizar(p.getNomeExibido()), k -> new ArrayList<>()).add(p);
            porNomeCompleto.computeIfAbsent(normalizar(p.getNome()), k -> new ArrayList<>()).add(p);
        }

        OffsetDateTime limite = OffsetDateTime.now(clock).minusDays(janelaDias);
        List<TarefaPlanner> naJanela = plano.tarefas().stream()
                .filter(t -> t.percentualConcluido() < 100)
                .filter(t -> !bucketIgnorado(plano, t))
                .filter(t -> t.criadaEm() == null || !t.criadaEm().isBefore(limite))
                .toList();

        Set<String> jaExistentes = new HashSet<>(sugestaoRepository.chavesJaExistentes(
                naJanela.stream().map(TarefaPlanner::id).toList()));

        Set<String> semCadastro = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> ambiguos = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<PlannerSugestao> novas = new ArrayList<>();
        for (TarefaPlanner tarefa : naJanela) {
            String codigo = extrairCodigo(tarefa.titulo());
            // uma sugestão por pessoa: a etiqueta vem antes da atribuição, que só aparece se a pessoa não tem etiqueta
            Map<UUID, Alvo> alvos = new LinkedHashMap<>();
            for (String chave : tarefa.etiquetasAplicadas()) {
                String etiqueta = plano.etiquetas().get(chave);
                if (etiqueta == null) {
                    continue;
                }
                Pessoa pessoa = porEtiqueta.get(normalizar(etiqueta));
                if (pessoa == null) {
                    Matcher m = ETIQUETA_DE_PESSOA.matcher(etiqueta.trim());
                    if (!m.matches()) {
                        continue;
                    }
                    List<Pessoa> candidatas = porNomeExibido.getOrDefault(normalizar(m.group(2)), List.of());
                    if (candidatas.size() > 1) {
                        ambiguos.add(etiqueta);
                        continue;
                    }
                    if (candidatas.isEmpty()) {
                        semCadastro.add(etiqueta);
                        continue;
                    }
                    pessoa = candidatas.get(0);
                }
                alvos.putIfAbsent(pessoa.getId(), new Alvo(pessoa, etiqueta));
            }
            for (String nome : tarefa.responsaveis()) {
                List<Pessoa> candidatas = porNomeCompleto.getOrDefault(normalizar(nome), List.of());
                if (candidatas.size() > 1) {
                    ambiguos.add(nome);
                } else if (candidatas.size() == 1) {
                    Pessoa pessoa = candidatas.get(0);
                    alvos.putIfAbsent(pessoa.getId(), new Alvo(pessoa, ETIQUETA_DA_ATRIBUICAO));
                }
            }
            for (Alvo alvo : alvos.values()) {
                Pessoa pessoa = alvo.pessoa();
                if (jaExistentes.contains(tarefa.id() + "|" + pessoa.getId()) || jaEstaNoTime(codigo, pessoa)) {
                    continue;
                }
                String etiqueta = alvo.etiqueta().length() > 80 ? alvo.etiqueta().substring(0, 80) : alvo.etiqueta();
                novas.add(new PlannerSugestao(plano.planId(), tarefa.id(), pessoa, codigo, tarefa.titulo(),
                        plano.buckets().get(tarefa.bucketId()), etiqueta, tarefa.criadaEm()));
                jaExistentes.add(tarefa.id() + "|" + pessoa.getId());
            }
        }
        sugestaoRepository.saveAll(novas);
        return new Resultado(naJanela.size(), novas.size(), semCadastro, ambiguos);
    }

    /** A tarefa já está num bucket de encerradas (Implementado Martech): o que passou por ele não gera sugestão. */
    private boolean bucketIgnorado(PlanoPlanner plano, TarefaPlanner tarefa) {
        String bucket = plano.buckets().get(tarefa.bucketId());
        return bucket != null && bucketsIgnorados.contains(normalizar(bucket));
    }

    private boolean daDiretoria(Pessoa pessoa) {
        return pessoa.getArea() != null && pessoa.getArea().getDiretoria() != null
                && diretoria.equalsIgnoreCase(pessoa.getArea().getDiretoria().getNome());
    }

    /** A demanda já existe no sistema e a pessoa já está no time dela: não há o que sugerir. */
    private boolean jaEstaNoTime(String codigo, Pessoa pessoa) {
        if (codigo == null) {
            return false;
        }
        for (Demanda d : demandaRepository.findByCodigoIgnoreCase(codigo)) {
            if (pessoaDemandaRepository.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(pessoa.getId(), d.getId()).isPresent()) {
                return true;
            }
        }
        return false;
    }

    static String extrairCodigo(String titulo) {
        if (titulo == null) {
            return null;
        }
        Matcher m = CODIGO.matcher(titulo);
        return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : null;
    }

    /** Sem acento, sem diferença de maiúscula e com espaços e hífens uniformes: "CRIATIVO – Cícero" = "criativo - cicero". */
    static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return semAcento.toLowerCase(Locale.ROOT).replaceAll("[\\u2010-\\u2015]", "-").replaceAll("\\s+", " ").replaceAll("\\s*-\\s*", " - ").trim();
    }
}
