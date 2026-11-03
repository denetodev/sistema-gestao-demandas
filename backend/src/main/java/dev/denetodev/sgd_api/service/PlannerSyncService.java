package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PlannerSincronizacaoResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PlannerSugestao;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lê o Planner e abre uma sugestão para cada pessoa cuja etiqueta está numa tarefa. Só cria; nunca remove nem refaz:
 * a pessoa que recusou não é incomodada de novo, e tirar a etiqueta depois não mexe no que já foi aceito.
 */
@Service
public class PlannerSyncService {

    private static final Logger log = LoggerFactory.getLogger(PlannerSyncService.class);

    /** Código de controle no começo do título: BC2610FN_email..., BC2610FO - Email... */
    private static final Pattern CODIGO = Pattern.compile("^\\s*(BC\\d{4}[A-Za-z0-9]{2})(?![A-Za-z0-9])", Pattern.CASE_INSENSITIVE);
    /** Etiquetas de gente: "CRIATIVO - Thais", "HTML - Carol". As demais (URGENTE, HTML ENTRADA...) não são pessoas. */
    private static final Pattern ETIQUETA_DE_PESSOA = Pattern.compile("^(CRIATIVO|HTML)\\s*-\\s*\\S.*", Pattern.CASE_INSENSITIVE);

    private final PlannerFonte fonte;
    private final PessoaRepository pessoaRepository;
    private final DemandaRepository demandaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final PlannerSugestaoRepository sugestaoRepository;
    private final Clock clock;
    private final boolean ligado;
    private final int janelaDias;

    public PlannerSyncService(PlannerFonte fonte, PessoaRepository pessoaRepository, DemandaRepository demandaRepository,
                              PessoaDemandaRepository pessoaDemandaRepository, PlannerSugestaoRepository sugestaoRepository,
                              Clock clock,
                              @Value("${app.planner.enabled:false}") boolean ligado,
                              @Value("${app.planner.janela-dias:30}") int janelaDias) {
        this.fonte = fonte;
        this.pessoaRepository = pessoaRepository;
        this.demandaRepository = demandaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.sugestaoRepository = sugestaoRepository;
        this.clock = clock;
        this.ligado = ligado;
        this.janelaDias = janelaDias;
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
        for (String planId : fonte.planos()) {
            PlanoPlanner plano = fonte.ler(planId);
            lidas += plano.tarefas().size();
            Resultado r = processar(plano);
            naJanela += r.naJanela;
            novas += r.novas;
            semCadastro.addAll(r.semCadastro);
        }
        return new PlannerSincronizacaoResponse(lidas, naJanela, novas, new ArrayList<>(semCadastro));
    }

    record Resultado(int naJanela, int novas, Set<String> semCadastro) {
    }

    Resultado processar(PlanoPlanner plano) {
        Map<String, Pessoa> porEtiqueta = new HashMap<>();
        for (Pessoa p : pessoaRepository.findByEtiquetaPlannerIsNotNull()) {
            if (p.getAprovadoEm() != null && !p.getEtiquetaPlanner().isBlank()) {
                porEtiqueta.put(normalizar(p.getEtiquetaPlanner()), p);
            }
        }

        OffsetDateTime limite = OffsetDateTime.now(clock).minusDays(janelaDias);
        List<TarefaPlanner> naJanela = plano.tarefas().stream()
                .filter(t -> t.percentualConcluido() < 100)
                .filter(t -> t.criadaEm() == null || !t.criadaEm().isBefore(limite))
                .toList();

        Set<String> jaExistentes = new HashSet<>(sugestaoRepository.chavesJaExistentes(
                naJanela.stream().map(TarefaPlanner::id).toList()));

        Set<String> semCadastro = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<PlannerSugestao> novas = new ArrayList<>();
        for (TarefaPlanner tarefa : naJanela) {
            String codigo = extrairCodigo(tarefa.titulo());
            for (String chave : tarefa.etiquetasAplicadas()) {
                String etiqueta = plano.etiquetas().get(chave);
                if (etiqueta == null) {
                    continue;
                }
                Pessoa pessoa = porEtiqueta.get(normalizar(etiqueta));
                if (pessoa == null) {
                    if (ETIQUETA_DE_PESSOA.matcher(etiqueta).matches()) {
                        semCadastro.add(etiqueta);
                    }
                    continue;
                }
                if (jaExistentes.contains(tarefa.id() + "|" + pessoa.getId()) || jaEstaNoTime(codigo, pessoa)) {
                    continue;
                }
                novas.add(new PlannerSugestao(plano.planId(), tarefa.id(), pessoa, codigo, tarefa.titulo(),
                        plano.buckets().get(tarefa.bucketId()), etiqueta, tarefa.criadaEm()));
                jaExistentes.add(tarefa.id() + "|" + pessoa.getId());
            }
        }
        sugestaoRepository.saveAll(novas);
        return new Resultado(naJanela.size(), novas.size(), semCadastro);
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
