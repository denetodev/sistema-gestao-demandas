package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DevolucaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoEdicaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoRequest;
import dev.denetodev.sgd_api.dto.request.ProrrogacaoRequest;
import dev.denetodev.sgd_api.dto.response.ResumoEmprestimosResponse;
import dev.denetodev.sgd_api.dto.request.RecusarEmprestimoRequest;
import dev.denetodev.sgd_api.dto.response.EmprestimoResponse;
import dev.denetodev.sgd_api.entity.Emprestimo;
import dev.denetodev.sgd_api.entity.EmprestimoItem;
import dev.denetodev.sgd_api.entity.Equipamento;
import dev.denetodev.sgd_api.entity.ControleEquipamento;
import dev.denetodev.sgd_api.entity.Auditoria;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;
import dev.denetodev.sgd_api.entity.TermoEquipamento;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.AuditoriaRepository;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.EmprestimoItemRepository;
import dev.denetodev.sgd_api.repository.EmprestimoRepository;
import dev.denetodev.sgd_api.repository.EquipamentoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.DisponibilidadePeriodo;
import dev.denetodev.sgd_api.service.support.DisponibilidadePeriodo.Uso;
import dev.denetodev.sgd_api.service.support.EquipamentoPermissao;
import dev.denetodev.sgd_api.util.ChaveC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Empréstimo de equipamento. Todo empréstimo passa por autorização, dentro ou fora do prédio; o externo ganha
 * destaque e vai para o topo da fila. Fluxo: AGUARDANDO_AUTORIZACAO, AUTORIZADO, RETIRADO, DEVOLVIDO
 * (ou RECUSADO e CANCELADO). Cada passo grava quem fez e quando, e a auditoria registra a mudança de status.
 */
@Service
@Transactional
public class EmprestimoService {

    /** Filas da tela: ATIVOS são os em andamento; HISTORICO são os encerrados. */
    public enum Grupo { ATIVOS, HISTORICO, TODOS }

    private static final Set<StatusEmprestimo> EM_ANDAMENTO = EnumSet.of(
            StatusEmprestimo.AGUARDANDO_AUTORIZACAO, StatusEmprestimo.AUTORIZADO, StatusEmprestimo.RETIRADO);
    private static final Set<StatusEmprestimo> ENCERRADOS = EnumSet.of(
            StatusEmprestimo.DEVOLVIDO, StatusEmprestimo.RECUSADO, StatusEmprestimo.CANCELADO);

    private final EmprestimoRepository emprestimoRepository;
    private final EmprestimoItemRepository itemRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final EquipamentoService equipamentoService;
    private final PessoaRepository pessoaRepository;
    private final DemandaRepository demandaRepository;
    private final AtividadeRepository atividadeRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final Clock clock;

    public EmprestimoService(
            EmprestimoRepository emprestimoRepository,
            EmprestimoItemRepository itemRepository,
            EquipamentoRepository equipamentoRepository,
            EquipamentoService equipamentoService,
            PessoaRepository pessoaRepository,
            DemandaRepository demandaRepository,
            AtividadeRepository atividadeRepository,
            AuditoriaRepository auditoriaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            Clock clock
    ) {
        this.emprestimoRepository = emprestimoRepository;
        this.itemRepository = itemRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.equipamentoService = equipamentoService;
        this.pessoaRepository = pessoaRepository;
        this.demandaRepository = demandaRepository;
        this.atividadeRepository = atividadeRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.clock = clock;
    }

    // ---------- leitura ----------

    /** Filas da tela: em andamento (padrão), histórico ou todos. */
    @Transactional(readOnly = true)
    public List<EmprestimoResponse> listar(Jwt jwt, Grupo grupo) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirAcesso(usuario);
        OffsetDateTime agora = OffsetDateTime.now(clock);

        Grupo g = grupo == null ? Grupo.ATIVOS : grupo;
        Set<StatusEmprestimo> status = switch (g) {
            case ATIVOS -> EM_ANDAMENTO;
            case HISTORICO -> ENCERRADOS;
            case TODOS -> EnumSet.allOf(StatusEmprestimo.class);
        };
        Comparator<Emprestimo> ordem = status.equals(ENCERRADOS)
                ? Comparator.comparing(Emprestimo::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                : fila();
        return emprestimoRepository.findByStatusIn(status).stream().sorted(ordem)
                .map(e -> paraResponse(e, agora)).toList();
    }

    @Transactional(readOnly = true)
    public EmprestimoResponse buscar(Jwt jwt, UUID id) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirAcesso(usuario);
        return paraResponse(buscarEntidade(id), OffsetDateTime.now(clock));
    }

    /** Contadores do aviso no menu. */
    @Transactional(readOnly = true)
    public ResumoEmprestimosResponse resumo(Jwt jwt) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirAcesso(usuario);
        OffsetDateTime agora = OffsetDateTime.now(clock);
        List<Emprestimo> abertos = emprestimoRepository.findByStatusIn(EM_ANDAMENTO);
        long atrasados = abertos.stream().filter(e -> e.atrasado(agora)).count();
        List<Emprestimo> aguardando = abertos.stream()
                .filter(e -> e.getStatus() == StatusEmprestimo.AGUARDANDO_AUTORIZACAO).toList();
        return new ResumoEmprestimosResponse(aguardando.size(), aguardando.stream().filter(Emprestimo::isExterno).count(), atrasados);
    }

    /** Fila de atenção: aguardando autorização primeiro, externo antes do interno, depois o que vence antes. */
    private Comparator<Emprestimo> fila() {
        return Comparator
                .comparing((Emprestimo e) -> e.getStatus() == StatusEmprestimo.AGUARDANDO_AUTORIZACAO ? 0 : 1)
                .thenComparing(e -> e.isExterno() ? 0 : 1)
                .thenComparing(Emprestimo::getPrevistaDevolucao);
    }

    // ---------- registrar ----------

    public EmprestimoResponse registrar(Jwt jwt, EmprestimoRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(ator);

        if (!request.termoAceito()) {
            throw new EstadoInvalidoException("É preciso confirmar o termo de responsabilidade (OK) para registrar o empréstimo");
        }
        OffsetDateTime agora = OffsetDateTime.now(clock);
        OffsetDateTime retirada = request.retiradaPrevista() == null ? agora : request.retiradaPrevista();
        if (retirada.isBefore(agora.minusMinutes(5))) {
            throw new IllegalArgumentException("A retirada prevista não pode ser no passado");
        }
        if (!request.previstaDevolucao().isAfter(retirada) || !request.previstaDevolucao().isAfter(agora)) {
            throw new IllegalArgumentException("A devolução prevista precisa ser depois da retirada");
        }

        TermoEquipamento termo = equipamentoService.termoVigente();
        Pessoa retirantePessoa = request.retirantePessoaId() == null ? null : pessoaRepository.findById(request.retirantePessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + request.retirantePessoaId()));
        String nome = retirantePessoa != null && (request.retiranteNome() == null || request.retiranteNome().isBlank())
                ? retirantePessoa.getNome() : request.retiranteNome().trim();

        Emprestimo emprestimo = new Emprestimo(nome, request.previstaDevolucao(), ator, termo.getVersao());
        emprestimo.setRetiradaPrevista(retirada);
        emprestimo.setRetirantePessoa(retirantePessoa);
        emprestimo.setRetiranteChaveC(chaveOpcional(request.retiranteChaveC(), retirantePessoa));
        emprestimo.setRetiranteContato(vazioParaNulo(request.retiranteContato()));
        emprestimo.setFinalidade(vazioParaNulo(request.finalidade()));
        emprestimo.setExterno(request.externo());
        emprestimo.setLocalUso(vazioParaNulo(request.localUso()));
        if (request.demandaId() != null) {
            emprestimo.setDemanda(demandaRepository.findById(request.demandaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + request.demandaId())));
        }
        if (request.atividadeId() != null) {
            emprestimo.setAtividade(atividadeRepository.findById(request.atividadeId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada: " + request.atividadeId())));
        }

        reservarItens(emprestimo, request.itens(), agora);
        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        registrarAuditoria(salvo, null, StatusEmprestimo.AGUARDANDO_AUTORIZACAO, ator, null);
        return paraResponse(salvo, agora);
    }

    private void reservarItens(Emprestimo emprestimo, List<EmprestimoRequest.Item> pedidos, OffsetDateTime agora) {
        Map<UUID, Integer> quantidades = new java.util.LinkedHashMap<>();
        for (EmprestimoRequest.Item pedido : pedidos) {
            if (quantidades.putIfAbsent(pedido.equipamentoId(), pedido.quantidade() == null ? 1 : pedido.quantidade()) != null) {
                throw new IllegalArgumentException("O mesmo equipamento aparece mais de uma vez no pedido");
            }
        }
        // trava as linhas: dois pedidos simultâneos não levam a mesma peça no mesmo período
        Map<UUID, Equipamento> equipamentos = equipamentoRepository.travarPorIds(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Equipamento::getId, e -> e));
        Map<UUID, List<EmprestimoItem>> usos = usosAbertos(quantidades.keySet());

        for (Map.Entry<UUID, Integer> pedido : quantidades.entrySet()) {
            Equipamento e = equipamentos.get(pedido.getKey());
            if (e == null) {
                throw new RecursoNaoEncontradoException("Equipamento não encontrado: " + pedido.getKey());
            }
            if (!e.isAtivo()) {
                throw new EstadoInvalidoException(e.getNome() + " está desativado");
            }
            int quantidade = e.getCategoria().getControle() == ControleEquipamento.INDIVIDUAL ? 1 : pedido.getValue();
            conferirPeriodo(e, quantidade, emprestimo.getRetiradaPrevista(), emprestimo.getPrevistaDevolucao(), null, usos, agora);
            emprestimo.getItens().add(new EmprestimoItem(emprestimo, e, quantidade));
        }
    }

    /** Itens ainda não devolvidos de empréstimos que ocupam equipamentos (aguardando, autorizado ou retirado), por equipamento. */
    private Map<UUID, List<EmprestimoItem>> usosAbertos(java.util.Collection<UUID> equipamentoIds) {
        return itemRepository.ocupacao(equipamentoIds, EquipamentoService.OCUPAM).stream()
                .collect(Collectors.groupingBy(i -> i.getEquipamento().getId()));
    }

    /**
     * Confere se cabe {@code quantidade} do equipamento em todo o período [ini, fim). O empréstimo informado em
     * {@code ignorarEmprestimoId} não conta (é ele mesmo, em correção, prorrogação ou entrega antecipada).
     */
    private void conferirPeriodo(Equipamento e, int quantidade, OffsetDateTime ini, OffsetDateTime fim,
                                 UUID ignorarEmprestimoId, Map<UUID, List<EmprestimoItem>> usos, OffsetDateTime agora) {
        List<EmprestimoItem> doEquipamento = usos.getOrDefault(e.getId(), List.of()).stream()
                .filter(i -> ignorarEmprestimoId == null || !i.getEmprestimo().getId().equals(ignorarEmprestimoId))
                .toList();
        List<Uso> lista = doEquipamento.stream().map(i -> DisponibilidadePeriodo.de(i, agora)).toList();
        int livre = DisponibilidadePeriodo.livre(e.getQuantidadeTotal(), lista, ini.toInstant(), fim.toInstant());
        if (quantidade <= livre) {
            return;
        }
        Uso conflito = DisponibilidadePeriodo.primeiroQueEncosta(lista, ini.toInstant(), fim.toInstant());
        if (livre > 0 || conflito == null) {
            throw new EstadoInvalidoException(e.getNome() + ": só há " + livre + " unidade(s) disponível(is) nesse período");
        }
        String quem = doEquipamento.stream().filter(i -> i.getEmprestimo().getId().equals(conflito.emprestimoId()))
                .map(i -> i.getEmprestimo().getRetiranteNome()).findFirst().orElse("outro empréstimo");
        String janela = conflito.fim().equals(Instant.MAX)
                ? "desde " + formatar(conflito.inicio()) + " e ainda não foi devolvido"
                : "de " + formatar(conflito.inicio()) + " a " + formatar(conflito.fim());
        throw new EstadoInvalidoException(e.getNome() + " não está disponível nesse período: já está reservado ou emprestado para "
                + quem + " " + janela);
    }

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private static String formatar(Instant instante) {
        return FORMATO.format(instante.atZone(FUSO));
    }

    // ---------- correção e prorrogação ----------

    /** Corrige dados enquanto o pedido espera autorização; cada campo alterado vai para a auditoria. */
    public EmprestimoResponse editar(Jwt jwt, UUID id, EmprestimoEdicaoRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(ator);
        Emprestimo e = buscarEntidade(id);
        if (e.getStatus() != StatusEmprestimo.AGUARDANDO_AUTORIZACAO) {
            throw new EstadoInvalidoException("Só dá para corrigir um empréstimo que ainda espera autorização. "
                    + "Depois disso, cancele e registre de novo");
        }
        OffsetDateTime agora = OffsetDateTime.now(clock);
        OffsetDateTime retirada = request.retiradaPrevista() == null ? e.getRetiradaPrevista() : request.retiradaPrevista();
        if (!retirada.equals(e.getRetiradaPrevista()) && retirada.isBefore(agora.minusMinutes(5))) {
            throw new IllegalArgumentException("A retirada prevista não pode ser no passado");
        }
        if (!request.previstaDevolucao().isAfter(retirada) || !request.previstaDevolucao().isAfter(agora)) {
            throw new IllegalArgumentException("A devolução prevista precisa ser depois da retirada");
        }
        // mudou o período: precisa continuar cabendo, sem contar o próprio pedido
        List<UUID> ids = e.getItens().stream().map(i -> i.getEquipamento().getId()).toList();
        equipamentoRepository.travarPorIds(ids);
        Map<UUID, List<EmprestimoItem>> usos = usosAbertos(ids);
        for (EmprestimoItem item : e.getItens()) {
            conferirPeriodo(item.getEquipamento(), item.getQuantidade(), retirada, request.previstaDevolucao(), e.getId(), usos, agora);
        }
        String nome = request.retiranteNome().trim();
        String chave = chaveOpcional(request.retiranteChaveC(), e.getRetirantePessoa());

        auditarSeMudou(e, "retirante_nome", e.getRetiranteNome(), nome, ator);
        auditarSeMudou(e, "retirante_chave_c", e.getRetiranteChaveC(), chave, ator);
        auditarSeMudou(e, "retirante_contato", e.getRetiranteContato(), vazioParaNulo(request.retiranteContato()), ator);
        auditarSeMudou(e, "finalidade", e.getFinalidade(), vazioParaNulo(request.finalidade()), ator);
        auditarSeMudou(e, "externo", String.valueOf(e.isExterno()), String.valueOf(request.externo()), ator);
        auditarSeMudou(e, "local_uso", e.getLocalUso(), vazioParaNulo(request.localUso()), ator);
        auditarSeMudou(e, "retirada_prevista", String.valueOf(e.getRetiradaPrevista()), String.valueOf(retirada), ator);
        auditarSeMudou(e, "prevista_devolucao", String.valueOf(e.getPrevistaDevolucao()), String.valueOf(request.previstaDevolucao()), ator);
        UUID demandaAtual = e.getDemanda() == null ? null : e.getDemanda().getId();
        auditarSeMudou(e, "demanda_id", demandaAtual == null ? null : demandaAtual.toString(),
                request.demandaId() == null ? null : request.demandaId().toString(), ator);

        e.setRetiranteNome(nome);
        e.setRetiranteChaveC(chave);
        e.setRetiranteContato(vazioParaNulo(request.retiranteContato()));
        e.setFinalidade(vazioParaNulo(request.finalidade()));
        e.setExterno(request.externo());
        e.setLocalUso(vazioParaNulo(request.localUso()));
        e.setRetiradaPrevista(retirada);
        e.setPrevistaDevolucao(request.previstaDevolucao());
        e.setDemanda(request.demandaId() == null ? null : demandaRepository.findById(request.demandaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + request.demandaId())));
        return paraResponse(e, agora);
    }

    /** Estende a devolução prevista de quem já está com o equipamento. Fica na auditoria, com o motivo. */
    public EmprestimoResponse prorrogar(Jwt jwt, UUID id, ProrrogacaoRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(ator);
        Emprestimo e = buscarEntidade(id);
        exigirStatus(e, StatusEmprestimo.RETIRADO, "prorrogar");
        OffsetDateTime agora = OffsetDateTime.now(clock);
        if (!request.novaPrevista().isAfter(e.getPrevistaDevolucao()) || !request.novaPrevista().isAfter(agora)) {
            throw new IllegalArgumentException("A nova data precisa ser futura e depois da devolução prevista atual");
        }
        List<EmprestimoItem> pendentes = e.getItens().stream().filter(EmprestimoItem::pendente).toList();
        List<UUID> ids = pendentes.stream().map(i -> i.getEquipamento().getId()).toList();
        equipamentoRepository.travarPorIds(ids);
        Map<UUID, List<EmprestimoItem>> usos = usosAbertos(ids);
        for (EmprestimoItem item : pendentes) {
            // se outra pessoa reservou o equipamento logo depois, não dá para estender por cima
            conferirPeriodo(item.getEquipamento(), item.getQuantidade(), e.inicioOcupacao(), request.novaPrevista(), e.getId(), usos, agora);
        }
        Auditoria a = new Auditoria("emprestimo", e.getId(), "prevista_devolucao",
                String.valueOf(e.getPrevistaDevolucao()), String.valueOf(request.novaPrevista()), ator.getId());
        a.setMotivo(vazioParaNulo(request.motivo()));
        auditoriaRepository.save(a);
        e.setPrevistaDevolucao(request.novaPrevista());
        return paraResponse(e, agora);
    }

    private void auditarSeMudou(Emprestimo e, String campo, String anterior, String novo, Pessoa ator) {
        if (!java.util.Objects.equals(anterior, novo)) {
            auditoriaRepository.save(new Auditoria("emprestimo", e.getId(), campo, anterior, novo, ator.getId()));
        }
    }

    // ---------- autorização ----------

    public EmprestimoResponse autorizar(Jwt jwt, UUID id) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirGerencia(ator);
        Emprestimo e = buscarEntidade(id);
        exigirStatus(e, StatusEmprestimo.AGUARDANDO_AUTORIZACAO, "autorizar");
        exigirNaoSerOProprio(ator, e);
        e.setAutorizadoPor(ator);
        e.setAutorizadoEm(OffsetDateTime.now(clock));
        mudarStatus(e, StatusEmprestimo.AUTORIZADO, ator, null);
        return paraResponse(e, OffsetDateTime.now(clock));
    }

    public EmprestimoResponse recusar(Jwt jwt, UUID id, RecusarEmprestimoRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirGerencia(ator);
        Emprestimo e = buscarEntidade(id);
        exigirStatus(e, StatusEmprestimo.AGUARDANDO_AUTORIZACAO, "recusar");
        exigirNaoSerOProprio(ator, e);
        e.setAutorizadoPor(ator);
        e.setAutorizadoEm(OffsetDateTime.now(clock));
        e.setMotivoRecusa(request.motivo().trim());
        mudarStatus(e, StatusEmprestimo.RECUSADO, ator, request.motivo().trim());
        return paraResponse(e, OffsetDateTime.now(clock));
    }

    /** Quem registrou não autoriza o próprio pedido, exceto o Admin (separação de funções). */
    private void exigirNaoSerOProprio(Pessoa ator, Emprestimo e) {
        boolean admin = ator.getPerfil() == dev.denetodev.sgd_api.entity.PerfilPessoa.ADMIN;
        if (!admin && e.getRegistradoPor().getId().equals(ator.getId())) {
            throw new AccessDeniedException("Outra pessoa precisa autorizar um empréstimo que você mesmo registrou");
        }
    }

    // ---------- entrega, devolução e cancelamento ----------

    public EmprestimoResponse retirar(Jwt jwt, UUID id) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(ator);
        Emprestimo e = buscarEntidade(id);
        exigirStatus(e, StatusEmprestimo.AUTORIZADO, "entregar");
        conferirEntrega(e);
        e.setRetiradoPor(ator);
        e.setRetiradoEm(OffsetDateTime.now(clock));
        mudarStatus(e, StatusEmprestimo.RETIRADO, ator, null);
        return paraResponse(e, OffsetDateTime.now(clock));
    }

    public EmprestimoResponse devolver(Jwt jwt, UUID id, DevolucaoRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(ator);
        Emprestimo e = buscarEntidade(id);
        exigirStatus(e, StatusEmprestimo.RETIRADO, "devolver");

        OffsetDateTime agora = OffsetDateTime.now(clock);
        List<EmprestimoItem> pendentes = e.getItens().stream().filter(EmprestimoItem::pendente).toList();
        List<UUID> pedidos = request == null || request.itemIds() == null ? List.of() : request.itemIds();
        List<EmprestimoItem> aDevolver;
        if (pedidos.isEmpty()) {
            aDevolver = pendentes;
        } else {
            Set<UUID> pendentesIds = pendentes.stream().map(EmprestimoItem::getId).collect(Collectors.toSet());
            if (!pendentesIds.containsAll(new HashSet<>(pedidos))) {
                throw new EstadoInvalidoException("Algum item informado não faz parte deste empréstimo ou já foi devolvido");
            }
            aDevolver = pendentes.stream().filter(i -> pedidos.contains(i.getId())).toList();
        }
        aDevolver.forEach(i -> i.setDevolvidoEm(agora));
        String observacoes = request == null ? null : vazioParaNulo(request.observacoes());
        if (observacoes != null) {
            e.setObservacoesDevolucao(observacoes);
        }
        boolean tudoDevolvido = e.getItens().stream().noneMatch(EmprestimoItem::pendente);
        if (tudoDevolvido) {
            e.setDevolvidoPor(ator);
            e.setDevolvidoEm(agora);
            mudarStatus(e, StatusEmprestimo.DEVOLVIDO, ator, observacoes);
        } else {
            auditoriaRepository.save(new Auditoria("emprestimo", e.getId(), "devolucao_parcial", null,
                    aDevolver.size() + " item(ns)", ator.getId()));
        }
        return paraResponse(e, agora);
    }

    public EmprestimoResponse cancelar(Jwt jwt, UUID id) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(ator);
        Emprestimo e = buscarEntidade(id);
        if (e.getStatus() != StatusEmprestimo.AGUARDANDO_AUTORIZACAO && e.getStatus() != StatusEmprestimo.AUTORIZADO) {
            throw new EstadoInvalidoException("Só dá para cancelar antes da entrega do equipamento");
        }
        mudarStatus(e, StatusEmprestimo.CANCELADO, ator, null);
        return paraResponse(e, OffsetDateTime.now(clock));
    }

    // ---------- apoio ----------

    /**
     * Antes de entregar: o equipamento tem que estar fisicamente aqui (o empréstimo anterior pode estar atrasado) e,
     * se a pessoa chegou antes da hora marcada, a retirada adiantada não pode invadir a reserva de outra pessoa.
     */
    private void conferirEntrega(Emprestimo e) {
        OffsetDateTime agora = OffsetDateTime.now(clock);
        List<EmprestimoItem> pendentes = e.getItens().stream().filter(EmprestimoItem::pendente).toList();
        List<UUID> ids = pendentes.stream().map(i -> i.getEquipamento().getId()).toList();
        equipamentoRepository.travarPorIds(ids);

        Map<UUID, List<EmprestimoItem>> comOutros = itemRepository.ocupacao(ids, EnumSet.of(StatusEmprestimo.RETIRADO)).stream()
                .filter(i -> !i.getEmprestimo().getId().equals(e.getId()))
                .collect(Collectors.groupingBy(i -> i.getEquipamento().getId()));
        for (EmprestimoItem item : pendentes) {
            Equipamento eq = item.getEquipamento();
            List<EmprestimoItem> outros = comOutros.getOrDefault(eq.getId(), List.of());
            int comOutras = outros.stream().mapToInt(EmprestimoItem::getQuantidade).sum();
            if (!outros.isEmpty() && eq.getQuantidadeTotal() - comOutras < item.getQuantidade()) {
                Emprestimo dono = outros.get(0).getEmprestimo();
                throw new EstadoInvalidoException(eq.getNome() + " ainda está com " + dono.getRetiranteNome()
                        + " (devolução prevista " + formatar(dono.getPrevistaDevolucao().toInstant()) + "). Registre a devolução antes de entregar");
            }
        }

        if (agora.isBefore(e.getRetiradaPrevista())) {
            Map<UUID, List<EmprestimoItem>> usos = usosAbertos(ids);
            for (EmprestimoItem item : pendentes) {
                try {
                    conferirPeriodo(item.getEquipamento(), item.getQuantidade(), agora, e.getPrevistaDevolucao(), e.getId(), usos, agora);
                } catch (EstadoInvalidoException ex) {
                    throw new EstadoInvalidoException("A retirada está prevista para " + formatar(e.getRetiradaPrevista().toInstant())
                            + " e entregar agora conflita com outra reserva. " + ex.getMessage());
                }
            }
        }
    }

    private void exigirStatus(Emprestimo e, StatusEmprestimo esperado, String acao) {
        if (e.getStatus() != esperado) {
            throw new EstadoInvalidoException("Não dá para " + acao + " um empréstimo com situação " + e.getStatus());
        }
    }

    private void mudarStatus(Emprestimo e, StatusEmprestimo novo, Pessoa ator, String motivo) {
        StatusEmprestimo anterior = e.getStatus();
        e.setStatus(novo);
        registrarAuditoria(e, anterior, novo, ator, motivo);
    }

    private void registrarAuditoria(Emprestimo e, StatusEmprestimo anterior, StatusEmprestimo novo, Pessoa ator, String motivo) {
        Auditoria a = new Auditoria("emprestimo", e.getId(), "status",
                anterior == null ? null : anterior.name(), novo.name(), ator.getId());
        a.setMotivo(vazioParaNulo(motivo));
        auditoriaRepository.save(a);
    }

    private String chaveOpcional(String informada, Pessoa retirantePessoa) {
        String chave = ChaveC.normalizar(informada);
        if (chave == null) {
            return retirantePessoa == null ? null : retirantePessoa.getChaveC();
        }
        if (!ChaveC.valido(chave)) {
            throw new IllegalArgumentException("Chave C inválida: use C ou F seguido de 7 números");
        }
        return chave;
    }

    private static String vazioParaNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private Emprestimo buscarEntidade(UUID id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado: " + id));
    }

    private EmprestimoResponse paraResponse(Emprestimo e, OffsetDateTime agora) {
        List<EmprestimoResponse.Item> itens = new ArrayList<>();
        for (EmprestimoItem i : e.getItens()) {
            Equipamento eq = i.getEquipamento();
            itens.add(new EmprestimoResponse.Item(i.getId(), eq.getId(), eq.getNome(), eq.getPatrimonio(),
                    eq.getCategoria().getNome(), i.getQuantidade(), i.getDevolvidoEm()));
        }
        return new EmprestimoResponse(
                e.getId(), e.getStatus(), e.atrasado(agora), e.isExterno(), e.getLocalUso(),
                e.getRetiranteNome(), e.getRetiranteChaveC(), e.getRetiranteContato(),
                e.getRetirantePessoa() == null ? null : e.getRetirantePessoa().getId(),
                e.getFinalidade(), e.getRetiradaPrevista(), e.getPrevistaDevolucao(),
                e.getDemanda() == null ? null : e.getDemanda().getId(),
                e.getDemanda() == null ? null : e.getDemanda().getTitulo(),
                e.getAtividade() == null ? null : e.getAtividade().getId(),
                e.getRegistradoPor().getId(), e.getRegistradoPor().getNome(),
                e.getTermoVersao(), e.getTermoAceitoEm(),
                e.getAutorizadoPor() == null ? null : e.getAutorizadoPor().getNome(), e.getAutorizadoEm(), e.getMotivoRecusa(),
                e.getRetiradoPor() == null ? null : e.getRetiradoPor().getNome(), e.getRetiradoEm(),
                e.getDevolvidoPor() == null ? null : e.getDevolvidoPor().getNome(), e.getDevolvidoEm(), e.getObservacoesDevolucao(),
                itens, e.getCreatedAt());
    }
}
