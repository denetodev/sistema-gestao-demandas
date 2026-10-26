package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.CampanhaDetalheResponse;
import dev.denetodev.sgd_api.dto.response.CampanhaResumoResponse;
import dev.denetodev.sgd_api.dto.response.DemandaNaCampanhaResponse;
import dev.denetodev.sgd_api.dto.response.PessoaNaCampanhaResponse;
import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Campanha;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.StatusDemanda;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.CampanhaRepository;
import dev.denetodev.sgd_api.repository.CampanhaResumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Resumo por campanha: quantas demandas, pessoas e quanto valor ela reuniu.
 *
 * Demandas canceladas contam no total e no status, mas ficam fora de pessoas, valor, peças e atividades
 * (mesma regra do dashboard). O valor da demanda é a soma das peças; sem peças, só vale o valor da
 * planilha nas demandas importadas.
 */
@Service
@Transactional(readOnly = true)
public class CampanhaResumoService {

    private final CampanhaRepository campanhaRepository;
    private final CampanhaResumoRepository consultas;

    public CampanhaResumoService(CampanhaRepository campanhaRepository, CampanhaResumoRepository consultas) {
        this.campanhaRepository = campanhaRepository;
        this.consultas = consultas;
    }

    /** Todas as campanhas que têm demanda, da que mais gerou valor para a que menos gerou. */
    public List<CampanhaResumoResponse> resumirTodas() {
        List<Demanda> todas = consultas.demandasComCampanha();
        if (todas.isEmpty()) {
            return List.of();
        }
        Dados dados = carregar(todas);
        Map<UUID, List<Demanda>> porCampanha = todas.stream()
                .collect(Collectors.groupingBy(d -> d.getCampanha().getId(), LinkedHashMap::new, Collectors.toList()));
        return porCampanha.values().stream()
                .map(lista -> resumir(lista.get(0).getCampanha(), lista, dados))
                .sorted(Comparator.comparing(CampanhaResumoResponse::valor).reversed()
                        .thenComparing(CampanhaResumoResponse::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public CampanhaDetalheResponse detalhar(UUID campanhaId) {
        Campanha campanha = campanhaRepository.findById(campanhaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Campanha não encontrada: " + campanhaId));
        List<Demanda> demandas = consultas.demandasDaCampanha(campanhaId);
        Dados dados = demandas.isEmpty() ? Dados.vazio() : carregar(demandas);
        CampanhaResumoResponse resumo = resumir(campanha, demandas, dados);
        return new CampanhaDetalheResponse(resumo, pessoasDa(demandas, dados), demandasDa(demandas, dados));
    }

    // ---- agregação ----

    private CampanhaResumoResponse resumir(Campanha campanha, List<Demanda> demandas, Dados dados) {
        List<Demanda> ativas = demandas.stream().filter(CampanhaResumoService::ativa).toList();

        Set<UUID> pessoas = new LinkedHashSet<>();
        int pecas = 0;
        int atividades = 0;
        BigDecimal valor = BigDecimal.ZERO;
        for (Demanda d : ativas) {
            dados.vinculos(d).forEach(v -> pessoas.add(v.getPessoa().getId()));
            pecas += dados.pecas(d).size();
            atividades += dados.atividades(d).size();
            valor = valor.add(valorDa(d, dados));
        }

        Map<StatusDemanda, Integer> porStatus = new EnumMap<>(StatusDemanda.class);
        for (StatusDemanda s : StatusDemanda.values()) {
            porStatus.put(s, 0);
        }
        demandas.forEach(d -> porStatus.merge(d.getStatus(), 1, Integer::sum));

        LocalDate primeira = demandas.stream().map(Demanda::getDataCriacao).filter(x -> x != null)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDate ultima = demandas.stream().map(Demanda::getDataCriacao).filter(x -> x != null)
                .max(Comparator.naturalOrder()).orElse(null);

        return new CampanhaResumoResponse(campanha.getId(), campanha.getNome(), campanha.getCodigo(),
                demandas.size(), ativas.size(), pessoas.size(), valor, pecas, atividades, porStatus, primeira, ultima);
    }

    private List<PessoaNaCampanhaResponse> pessoasDa(List<Demanda> demandas, Dados dados) {
        Map<UUID, Pessoa> pessoas = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> demandasDe = new HashMap<>();
        Map<UUID, Integer> atividadesDe = new HashMap<>();
        Map<UUID, Integer> pecasDe = new HashMap<>();
        Map<UUID, BigDecimal> valorDe = new HashMap<>();

        for (Demanda d : demandas.stream().filter(CampanhaResumoService::ativa).toList()) {
            for (PessoaDemanda v : dados.vinculos(d)) {
                pessoas.putIfAbsent(v.getPessoa().getId(), v.getPessoa());
                demandasDe.computeIfAbsent(v.getPessoa().getId(), k -> new LinkedHashSet<>()).add(d.getId());
            }
            for (Atividade a : dados.atividades(d)) {
                if (a.getPessoa() != null) {
                    pessoas.putIfAbsent(a.getPessoa().getId(), a.getPessoa());
                    atividadesDe.merge(a.getPessoa().getId(), 1, Integer::sum);
                }
            }
            for (Peca p : dados.pecas(d)) {
                if (p.getPessoa() != null) {
                    pessoas.putIfAbsent(p.getPessoa().getId(), p.getPessoa());
                    pecasDe.merge(p.getPessoa().getId(), 1, Integer::sum);
                    valorDe.merge(p.getPessoa().getId(), p.getValorTotal(), BigDecimal::add);
                }
            }
        }

        List<PessoaNaCampanhaResponse> lista = new ArrayList<>();
        pessoas.forEach((id, pessoa) -> lista.add(new PessoaNaCampanhaResponse(
                id, nomeDe(pessoa),
                demandasDe.getOrDefault(id, Set.of()).size(),
                atividadesDe.getOrDefault(id, 0),
                pecasDe.getOrDefault(id, 0),
                valorDe.getOrDefault(id, BigDecimal.ZERO))));
        lista.sort(Comparator.comparing(PessoaNaCampanhaResponse::valor).reversed()
                .thenComparing(PessoaNaCampanhaResponse::nome, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    private List<DemandaNaCampanhaResponse> demandasDa(List<Demanda> demandas, Dados dados) {
        return demandas.stream()
                .sorted(Comparator.comparing(Demanda::getDataCriacao, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(d -> new DemandaNaCampanhaResponse(d.getId(), d.getTitulo(), d.getStatus(), d.getDataCriacao(),
                        valorDa(d, dados)))
                .toList();
    }

    private static BigDecimal valorDa(Demanda d, Dados dados) {
        List<Peca> pecas = dados.pecas(d);
        if (!pecas.isEmpty()) {
            return pecas.stream().map(Peca::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        // sem peças, só vale o valor que veio da planilha (demanda criada no sistema não tem valor manual)
        return d.getSourceSystem() != null && d.getValor() != null ? d.getValor() : BigDecimal.ZERO;
    }

    private static boolean ativa(Demanda d) {
        return d.getStatus() != StatusDemanda.CANCELADA;
    }

    private static String nomeDe(Pessoa p) {
        return p.getNomeExibido();
    }

    private Dados carregar(List<Demanda> demandas) {
        List<UUID> ids = demandas.stream().map(Demanda::getId).toList();
        return new Dados(
                consultas.pecasDasDemandas(ids).stream().collect(Collectors.groupingBy(p -> p.getDemanda().getId())),
                consultas.vinculosDasDemandas(ids).stream().collect(Collectors.groupingBy(v -> v.getDemanda().getId())),
                consultas.atividadesDasDemandas(ids).stream().collect(Collectors.groupingBy(a -> a.getDemanda().getId())));
    }

    /** Peças, vínculos e atividades já agrupados por demanda. */
    private record Dados(
            Map<UUID, List<Peca>> pecas,
            Map<UUID, List<PessoaDemanda>> vinculos,
            Map<UUID, List<Atividade>> atividades
    ) {
        static Dados vazio() {
            return new Dados(Map.of(), Map.of(), Map.of());
        }

        List<Peca> pecas(Demanda d) {
            return pecas.getOrDefault(d.getId(), List.of());
        }

        List<PessoaDemanda> vinculos(Demanda d) {
            return vinculos.getOrDefault(d.getId(), List.of());
        }

        List<Atividade> atividades(Demanda d) {
            return atividades.getOrDefault(d.getId(), List.of());
        }
    }
}
