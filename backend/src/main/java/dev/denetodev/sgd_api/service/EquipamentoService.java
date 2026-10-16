package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.CategoriaEquipamentoRequest;
import dev.denetodev.sgd_api.dto.request.EquipamentoRequest;
import dev.denetodev.sgd_api.dto.request.TermoEquipamentoRequest;
import dev.denetodev.sgd_api.dto.response.CategoriaEquipamentoResponse;
import dev.denetodev.sgd_api.dto.response.EquipamentoResponse;
import dev.denetodev.sgd_api.dto.response.TermoEquipamentoResponse;
import dev.denetodev.sgd_api.entity.CategoriaEquipamento;
import dev.denetodev.sgd_api.entity.ControleEquipamento;
import dev.denetodev.sgd_api.entity.Equipamento;
import dev.denetodev.sgd_api.entity.EmprestimoItem;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;
import dev.denetodev.sgd_api.entity.TermoEquipamento;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.CategoriaEquipamentoRepository;
import dev.denetodev.sgd_api.repository.EmprestimoItemRepository;
import dev.denetodev.sgd_api.repository.EquipamentoRepository;
import dev.denetodev.sgd_api.repository.TermoEquipamentoRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.EquipamentoPermissao;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Inventário: categorias, equipamentos e o texto do termo. Ler exige acesso ao módulo; editar exige gerência. */
@Service
@Transactional
public class EquipamentoService {

    static final java.util.Set<StatusEmprestimo> OCUPAM =
            EnumSet.of(StatusEmprestimo.AGUARDANDO_AUTORIZACAO, StatusEmprestimo.AUTORIZADO, StatusEmprestimo.RETIRADO);

    private final CategoriaEquipamentoRepository categoriaRepository;
    private final EquipamentoRepository equipamentoRepository;
    private final EmprestimoItemRepository itemRepository;
    private final TermoEquipamentoRepository termoRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final Clock clock;

    public EquipamentoService(
            CategoriaEquipamentoRepository categoriaRepository,
            EquipamentoRepository equipamentoRepository,
            EmprestimoItemRepository itemRepository,
            TermoEquipamentoRepository termoRepository,
            CurrentPessoaResolver currentPessoaResolver,
            Clock clock
    ) {
        this.categoriaRepository = categoriaRepository;
        this.equipamentoRepository = equipamentoRepository;
        this.itemRepository = itemRepository;
        this.termoRepository = termoRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.clock = clock;
    }

    // ---------- categorias ----------

    @Transactional(readOnly = true)
    public List<CategoriaEquipamentoResponse> listarCategorias(Jwt jwt) {
        EquipamentoPermissao.exigirAcesso(currentPessoaResolver.resolver(jwt));
        return categoriaRepository.findAllByOrderByNomeAsc().stream().map(this::paraResponse).toList();
    }

    public CategoriaEquipamentoResponse criarCategoria(Jwt jwt, CategoriaEquipamentoRequest request) {
        EquipamentoPermissao.exigirGerencia(currentPessoaResolver.resolver(jwt));
        String nome = request.nome().trim();
        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new EstadoInvalidoException("Já existe uma categoria com esse nome");
        }
        return paraResponse(categoriaRepository.save(new CategoriaEquipamento(nome, request.controle())));
    }

    public CategoriaEquipamentoResponse atualizarCategoria(Jwt jwt, UUID id, CategoriaEquipamentoRequest request) {
        EquipamentoPermissao.exigirGerencia(currentPessoaResolver.resolver(jwt));
        CategoriaEquipamento categoria = buscarCategoria(id);
        String nome = request.nome().trim();
        if (!categoria.getNome().equalsIgnoreCase(nome) && categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new EstadoInvalidoException("Já existe uma categoria com esse nome");
        }
        if (categoria.getControle() != request.controle()
                && equipamentoRepository.findAll().stream().anyMatch(e -> e.getCategoria().equals(categoria))) {
            throw new EstadoInvalidoException("Não dá para trocar o tipo de controle de uma categoria que já tem equipamentos");
        }
        categoria.setNome(nome);
        categoria.setControle(request.controle());
        return paraResponse(categoria);
    }

    public void desativarCategoria(Jwt jwt, UUID id) {
        EquipamentoPermissao.exigirGerencia(currentPessoaResolver.resolver(jwt));
        buscarCategoria(id).setAtivo(false);
    }

    // ---------- equipamentos ----------

    @Transactional(readOnly = true)
    public List<EquipamentoResponse> listar(Jwt jwt, UUID categoriaId, String busca) {
        EquipamentoPermissao.exigirAcesso(currentPessoaResolver.resolver(jwt));
        String termo = busca == null ? "" : busca.trim().toLowerCase();
        List<Equipamento> equipamentos = equipamentoRepository.findAllByOrderByNomeAsc().stream()
                .filter(Equipamento::isAtivo)
                .filter(e -> categoriaId == null || e.getCategoria().getId().equals(categoriaId))
                .filter(e -> termo.isEmpty() || contem(e, termo))
                .toList();
        return paraResponses(equipamentos);
    }

    @Transactional(readOnly = true)
    public EquipamentoResponse buscar(Jwt jwt, UUID id) {
        EquipamentoPermissao.exigirAcesso(currentPessoaResolver.resolver(jwt));
        return paraResponses(List.of(buscarEntidade(id))).get(0);
    }

    public EquipamentoResponse criar(Jwt jwt, EquipamentoRequest request) {
        EquipamentoPermissao.exigirGerencia(currentPessoaResolver.resolver(jwt));
        CategoriaEquipamento categoria = buscarCategoriaAtiva(request.categoriaId());
        Equipamento equipamento = new Equipamento(categoria, request.nome().trim());
        aplicar(equipamento, categoria, request, 0);
        return paraResponses(List.of(equipamentoRepository.save(equipamento))).get(0);
    }

    public EquipamentoResponse atualizar(Jwt jwt, UUID id, EquipamentoRequest request) {
        EquipamentoPermissao.exigirGerencia(currentPessoaResolver.resolver(jwt));
        Equipamento equipamento = buscarEntidade(id);
        CategoriaEquipamento categoria = buscarCategoriaAtiva(request.categoriaId());
        if (!equipamento.getCategoria().equals(categoria)
                && equipamento.getCategoria().getControle() != categoria.getControle()) {
            throw new EstadoInvalidoException("Não dá para mudar para uma categoria com outro tipo de controle");
        }
        equipamento.setNome(request.nome().trim());
        equipamento.setCategoria(categoria);
        aplicar(equipamento, categoria, request, ocupado(List.of(equipamento.getId())).getOrDefault(equipamento.getId(), 0));
        return paraResponses(List.of(equipamento)).get(0);
    }

    public void desativar(Jwt jwt, UUID id) {
        EquipamentoPermissao.exigirGerencia(currentPessoaResolver.resolver(jwt));
        Equipamento equipamento = buscarEntidade(id);
        if (ocupado(List.of(id)).getOrDefault(id, 0) > 0) {
            throw new EstadoInvalidoException("Este equipamento está em um empréstimo em aberto. Conclua a devolução antes de desativar");
        }
        equipamento.setAtivo(false);
    }

    // ---------- termo ----------

    @Transactional(readOnly = true)
    public TermoEquipamentoResponse termoAtual(Jwt jwt) {
        EquipamentoPermissao.exigirAcesso(currentPessoaResolver.resolver(jwt));
        return paraResponse(termoVigente());
    }

    /** Nunca edita um texto já aceito: cria uma versão nova, e os empréstimos antigos continuam apontando para a que foi aceita. */
    public TermoEquipamentoResponse publicarTermo(Jwt jwt, TermoEquipamentoRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirGerencia(ator);
        int proxima = termoRepository.findTopByOrderByVersaoDesc().map(t -> t.getVersao() + 1).orElse(1);
        return paraResponse(termoRepository.save(new TermoEquipamento(proxima, request.texto().trim(), ator.getId())));
    }

    TermoEquipamento termoVigente() {
        return termoRepository.findTopByOrderByVersaoDesc()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Termo de responsabilidade não cadastrado"));
    }

    // ---------- apoio ----------

    private void aplicar(Equipamento equipamento, CategoriaEquipamento categoria, EquipamentoRequest request, int emUso) {
        String patrimonio = vazioParaNulo(request.patrimonio());
        if (patrimonio != null) {
            equipamentoRepository.findByPatrimonioIgnoreCase(patrimonio)
                    .filter(outro -> !outro.equals(equipamento))
                    .ifPresent(outro -> {
                        throw new EstadoInvalidoException("Esse patrimônio já está cadastrado em outro equipamento");
                    });
        }
        int total;
        if (categoria.getControle() == ControleEquipamento.INDIVIDUAL) {
            total = 1;
        } else {
            if (request.quantidadeTotal() == null) {
                throw new IllegalArgumentException("Informe a quantidade: essa categoria é controlada por quantidade");
            }
            total = request.quantidadeTotal();
        }
        if (total < emUso) {
            throw new EstadoInvalidoException("Há " + emUso + " unidade(s) em empréstimo: a quantidade total não pode ser menor");
        }
        equipamento.setPatrimonio(patrimonio);
        equipamento.setNumeroSerie(vazioParaNulo(request.numeroSerie()));
        equipamento.setQuantidadeTotal(total);
        equipamento.setLocalizacao(vazioParaNulo(request.localizacao()));
        equipamento.setObservacoes(vazioParaNulo(request.observacoes()));
    }

    /** Unidades ocupadas por empréstimo em aberto, por equipamento. */
    Map<UUID, Integer> ocupado(Collection<UUID> ids) {
        return itemRepository.ocupacao(ids, OCUPAM).stream()
                .collect(Collectors.groupingBy(i -> i.getEquipamento().getId(), Collectors.summingInt(EmprestimoItem::getQuantidade)));
    }

    private List<EquipamentoResponse> paraResponses(List<Equipamento> equipamentos) {
        if (equipamentos.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = equipamentos.stream().map(Equipamento::getId).toList();
        Map<UUID, List<EmprestimoItem>> usos = itemRepository.ocupacao(ids, OCUPAM).stream()
                .collect(Collectors.groupingBy(i -> i.getEquipamento().getId()));
        OffsetDateTime agora = OffsetDateTime.now(clock);
        return equipamentos.stream().map(e -> {
            List<EmprestimoItem> doItem = usos.getOrDefault(e.getId(), List.of());
            int ocupadas = doItem.stream().mapToInt(EmprestimoItem::getQuantidade).sum();
            List<EquipamentoResponse.UsoAtual> emUso = doItem.stream()
                    .sorted(Comparator.comparing(i -> i.getEmprestimo().getPrevistaDevolucao()))
                    .map(i -> new EquipamentoResponse.UsoAtual(
                            i.getEmprestimo().getId(), i.getEmprestimo().getRetiranteNome(), i.getQuantidade(),
                            i.getEmprestimo().getPrevistaDevolucao(), i.getEmprestimo().getStatus(),
                            i.getEmprestimo().atrasado(agora), i.getEmprestimo().isExterno(), i.getEmprestimo().getLocalUso()))
                    .toList();
            return new EquipamentoResponse(
                    e.getId(), e.getCategoria().getId(), e.getCategoria().getNome(), e.getCategoria().getControle(),
                    e.getNome(), e.getPatrimonio(), e.getNumeroSerie(),
                    e.getQuantidadeTotal(), Math.max(0, e.getQuantidadeTotal() - ocupadas),
                    e.getLocalizacao(), e.getObservacoes(), e.isAtivo(), emUso);
        }).toList();
    }

    private boolean contem(Equipamento e, String termo) {
        return Arrays.asList(e.getNome(), e.getPatrimonio(), e.getNumeroSerie(), e.getLocalizacao()).stream()
                .anyMatch(campo -> campo != null && campo.toLowerCase().contains(termo));
    }

    private CategoriaEquipamento buscarCategoria(UUID id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }

    private CategoriaEquipamento buscarCategoriaAtiva(UUID id) {
        CategoriaEquipamento categoria = buscarCategoria(id);
        if (!categoria.isAtivo()) {
            throw new EstadoInvalidoException("Essa categoria está desativada");
        }
        return categoria;
    }

    Equipamento buscarEntidade(UUID id) {
        return equipamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado: " + id));
    }

    private static String vazioParaNulo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    private CategoriaEquipamentoResponse paraResponse(CategoriaEquipamento c) {
        return new CategoriaEquipamentoResponse(c.getId(), c.getNome(), c.getControle(), c.isAtivo());
    }

    private TermoEquipamentoResponse paraResponse(TermoEquipamento t) {
        return new TermoEquipamentoResponse(t.getVersao(), t.getTexto());
    }
}
