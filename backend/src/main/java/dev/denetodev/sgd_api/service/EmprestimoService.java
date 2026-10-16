package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DevolucaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoRequest;
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
import dev.denetodev.sgd_api.service.support.EquipamentoPermissao;
import dev.denetodev.sgd_api.util.ChaveC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
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

    /**
     * Quem opera ou gerencia vê todas as filas. Quem só tem acesso enxerga apenas o que está retirado ("quem está
     * com quê"), independentemente do grupo pedido.
     */
    @Transactional(readOnly = true)
    public List<EmprestimoResponse> listar(Jwt jwt, Grupo grupo) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirAcesso(usuario);
        OffsetDateTime agora = OffsetDateTime.now(clock);

        Set<StatusEmprestimo> status;
        if (!EquipamentoPermissao.opera(usuario)) {
            status = EnumSet.of(StatusEmprestimo.RETIRADO);
        } else {
            Grupo g = grupo == null ? Grupo.ATIVOS : grupo;
            status = switch (g) {
                case ATIVOS -> EM_ANDAMENTO;
                case HISTORICO -> ENCERRADOS;
                case TODOS -> EnumSet.allOf(StatusEmprestimo.class);
            };
        }
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
        Emprestimo emprestimo = buscarEntidade(id);
        if (!EquipamentoPermissao.opera(usuario) && emprestimo.getStatus() != StatusEmprestimo.RETIRADO) {
            throw new AccessDeniedException("Sem permissão para ver este empréstimo");
        }
        return paraResponse(emprestimo, OffsetDateTime.now(clock));
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
        if (!request.previstaDevolucao().isAfter(agora)) {
            throw new IllegalArgumentException("A devolução prevista precisa ser uma data futura");
        }

        TermoEquipamento termo = equipamentoService.termoVigente();
        Pessoa retirantePessoa = request.retirantePessoaId() == null ? null : pessoaRepository.findById(request.retirantePessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + request.retirantePessoaId()));
        String nome = retirantePessoa != null && (request.retiranteNome() == null || request.retiranteNome().isBlank())
                ? retirantePessoa.getNome() : request.retiranteNome().trim();

        Emprestimo emprestimo = new Emprestimo(nome, request.previstaDevolucao(), ator, termo.getVersao());
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

        reservarItens(emprestimo, request.itens());
        Emprestimo salvo = emprestimoRepository.save(emprestimo);
        registrarAuditoria(salvo, null, StatusEmprestimo.AGUARDANDO_AUTORIZACAO, ator, null);
        return paraResponse(salvo, agora);
    }

    private void reservarItens(Emprestimo emprestimo, List<EmprestimoRequest.Item> pedidos) {
        Map<UUID, Integer> quantidades = new java.util.LinkedHashMap<>();
        for (EmprestimoRequest.Item pedido : pedidos) {
            if (quantidades.putIfAbsent(pedido.equipamentoId(), pedido.quantidade() == null ? 1 : pedido.quantidade()) != null) {
                throw new IllegalArgumentException("O mesmo equipamento aparece mais de uma vez no pedido");
            }
        }
        // trava as linhas: dois pedidos simultâneos não levam a mesma peça
        Map<UUID, Equipamento> equipamentos = equipamentoRepository.travarPorIds(quantidades.keySet()).stream()
                .collect(Collectors.toMap(Equipamento::getId, e -> e));
        Map<UUID, Integer> ocupado = equipamentoService.ocupado(quantidades.keySet());

        for (Map.Entry<UUID, Integer> pedido : quantidades.entrySet()) {
            Equipamento e = equipamentos.get(pedido.getKey());
            if (e == null) {
                throw new RecursoNaoEncontradoException("Equipamento não encontrado: " + pedido.getKey());
            }
            if (!e.isAtivo()) {
                throw new EstadoInvalidoException(e.getNome() + " está desativado");
            }
            int quantidade = e.getCategoria().getControle() == ControleEquipamento.INDIVIDUAL ? 1 : pedido.getValue();
            int livre = e.getQuantidadeTotal() - ocupado.getOrDefault(e.getId(), 0);
            if (quantidade > livre) {
                throw new EstadoInvalidoException(livre <= 0
                        ? e.getNome() + " não está disponível: já está reservado ou emprestado"
                        : e.getNome() + ": só há " + livre + " unidade(s) disponível(is)");
            }
            emprestimo.getItens().add(new EmprestimoItem(emprestimo, e, quantidade));
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
        EquipamentoPermissao.exigirAcesso(ator);
        Emprestimo e = buscarEntidade(id);
        boolean quemRegistrou = e.getRegistradoPor().getId().equals(ator.getId());
        if (!EquipamentoPermissao.opera(ator) && !quemRegistrou) {
            throw new AccessDeniedException("Sem permissão para cancelar este empréstimo");
        }
        if (e.getStatus() != StatusEmprestimo.AGUARDANDO_AUTORIZACAO && e.getStatus() != StatusEmprestimo.AUTORIZADO) {
            throw new EstadoInvalidoException("Só dá para cancelar antes da entrega do equipamento");
        }
        mudarStatus(e, StatusEmprestimo.CANCELADO, ator, null);
        return paraResponse(e, OffsetDateTime.now(clock));
    }

    // ---------- apoio ----------

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
                e.getFinalidade(), e.getPrevistaDevolucao(),
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
