package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.SalvarRelatorioRequest;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemAtividade;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemDemanda;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemEvidencia;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ResumoPeca;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemPeca;
import dev.denetodev.sgd_api.dto.response.RelatorioResumoResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.EvidenciaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.repository.RelatorioMensalRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Relatório mensal de atividades da pessoa, entregue à empresa (assinado e enviado).
 *
 * Quem pode o quê:
 * - ver: a própria pessoa e Gestor/Admin (de qualquer profissional);
 * - editar a seleção, escrever observações e aprovar: só a própria pessoa (a assinatura é pessoal);
 * - reabrir um relatório aprovado: só Gestor/Admin (depois de aprovado o relatório fica travado para a pessoa);
 * - Visualizador não tem relatório.
 *
 * Ao aprovar, o conteúdo é congelado em JSON: o relatório assinado não muda se atividades
 * forem editadas depois. Reabrir descarta o congelado.
 */
@Service
@Transactional
public class RelatorioMensalService {

    private final CurrentPessoaResolver currentPessoaResolver;
    private final PessoaRepository pessoaRepository;
    private final AtividadeRepository atividadeRepository;
    private final PecaRepository pecaRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final RelatorioMensalRepository relatorioRepository;
    private final JsonMapper jsonMapper;

    public RelatorioMensalService(
            CurrentPessoaResolver currentPessoaResolver,
            PessoaRepository pessoaRepository,
            AtividadeRepository atividadeRepository,
            PecaRepository pecaRepository,
            EvidenciaRepository evidenciaRepository,
            RelatorioMensalRepository relatorioRepository,
            JsonMapper jsonMapper
    ) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.pessoaRepository = pessoaRepository;
        this.atividadeRepository = atividadeRepository;
        this.pecaRepository = pecaRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.relatorioRepository = relatorioRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public RelatorioResponse buscar(Jwt jwt, YearMonth mes, UUID pessoaId) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        return buscar(usuario, mes, pessoaId, YearMonth.now());
    }

    RelatorioResponse buscar(Pessoa usuario, YearMonth mes, UUID pessoaId, YearMonth atual) {
        validarMes(mes, atual);
        Pessoa dono = resolverDono(usuario, pessoaId);
        return montar(usuario, dono, mes);
    }

    public RelatorioResponse salvar(Jwt jwt, YearMonth mes, SalvarRelatorioRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        return salvar(usuario, mes, request, YearMonth.now());
    }

    RelatorioResponse salvar(Pessoa usuario, YearMonth mes, SalvarRelatorioRequest request, YearMonth atual) {
        validarMes(mes, atual);
        exigirProfissional(usuario);
        RelatorioMensal relatorio = obterOuCriar(usuario, mes);
        exigirAberto(relatorio);

        relatorio.setObservacoes(request.observacoes() == null || request.observacoes().isBlank()
                ? null : request.observacoes().trim());
        relatorio.setAtividadesExcluidas(request.atividadesExcluidas() == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(request.atividadesExcluidas()));
        relatorioRepository.save(relatorio);
        return montar(usuario, usuario, mes);
    }

    public RelatorioResponse aprovar(Jwt jwt, YearMonth mes) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        return aprovar(usuario, mes, YearMonth.now());
    }

    RelatorioResponse aprovar(Pessoa usuario, YearMonth mes, YearMonth atual) {
        validarMes(mes, atual);
        exigirProfissional(usuario);
        RelatorioMensal relatorio = obterOuCriar(usuario, mes);
        exigirAberto(relatorio);

        RelatorioResponse conteudo = conteudoAoVivo(usuario, relatorio, mes);
        List<ItemAtividade> incluidas = conteudo.atividades().stream().filter(ItemAtividade::incluida).toList();
        OffsetDateTime agora = OffsetDateTime.now(ZoneOffset.UTC);

        RelatorioResponse congelado = new RelatorioResponse(
                conteudo.pessoaId(), conteudo.pessoaNome(), conteudo.diretoria(), conteudo.area(), conteudo.cargo(),
                conteudo.mes(), StatusRelatorio.APROVADO, agora, conteudo.observacoes(),
                incluidas, conteudo.pecas(), conteudo.valorReferencia(), conteudo.modelo(),
                soIncluidas(conteudo.demandas(), incluidas), false, false);

        relatorio.setSnapshotJson(jsonMapper.writeValueAsString(congelado));
        relatorio.setStatus(StatusRelatorio.APROVADO);
        relatorio.setAprovadoEm(agora);
        relatorioRepository.save(relatorio);
        return congelado.comPermissoes(false, ehAdminOuGestor(usuario));
    }

    public RelatorioResponse reabrir(Jwt jwt, YearMonth mes, UUID pessoaId) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        return reabrir(usuario, mes, pessoaId, YearMonth.now());
    }

    RelatorioResponse reabrir(Pessoa usuario, YearMonth mes, UUID pessoaId, YearMonth atual) {
        validarMes(mes, atual);
        if (!ehAdminOuGestor(usuario)) {
            throw new AccessDeniedException("Só Gestor/Admin reabrem um relatório aprovado");
        }
        Pessoa dono = resolverDono(usuario, pessoaId);
        RelatorioMensal relatorio = relatorioRepository.findByPessoaIdAndMes(dono.getId(), mes.atDay(1))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatório não encontrado para " + mes));
        if (relatorio.getStatus() != StatusRelatorio.APROVADO) {
            throw new EstadoInvalidoException("O relatório de " + mes + " não está aprovado");
        }
        relatorio.setStatus(StatusRelatorio.ABERTO);
        relatorio.setAprovadoEm(null);
        relatorio.setSnapshotJson(null);
        relatorioRepository.save(relatorio);
        return montar(usuario, dono, mes);
    }

    /** Situação do relatório do mês de cada profissional ativo (Gestor/Admin). */
    @Transactional(readOnly = true)
    public List<RelatorioResumoResponse> resumo(Jwt jwt, YearMonth mes) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        return resumo(usuario, mes, YearMonth.now());
    }

    List<RelatorioResumoResponse> resumo(Pessoa usuario, YearMonth mes, YearMonth atual) {
        validarMes(mes, atual);
        if (!ehAdminOuGestor(usuario)) {
            throw new AccessDeniedException("Resumo de relatórios restrito a Gestor/Admin");
        }
        List<Pessoa> profissionais = pessoaRepository.findAll().stream()
                .filter(p -> estavaAtivaNoMes(p, mes)
                        && p.getAprovadoEm() != null
                        && p.getPerfil() == PerfilPessoa.PROFISSIONAL)
                .sorted(Comparator.comparing(Pessoa::getNomeExibido, String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (profissionais.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = profissionais.stream().map(Pessoa::getId).toList();
        Map<UUID, RelatorioMensal> porPessoa = new HashMap<>();
        relatorioRepository.findByMesAndPessoaIdIn(mes.atDay(1), ids)
                .forEach(r -> porPessoa.put(r.getPessoa().getId(), r));
        Map<UUID, Long> atividades = new HashMap<>();
        for (Object[] linha : atividadeRepository.contarPorPessoaEDia(ids, mes.atDay(1), mes.atEndOfMonth())) {
            atividades.merge((UUID) linha[0], (Long) linha[2], Long::sum);
        }

        return profissionais.stream().map(p -> {
            RelatorioMensal r = porPessoa.get(p.getId());
            return new RelatorioResumoResponse(
                    p.getId(), p.getNomeExibido(), p.getArea() != null ? p.getArea().getNome() : null,
                    r == null ? "NAO_INICIADO" : r.getStatus().name(),
                    atividades.getOrDefault(p.getId(), 0L),
                    r != null ? r.getAprovadoEm() : null);
        }).toList();
    }

    /** Ativa hoje, ou inativada depois do mês consultado (quem saiu em setembro ainda aparece até agosto). */
    static boolean estavaAtivaNoMes(Pessoa p, YearMonth mes) {
        if (p.getStatus() == StatusPessoa.ATIVO) {
            return true;
        }
        return p.getStatus() == StatusPessoa.INATIVO
                && p.getInativadoEm() != null
                && mes.isBefore(YearMonth.from(p.getInativadoEm()));
    }

    // ---- montagem ----

    private RelatorioResponse montar(Pessoa usuario, Pessoa dono, YearMonth mes) {
        Optional<RelatorioMensal> existente = relatorioRepository.findByPessoaIdAndMes(dono.getId(), mes.atDay(1));
        boolean propria = usuario.getId().equals(dono.getId());
        boolean gestor = ehAdminOuGestor(usuario);

        if (existente.isPresent() && existente.get().getStatus() == StatusRelatorio.APROVADO) {
            RelatorioResponse congelado = jsonMapper.readValue(existente.get().getSnapshotJson(), RelatorioResponse.class);
            return congelado.comPermissoes(false, gestor);
        }
        RelatorioMensal relatorio = existente.orElse(new RelatorioMensal(dono, mes.atDay(1)));
        return conteudoAoVivo(dono, relatorio, mes).comPermissoes(propria, false);
    }

    private RelatorioResponse conteudoAoVivo(Pessoa dono, RelatorioMensal relatorio, YearMonth mes) {
        Set<UUID> excluidas = relatorio.getAtividadesExcluidas();

        List<Atividade> atividadesDoMes = atividadeRepository
                .findByPessoa_IdAndDataRealizacaoBetweenOrderByDataRealizacaoAsc(dono.getId(), mes.atDay(1), mes.atEndOfMonth());
        List<ItemAtividade> atividades = atividadesDoMes
                .stream()
                .map(a -> new ItemAtividade(
                        a.getId(), a.getDataRealizacao(),
                        a.getDemanda() != null ? a.getDemanda().getId() : null,
                        a.getDemanda() != null ? a.getDemanda().getTitulo() : null,
                        a.getTipoAtividade().getNome(), a.getDescricao(),
                        !excluidas.contains(a.getId())))
                .toList();

        LocalDate de = mes.atDay(1);
        LocalDate ate = mes.atEndOfMonth();
        List<Peca> pecasDoMes = pecaRepository.findProduzidasNoPeriodo(
                List.of(dono.getId()), StatusDemanda.CANCELADA, de, ate,
                de.atStartOfDay().atOffset(ZoneOffset.UTC), ate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC));
        List<ItemPeca> pecas = pecasDoMes.stream()
                .map(p -> new ItemPeca(
                        p.getId(), p.getDemanda().getTitulo(), p.getNome(), p.getTipoPeca().getNome(),
                        p.getQuantidade(), p.getValorUnitario(), p.getValorTotal()))
                .toList();
        BigDecimal valor = pecas.stream().map(ItemPeca::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

        Area area = dono.getArea();
        String cargo = dono.getCargo() != null ? dono.getCargo().getNome() : null;
        return new RelatorioResponse(
                dono.getId(), dono.getNome(),
                area != null ? area.getDiretoria().getNome() : null,
                area != null ? area.getNome() : null,
                cargo,
                mes.toString(), StatusRelatorio.ABERTO, null, relatorio.getObservacoes(),
                atividades, pecas, valor, ModeloRelatorio.de(cargo, area != null ? area.getNome() : null),
                montarDemandas(atividadesDoMes, pecasDoMes), false, false);
    }

    // ---- demandas do mês ----

    /**
     * Agrupa o trabalho do mês por demanda: atividades, peças por tipo e as evidências ligadas a elas.
     * Atividades avulsas (sem demanda) ficam só na lista de atividades.
     */
    private List<ItemDemanda> montarDemandas(List<Atividade> atividades, List<Peca> pecas) {
        Map<UUID, Demanda> demandas = new LinkedHashMap<>();
        Map<UUID, List<Atividade>> atividadesPor = new HashMap<>();
        Map<UUID, List<Peca>> pecasPor = new HashMap<>();
        for (Atividade a : atividades) {
            if (a.getDemanda() == null) continue;
            demandas.putIfAbsent(a.getDemanda().getId(), a.getDemanda());
            atividadesPor.computeIfAbsent(a.getDemanda().getId(), k -> new ArrayList<>()).add(a);
        }
        for (Peca p : pecas) {
            demandas.putIfAbsent(p.getDemanda().getId(), p.getDemanda());
            pecasPor.computeIfAbsent(p.getDemanda().getId(), k -> new ArrayList<>()).add(p);
        }
        if (demandas.isEmpty()) {
            return List.of();
        }

        List<UUID> idsAtividades = atividades.stream().filter(a -> a.getDemanda() != null).map(Atividade::getId).toList();
        List<UUID> idsPecas = pecas.stream().map(Peca::getId).toList();
        List<Evidencia> todas = new ArrayList<>();
        if (!idsAtividades.isEmpty()) todas.addAll(evidenciaRepository.findByAtividadeIdIn(idsAtividades));
        if (!idsPecas.isEmpty()) todas.addAll(evidenciaRepository.findByPecaIdIn(idsPecas));
        Map<UUID, List<Evidencia>> evidenciasPor = new HashMap<>();
        Set<UUID> vistas = new HashSet<>();
        for (Evidencia e : todas) {
            if (!vistas.add(e.getId())) continue;
            Demanda d = e.getAtividade() != null ? e.getAtividade().getDemanda() : e.getPeca().getDemanda();
            if (d != null) evidenciasPor.computeIfAbsent(d.getId(), k -> new ArrayList<>()).add(e);
        }

        return demandas.values().stream().map(d -> {
            List<Atividade> atv = atividadesPor.getOrDefault(d.getId(), List.of());
            List<Peca> pcs = pecasPor.getOrDefault(d.getId(), List.of());
            LocalDate data = atv.stream().map(Atividade::getDataRealizacao).min(Comparator.naturalOrder())
                    .orElseGet(() -> pcs.stream().map(RelatorioMensalService::dataDaPeca).min(Comparator.naturalOrder()).orElse(null));
            Map<String, Integer> porTipo = new LinkedHashMap<>();
            pcs.forEach(p -> porTipo.merge(p.getTipoPeca().getNome(), p.getQuantidade(), Integer::sum));
            return new ItemDemanda(
                    d.getId(), d.getCodigo(), d.getTitulo(), d.getDescricao(), d.getLinkExterno(),
                    d.getDemandante() != null ? d.getDemandante().getNome() : null,
                    d.getDiretoria() != null ? d.getDiretoria().getNome() : null,
                    d.getCampanha() != null ? d.getCampanha().getNome() : null,
                    data,
                    atv.stream().map(Atividade::getId).toList(),
                    pcs.stream().mapToInt(Peca::getQuantidade).sum(),
                    pcs.stream().map(Peca::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add),
                    porTipo.entrySet().stream().map(e -> new ResumoPeca(e.getKey(), e.getValue())).toList(),
                    atv.stream().map(a -> a.getTipoAtividade().getNome()).distinct().toList(),
                    evidenciasPor.getOrDefault(d.getId(), List.of()).stream().map(RelatorioMensalService::paraItem).toList());
        }).sorted(Comparator.comparing(ItemDemanda::data, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(i -> i.codigo() == null ? "" : i.codigo())).toList();
    }

    private static LocalDate dataDaPeca(Peca p) {
        if (p.getDataEntrega() != null) return p.getDataEntrega();
        return p.getCreatedAt() != null ? p.getCreatedAt().toLocalDate() : LocalDate.MAX;
    }

    private static ItemEvidencia paraItem(Evidencia e) {
        return new ItemEvidencia(
                e.getId(),
                e.getAtividade() != null ? e.getAtividade().getId() : null,
                e.getPeca() != null ? e.getPeca().getId() : null,
                e.getTipo(), e.getDescricao(),
                e.getTipo() == TipoEvidencia.LINK ? e.getConteudo() : null,
                e.getArquivoPath() != null && !e.getArquivoPath().isBlank(),
                e.getArquivoMime());
    }

    /** No relatório aprovado ficam só as demandas com atividade incluída ou peça, e só as evidências do que ficou. */
    static List<ItemDemanda> soIncluidas(List<ItemDemanda> demandas, List<ItemAtividade> incluidas) {
        if (demandas == null) return List.of();
        Set<UUID> ids = new HashSet<>();
        incluidas.forEach(a -> ids.add(a.id()));
        return demandas.stream()
                .map(d -> {
                    List<UUID> atv = d.atividadeIds().stream().filter(ids::contains).toList();
                    List<ItemEvidencia> evid = d.evidencias().stream()
                            .filter(e -> e.atividadeId() == null || ids.contains(e.atividadeId())).toList();
                    return new ItemDemanda(d.id(), d.codigo(), d.titulo(), d.descricao(), d.link(), d.demandante(),
                            d.diretoria(), d.campanha(), d.data(), atv, d.quantidadePecas(), d.valor(), d.pecas(),
                            d.naturezas(), evid);
                })
                .filter(d -> !d.atividadeIds().isEmpty() || d.quantidadePecas() > 0)
                .toList();
    }

    // ---- regras ----

    private Pessoa resolverDono(Pessoa usuario, UUID pessoaId) {
        if (pessoaId == null || pessoaId.equals(usuario.getId())) {
            exigirProfissional(usuario);
            return usuario;
        }
        if (!ehAdminOuGestor(usuario)) {
            throw new AccessDeniedException("Só Gestor/Admin consultam relatórios de outras pessoas");
        }
        return pessoaRepository.findById(pessoaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + pessoaId));
    }

    private void exigirProfissional(Pessoa usuario) {
        if (usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            throw new AccessDeniedException("Visualizador não tem relatório mensal");
        }
    }

    private void exigirAberto(RelatorioMensal relatorio) {
        if (relatorio.getStatus() == StatusRelatorio.APROVADO) {
            throw new EstadoInvalidoException("Relatório aprovado: reabra para editar");
        }
    }

    private RelatorioMensal obterOuCriar(Pessoa pessoa, YearMonth mes) {
        return relatorioRepository.findByPessoaIdAndMes(pessoa.getId(), mes.atDay(1))
                .orElseGet(() -> relatorioRepository.save(new RelatorioMensal(pessoa, mes.atDay(1))));
    }

    private void validarMes(YearMonth mes, YearMonth atual) {
        if (mes.isAfter(atual)) {
            throw new IllegalArgumentException("Não é possível gerar relatório de mês futuro: " + mes);
        }
    }

    private static boolean ehAdminOuGestor(Pessoa usuario) {
        return usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR;
    }
}
