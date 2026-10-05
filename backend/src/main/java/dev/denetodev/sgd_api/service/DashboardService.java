package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.DashboardResponse;
import dev.denetodev.sgd_api.dto.response.DashboardResponse.DiaAtividade;
import dev.denetodev.sgd_api.dto.response.DashboardResponse.LinhaArea;
import dev.denetodev.sgd_api.dto.response.DashboardResponse.LinhaPessoa;
import dev.denetodev.sgd_api.dto.response.DashboardResponse.Resumo;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Números do dashboard. A regra de quem vê o quê vive aqui, não no frontend:
 * Profissional vê só os próprios números; Referência de Equipe vê a equipe da
 * área que referencia (agregado ou um colega); Gestor/Admin e Visualizador veem
 * agregado por diretoria, e só Gestor/Admin podem detalhar por pessoa.
 *
 * Valor gerado = soma de quantidade × valor unitário das peças produzidas pela
 * pessoa (cada peça tem um único "produzida por", então somar equipe ou diretoria
 * nunca conta a mesma peça duas vezes). Demandas canceladas ficam fora.
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Set<StatusDemanda> INATIVAS = EnumSet.of(StatusDemanda.CONCLUIDA, StatusDemanda.CANCELADA);

    private final CurrentPessoaResolver currentPessoaResolver;
    private final PessoaRepository pessoaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final DemandaRepository demandaRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final AtividadeRepository atividadeRepository;
    private final PecaRepository pecaRepository;

    public DashboardService(
            CurrentPessoaResolver currentPessoaResolver,
            PessoaRepository pessoaRepository,
            PessoaDemandaRepository pessoaDemandaRepository,
            DemandaRepository demandaRepository,
            DiretoriaRepository diretoriaRepository,
            AtividadeRepository atividadeRepository,
            PecaRepository pecaRepository
    ) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.pessoaRepository = pessoaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.demandaRepository = demandaRepository;
        this.diretoriaRepository = diretoriaRepository;
        this.atividadeRepository = atividadeRepository;
        this.pecaRepository = pecaRepository;
    }

    public DashboardResponse montar(
            Jwt jwt, VisaoDashboard visao, UUID pessoaId, UUID diretoriaId, boolean porPessoa, YearMonth mes
    ) {
        return montar(currentPessoaResolver.resolver(jwt), visao, pessoaId, diretoriaId, porPessoa, mes, LocalDate.now());
    }

    DashboardResponse montar(
            Pessoa usuario, VisaoDashboard visaoPedida, UUID pessoaId, UUID diretoriaId,
            boolean porPessoa, YearMonth mesPedido, LocalDate hoje
    ) {
        boolean adminOuGestor = ehAdminOuGestor(usuario);
        VisaoDashboard visao = visaoPedida != null ? visaoPedida : visaoPadrao(usuario, adminOuGestor);
        YearMonth mes = mesPedido != null ? mesPedido : YearMonth.from(hoje);

        validar(usuario, visao, adminOuGestor, porPessoa);

        List<Pessoa> alvo;
        UUID diretoriaFiltro = null;
        String titulo;
        switch (visao) {
            case MINHA -> {
                alvo = List.of(usuario);
                titulo = "Meus números";
            }
            case EQUIPE -> {
                Area areaRef = usuario.getReferenciaArea();
                List<Pessoa> equipe = elegiveis().stream()
                        .filter(p -> p.getArea() != null && p.getArea().getId().equals(areaRef.getId()))
                        .toList();
                if (pessoaId != null) {
                    Pessoa colega = equipe.stream().filter(p -> p.getId().equals(pessoaId)).findFirst()
                            .orElseThrow(() -> new AccessDeniedException("Pessoa fora da equipe que você referencia"));
                    alvo = List.of(colega);
                    titulo = "Individual: " + colega.getNome();
                } else {
                    alvo = equipe;
                    titulo = "Equipe " + areaRef.getNome() + " (agregado, " + equipe.size() + " pessoas)";
                }
            }
            default -> {
                // Visualizador sempre fica preso à própria diretoria
                if (usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
                    if (usuario.getArea() == null) {
                        throw new AccessDeniedException("Visualizador sem área não tem diretoria para consultar");
                    }
                    diretoriaFiltro = usuario.getArea().getDiretoria().getId();
                } else {
                    diretoriaFiltro = diretoriaId;
                }
                UUID filtro = diretoriaFiltro;
                alvo = elegiveis().stream()
                        .filter(p -> filtro == null || (p.getArea() != null && p.getArea().getDiretoria().getId().equals(filtro)))
                        .toList();
                titulo = filtro == null ? "Todas as diretorias (agregado)" : "Diretoria " + diretoriaRepository.findById(filtro).map(Diretoria::getNome).orElse("") + " (agregado)";
            }
        }

        List<UUID> ids = alvo.stream().map(Pessoa::getId).toList();

        Map<UUID, Set<Demanda>> demandasPorPessoa = new HashMap<>();
        Map<UUID, Demanda> demandasDistintas = new LinkedHashMap<>();
        if (!ids.isEmpty()) {
            for (PessoaDemanda pd : pessoaDemandaRepository.findVigentesPorPessoas(ids)) {
                demandasPorPessoa.computeIfAbsent(pd.getPessoa().getId(), k -> new LinkedHashSet<>()).add(pd.getDemanda());
                demandasDistintas.putIfAbsent(pd.getDemanda().getId(), pd.getDemanda());
            }
        }
        // Na visão por diretoria o resumo conta as demandas da diretoria, não só as das pessoas elegíveis
        Collection<Demanda> demandasResumo = visao == VisaoDashboard.DIRETORIA
                ? (diretoriaFiltro == null ? demandaRepository.findAll() : demandaRepository.findByDiretoriaId(diretoriaFiltro))
                : demandasDistintas.values();

        // Atividades: cobre a janela de 12 meses do calendário e o mês pedido
        LocalDate inicioCalendario = hoje.minusDays(364);
        LocalDate de = min(inicioCalendario, mes.atDay(1));
        LocalDate ate = max(hoje, mes.atEndOfMonth());
        Map<LocalDate, Long> calendario = new TreeMap<>();
        Map<UUID, Long> atividadesMesPorPessoa = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] linha : atividadeRepository.contarPorPessoaEDia(ids, de, ate)) {
                UUID pessoa = (UUID) linha[0];
                LocalDate dia = (LocalDate) linha[1];
                long qtd = (Long) linha[2];
                if (!dia.isBefore(inicioCalendario) && !dia.isAfter(hoje)) {
                    calendario.merge(dia, qtd, Long::sum);
                }
                if (YearMonth.from(dia).equals(mes)) {
                    atividadesMesPorPessoa.merge(pessoa, qtd, Long::sum);
                }
            }
        }

        // Valor gerado: peças do ano do mês pedido
        Map<UUID, BigDecimal> valorMesPorPessoa = new HashMap<>();
        BigDecimal valorAno = BigDecimal.ZERO;
        BigDecimal valorMes = BigDecimal.ZERO;
        if (!ids.isEmpty()) {
            LocalDate inicioAno = LocalDate.of(mes.getYear(), 1, 1);
            LocalDate fimAno = LocalDate.of(mes.getYear(), 12, 31);
            List<Peca> pecas = pecaRepository.findProduzidasNoPeriodo(
                    ids, StatusDemanda.CANCELADA, inicioAno, fimAno,
                    inicioAno.atStartOfDay().atOffset(ZoneOffset.UTC),
                    fimAno.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
            for (Peca peca : pecas) {
                BigDecimal total = peca.getValorTotal();
                valorAno = valorAno.add(total);
                if (YearMonth.from(dataDaPeca(peca)).equals(mes)) {
                    valorMes = valorMes.add(total);
                    valorMesPorPessoa.merge(peca.getPessoa().getId(), total, BigDecimal::add);
                }
            }
        }

        Map<StatusDemanda, Long> porStatus = new EnumMap<>(StatusDemanda.class);
        for (StatusDemanda s : StatusDemanda.values()) {
            porStatus.put(s, 0L);
        }
        demandasResumo.forEach(d -> porStatus.merge(d.getStatus(), 1L, Long::sum));
        long atrasadas = demandasResumo.stream().filter(d -> atrasada(d, hoje)).count();
        long atividadesMes = atividadesMesPorPessoa.values().stream().mapToLong(Long::longValue).sum();

        Resumo resumo = new Resumo(alvo.size(), porStatus, atrasadas, atividadesMes, valorMes, valorAno);

        List<DiaAtividade> dias = calendario.entrySet().stream()
                .map(e -> new DiaAtividade(e.getKey(), e.getValue()))
                .toList();

        List<LinhaArea> linhasArea = visao == VisaoDashboard.DIRETORIA
                ? porArea(alvo, demandasPorPessoa, atividadesMesPorPessoa, valorMesPorPessoa, hoje)
                : List.of();
        List<LinhaPessoa> linhasPessoa = porPessoa
                ? alvo.stream()
                        .sorted(Comparator.comparing(Pessoa::getNome, String.CASE_INSENSITIVE_ORDER))
                        .map(p -> new LinhaPessoa(
                                p.getId(), p.getNome(), nomeArea(p), nomeDiretoriaDe(p),
                                ativas(demandasPorPessoa.get(p.getId())).count(),
                                atividadesMesPorPessoa.getOrDefault(p.getId(), 0L),
                                valorMesPorPessoa.getOrDefault(p.getId(), BigDecimal.ZERO)))
                        .toList()
                : List.of();

        return new DashboardResponse(visao, titulo, mes.toString(), resumo, dias, linhasArea, linhasPessoa);
    }

    private List<LinhaArea> porArea(
            List<Pessoa> alvo, Map<UUID, Set<Demanda>> demandasPorPessoa,
            Map<UUID, Long> atividadesMes, Map<UUID, BigDecimal> valorMes, LocalDate hoje
    ) {
        Map<Area, List<Pessoa>> porArea = alvo.stream()
                .filter(p -> p.getArea() != null)
                .collect(Collectors.groupingBy(Pessoa::getArea, LinkedHashMap::new, Collectors.toList()));

        return porArea.entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().getNome(), String.CASE_INSENSITIVE_ORDER))
                .map(e -> {
                    List<Pessoa> pessoas = e.getValue();
                    Map<UUID, Demanda> demandas = new LinkedHashMap<>();
                    pessoas.forEach(p -> demandasPorPessoa.getOrDefault(p.getId(), Set.of())
                            .forEach(d -> demandas.putIfAbsent(d.getId(), d)));
                    return new LinhaArea(
                            e.getKey().getId(), e.getKey().getNome(), pessoas.size(),
                            ativas(demandas.values()).count(),
                            pessoas.stream().mapToLong(p -> atividadesMes.getOrDefault(p.getId(), 0L)).sum(),
                            demandas.values().stream().filter(d -> atrasada(d, hoje)).count(),
                            pessoas.stream().map(p -> valorMes.getOrDefault(p.getId(), BigDecimal.ZERO))
                                    .reduce(BigDecimal.ZERO, BigDecimal::add));
                })
                .toList();
    }

    private void validar(Pessoa usuario, VisaoDashboard visao, boolean adminOuGestor, boolean porPessoa) {
        PerfilPessoa perfil = usuario.getPerfil();
        if (visao == VisaoDashboard.MINHA && perfil == PerfilPessoa.VISUALIZADOR) {
            throw new AccessDeniedException("Visualizador não tem 'meus números'");
        }
        if (visao == VisaoDashboard.EQUIPE && usuario.getReferenciaArea() == null) {
            throw new AccessDeniedException("Visão 'equipe' exige ser Referência de uma Área");
        }
        if (visao == VisaoDashboard.DIRETORIA && perfil == PerfilPessoa.PROFISSIONAL) {
            throw new AccessDeniedException("Visão 'diretoria' restrita a Gestor, Admin e Visualizador");
        }
        if (porPessoa && !adminOuGestor) {
            throw new AccessDeniedException("Detalhamento por pessoa restrito a Gestor/Admin");
        }
    }

    private VisaoDashboard visaoPadrao(Pessoa usuario, boolean adminOuGestor) {
        if (adminOuGestor || usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            return VisaoDashboard.DIRETORIA;
        }
        return usuario.getReferenciaArea() != null ? VisaoDashboard.EQUIPE : VisaoDashboard.MINHA;
    }

    /** Profissionais ativos e já aprovados: quem entra nos agregados. */
    private List<Pessoa> elegiveis() {
        return pessoaRepository.findAll().stream()
                .filter(p -> p.getStatus() == StatusPessoa.ATIVO
                        && p.getAprovadoEm() != null
                        && p.getPerfil() == PerfilPessoa.PROFISSIONAL)
                .toList();
    }

    private static boolean ehAdminOuGestor(Pessoa usuario) {
        return usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR;
    }

    private static boolean atrasada(Demanda d, LocalDate hoje) {
        return d.getDataPrazo() != null && d.getDataPrazo().isBefore(hoje) && !INATIVAS.contains(d.getStatus());
    }

    private static Stream<Demanda> ativas(Collection<Demanda> demandas) {
        return demandas == null ? Stream.empty()
                : demandas.stream().filter(d -> !INATIVAS.contains(d.getStatus()));
    }

    private static LocalDate dataDaPeca(Peca peca) {
        if (peca.getDataEntrega() != null) {
            return peca.getDataEntrega();
        }
        OffsetDateTime criada = peca.getCreatedAt();
        return criada != null ? criada.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate() : LocalDate.MIN;
    }

    private static String nomeArea(Pessoa p) {
        return p.getArea() != null ? p.getArea().getNome() : null;
    }

    private static String nomeDiretoriaDe(Pessoa p) {
        return p.getArea() != null ? p.getArea().getDiretoria().getNome() : null;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
