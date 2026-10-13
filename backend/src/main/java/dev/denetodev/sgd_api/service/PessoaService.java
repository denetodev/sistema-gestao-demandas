package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.*;
import dev.denetodev.sgd_api.dto.response.MeResponse;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.AuditoriaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.ConfirmacaoEmail;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.security.LimitadorTentativas;
import dev.denetodev.sgd_api.util.ChaveC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.denetodev.sgd_api.dto.request.RejeitarRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final ConfirmacaoEmail confirmacaoEmail;
    private final LimitadorTentativas limitadorTentativas;

    /** Mesma resposta para qualquer chave C que não pode ser usado, para o cadastro não virar consulta de chaves C. */
    static final String MSG_CADASTRO_NAO_CONCLUIDO =
            "Não foi possível concluir o cadastro com os dados informados. Procure um Gestor ou Administrador.";

    public PessoaService(
            PessoaRepository pessoaRepository,
            AreaRepository areaRepository,
            CargoRepository cargoRepository,
            AuditoriaRepository auditoriaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            ConfirmacaoEmail confirmacaoEmail,
            LimitadorTentativas limitadorTentativas
    ) {
        this.pessoaRepository = pessoaRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.confirmacaoEmail = confirmacaoEmail;
        this.limitadorTentativas = limitadorTentativas;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listarTodas() {
        return pessoaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listarPendentes() {
        return pessoaRepository.findByAprovadoEmIsNullAndStatus(StatusPessoa.ATIVO).stream()
                .map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public PessoaResponse criar(Jwt jwt, PessoaRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        PerfilPessoa perfilAlvo = request.perfil() != null ? request.perfil() : PerfilPessoa.PROFISSIONAL;
        checarPermissaoSobrePerfil(ator, perfilAlvo, null);

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        Pessoa pessoa = new Pessoa(request.nome(), area);
        pessoa.setEmail(request.email());
        aplicarChaveC(pessoa, request.chaveC());
        pessoa.setCargo(resolverCargo(request.cargoId()));
        if (request.status() == StatusPessoa.REJEITADO) {
            throw new EstadoInvalidoException("Use o endpoint de rejeição para recusar um cadastro pendente");
        }
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }
        pessoa.setPerfil(perfilAlvo);
        pessoa.setReferenciaArea(resolverReferenciaArea(request.referenciaAreaId()));
        pessoa.setAprovadoEm(OffsetDateTime.now());
        pessoa.setAprovadoPor(ator);

        return paraResponse(pessoaRepository.save(pessoa));
    }

    public PessoaResponse atualizar(Jwt jwt, UUID id, PessoaRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        Pessoa pessoa = buscarEntidade(id);
        PerfilPessoa perfilAlvo = request.perfil() != null ? request.perfil() : pessoa.getPerfil();
        checarPermissaoSobrePerfil(ator, perfilAlvo, pessoa);

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        pessoa.setNome(request.nome());
        pessoa.setEmail(request.email());
        aplicarChaveC(pessoa, request.chaveC());
        pessoa.setArea(area);
        pessoa.setCargo(resolverCargo(request.cargoId()));
        if (request.status() == StatusPessoa.REJEITADO) {
            throw new EstadoInvalidoException("Use o endpoint de rejeição para recusar um cadastro pendente");
        }
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }
        pessoa.setPerfil(perfilAlvo);
        pessoa.setReferenciaArea(resolverReferenciaArea(request.referenciaAreaId()));

        return paraResponse(pessoa);
    }

    public void desativar(Jwt jwt, UUID id) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        Pessoa pessoa = buscarEntidade(id);
        checarPermissaoSobrePerfil(ator, pessoa.getPerfil(), pessoa);
        pessoa.setStatus(StatusPessoa.INATIVO);
    }

    /** Ligação direta de uma credencial a uma pessoa (só Admin). Confere conta livre, pessoa sem conta e e-mail confirmado. */
    public PessoaResponse vincularAuth(Jwt jwt, UUID id, UUID authUserId) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        Pessoa pessoa = buscarEntidade(id);
        if (pessoa.getAuthUserId() != null) {
            throw new EstadoInvalidoException("Essa pessoa já tem uma conta vinculada");
        }
        if (pessoaRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new EstadoInvalidoException("Essa conta já está ligada a outra pessoa");
        }
        if (!confirmacaoEmail.confirmado(authUserId)) {
            throw new EstadoInvalidoException("Essa conta ainda não confirmou o e-mail");
        }
        pessoa.setAuthUserId(authUserId);
        registrarAuditoria(pessoa.getId(), "auth_user_id", null, authUserId.toString(), ator.getId());
        return paraResponse(pessoa);
    }

    /**
     * Liga a conta pendente (recém-cadastrada) a uma pessoa que já existe, por exemplo importada da planilha.
     * A conta passa a ser da pessoa existente, que mantém todo o histórico; o cadastro pendente, que seria
     * uma duplicata, é removido. chave C e e-mail informados no cadastro só entram se a pessoa ainda não os tem.
     */
    public PessoaResponse vincularContaAPessoaExistente(Jwt jwt, UUID pendenteId, UUID existenteId) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        exigirGestorOuAdmin(ator);
        if (pendenteId.equals(existenteId)) {
            throw new EstadoInvalidoException("Escolha um cadastro diferente do pendente");
        }
        Pessoa pendente = buscarEntidade(pendenteId);
        Pessoa existente = buscarEntidade(existenteId);

        if (pendente.getAprovadoEm() != null || pendente.getStatus() == StatusPessoa.REJEITADO
                || pendente.getAuthUserId() == null) {
            throw new EstadoInvalidoException("Só uma conta aguardando aprovação pode ser vinculada");
        }
        if (existente.getAuthUserId() != null) {
            throw new EstadoInvalidoException("Essa pessoa já tem uma conta vinculada");
        }
        if (existente.getAprovadoEm() == null) {
            throw new EstadoInvalidoException("Escolha um cadastro já aprovado");
        }
        if (existente.getStatus() != StatusPessoa.ATIVO) {
            throw new EstadoInvalidoException("Reative a pessoa antes de vincular uma conta a ela");
        }
        checarPermissaoSobrePerfil(ator, existente.getPerfil(), existente);

        UUID authUserId = pendente.getAuthUserId();
        String chaveC = pendente.getChaveC();
        String email = pendente.getEmail();

        // remove o duplicado primeiro: auth_user_id e chaveC são únicos e vão para a pessoa existente
        pessoaRepository.delete(pendente);
        pessoaRepository.flush();

        existente.setAuthUserId(authUserId);
        // o e-mail do cadastro passa a ser o da conta de login (é por ele que a pessoa entra)
        if (email != null && !email.equalsIgnoreCase(existente.getEmail())) {
            registrarAuditoria(existente.getId(), "email", existente.getEmail(), email, ator.getId());
            existente.setEmail(email);
        }
        if (existente.getChaveC() == null && chaveC != null) {
            existente.setChaveC(chaveC);
        }
        registrarAuditoria(existente.getId(), "auth_user_id", null, authUserId.toString(), ator.getId());

        return paraResponse(existente);
    }

    /**
     * Aprovação do vínculo sugerido pelo chave C: é aqui que o Gestor/Admin confirma que a conta nova é a pessoa do
     * cadastro importado. Só então o login passa para o cadastro (e o histórico dele fica acessível).
     */
    public PessoaResponse confirmarVinculo(Jwt jwt, UUID pendenteId) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        exigirGestorOuAdmin(ator);
        Pessoa pendente = buscarEntidade(pendenteId);
        Pessoa sugerida = pendente.getVinculoSugerido();
        if (sugerida == null) {
            throw new EstadoInvalidoException("Esta conta não tem vínculo sugerido");
        }
        if (pendente.getAuthUserId() == null || !confirmacaoEmail.confirmado(pendente.getAuthUserId())) {
            throw new EstadoInvalidoException("A conta ainda não confirmou o e-mail");
        }
        UUID sugeridaId = sugerida.getId();
        PessoaResponse resposta = vincularContaAPessoaExistente(jwt, pendenteId, sugeridaId);
        registrarAuditoria(sugeridaId, "vinculo_confirmado", pendenteId.toString(), sugeridaId.toString(), ator.getId());
        return resposta;
    }

    @Transactional(readOnly = true)
    public MeResponse buscarStatusPorAuthUserId(UUID authUserId) {
        return pessoaRepository.findByAuthUserId(authUserId)
                .map(pessoa -> new MeResponse(pessoa.getAprovadoEm() != null, paraResponse(pessoa)))
                .orElseGet(() -> new MeResponse(false, null));
    }

    /**
     * Cadastro da própria conta. Se o chave C é de um cadastro que já existe sem login (importado da planilha), a conta
     * NÃO assume esse cadastro: nasce como pendente comum, sem histórico e sem permissão, e guarda só a sugestão
     * de vínculo. Quem concretiza é um Gestor/Admin (confirmarVinculo). Quem não tem como ser vinculado recebe a
     * mesma mensagem neutra, sem dados do cadastro.
     */
    public PessoaResponse autoCadastro(Jwt jwt, AutoCadastroRequest request) {
        UUID authUserId = UUID.fromString(jwt.getSubject());
        limitadorTentativas.registrar("auto-cadastro:" + authUserId);

        if (!confirmacaoEmail.confirmado(authUserId)) {
            throw new EstadoInvalidoException("Confirme seu e-mail pelo link que enviamos antes de enviar o cadastro.");
        }
        if (pessoaRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new EstadoInvalidoException("Já existe um cadastro vinculado a este usuário");
        }

        String chaveC = ChaveC.normalizar(request.chaveC());
        if (!ChaveC.valido(chaveC)) {
            throw new IllegalArgumentException("Chave C inválida: use C ou F seguido de 7 números");
        }

        Pessoa sugerida = null;
        Pessoa comEssaChave = pessoaRepository.findByChaveC(chaveC).orElse(null);
        if (comEssaChave != null) {
            if (comEssaChave.getAuthUserId() != null) {
                throw new EstadoInvalidoException(MSG_CADASTRO_NAO_CONCLUIDO);
            }
            sugerida = comEssaChave;
        }

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        Pessoa pessoa = new Pessoa(request.nome().trim(), area);
        if (sugerida == null) {
            pessoa.setChaveC(chaveC);
        } else {
            // o chave C é único e já pertence ao cadastro importado; a ligação é só uma sugestão até a aprovação
            pessoa.setVinculoSugerido(sugerida);
        }
        String exibicao = request.nomeExibicao() == null ? "" : request.nomeExibicao().trim();
        pessoa.setNomeExibicao(exibicao.isEmpty() ? null : exibicao);
        pessoa.setEmail(jwt.getClaimAsString("email"));
        pessoa.setCargo(resolverCargo(request.cargoId()));
        pessoa.setAuthUserId(authUserId);

        Pessoa salva = pessoaRepository.save(pessoa);
        if (sugerida != null) {
            registrarAuditoria(salva.getId(), "vinculo_sugerido_id", null, sugerida.getId().toString(), salva.getId());
        }
        return paraResponse(salva);
    }

    public PessoaResponse aprovar(UUID id, Jwt jwt, AprovarRequest request) {
        Pessoa aprovador = currentPessoaResolver.resolver(jwt);
        Pessoa alvo = buscarEntidade(id);

        checarPermissaoSobrePerfil(aprovador, request.perfil(), alvo);

        if (request.areaId() != null && !request.areaId().equals(alvo.getArea().getId())) {
            Area novaArea = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));
            registrarAuditoria(alvo.getId(), "area_id", alvo.getArea().getId().toString(), novaArea.getId().toString(), aprovador.getId());
            alvo.setArea(novaArea);
        }

        UUID cargoAtualId = alvo.getCargo() != null ? alvo.getCargo().getId() : null;
        if (request.cargoId() != null && !request.cargoId().equals(cargoAtualId)) {
            Cargo novoCargo = resolverCargo(request.cargoId());
            registrarAuditoria(
                    alvo.getId(), "cargo_id",
                    cargoAtualId != null ? cargoAtualId.toString() : null,
                    request.cargoId().toString(),
                    aprovador.getId()
            );
            alvo.setCargo(novoCargo);
        }

        Area referenciaArea = request.referenciaAreaId() != null
                ? areaRepository.findById(request.referenciaAreaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.referenciaAreaId()))
                : null;

        if (alvo.getVinculoSugerido() != null) {
            // aprovada como pessoa nova: a sugestão deixa de valer e fica registrada
            registrarAuditoria(alvo.getId(), "vinculo_sugerido_id", alvo.getVinculoSugerido().getId().toString(), null, aprovador.getId());
            alvo.setVinculoSugerido(null);
        }
        alvo.setPerfil(request.perfil());
        alvo.setReferenciaArea(referenciaArea);
        alvo.setAprovadoEm(OffsetDateTime.now());
        alvo.setAprovadoPor(aprovador);

        return paraResponse(alvo);
    }

    public PessoaResponse rejeitar(UUID id, Jwt jwt, RejeitarRequest request) {
        Pessoa ator = currentPessoaResolver.resolver(jwt);
        Pessoa alvo = buscarEntidade(id);

        if (alvo.getAprovadoEm() != null) {
            throw new EstadoInvalidoException("Pessoa já aprovada não pode ser rejeitada — use desativar");
        }

        checarPermissaoSobrePerfil(ator, alvo.getPerfil(), alvo);

        Auditoria registro = new Auditoria("pessoa", alvo.getId(), "status", alvo.getStatus().name(), StatusPessoa.REJEITADO.name(), ator.getId());
        registro.setMotivo(request.motivo());
        auditoriaRepository.save(registro);

        alvo.setStatus(StatusPessoa.REJEITADO);

        return paraResponse(alvo);
    }

    public PessoaResponse atualizarPerfilProprio(Jwt jwt, AtualizarPerfilRequest request) {
        Pessoa pessoa = currentPessoaResolver.resolver(jwt);
        // o nome completo é do cadastro da empresa: quem muda é Gestor/Admin. A própria pessoa escolhe como ser chamada.
        String exibicao = request.nomeExibicao() == null ? "" : request.nomeExibicao().trim();
        pessoa.setNomeExibicao(exibicao.isEmpty() ? null : exibicao);
        pessoa.setFotoUrl(request.fotoUrl());
        return paraResponse(pessoa);
    }

    /**
     * ADMIN pode tudo. GESTOR pode gerenciar qualquer Pessoa, EXCETO:
     * conceder o perfil ADMIN, ou mexer numa Pessoa que já é ADMIN.
     * pessoaAlvoExistente é null em criar() (ainda não existe alvo).
     */
    private void checarPermissaoSobrePerfil(Pessoa ator, PerfilPessoa perfilAlvo, Pessoa pessoaAlvoExistente) {
        if (ator.getPerfil() == PerfilPessoa.ADMIN) {
            return;
        }
        boolean elevandoParaAdmin = perfilAlvo == PerfilPessoa.ADMIN;
        boolean alvoJaEhAdmin = pessoaAlvoExistente != null && pessoaAlvoExistente.getPerfil() == PerfilPessoa.ADMIN;
        if (elevandoParaAdmin || alvoJaEhAdmin) {
            throw new AccessDeniedException("Gestor não pode conceder ADMIN nem alterar uma Pessoa ADMIN");
        }
    }

    private void exigirGestorOuAdmin(Pessoa ator) {
        if (ator.getPerfil() != PerfilPessoa.ADMIN && ator.getPerfil() != PerfilPessoa.GESTOR) {
            throw new AccessDeniedException("Só Gestor ou Admin pode ligar uma conta a um cadastro");
        }
    }

    private void registrarAuditoria(UUID registroId, String campo, String valorAnterior, String valorNovo, UUID alteradoPor) {
        auditoriaRepository.save(new Auditoria("pessoa", registroId, campo, valorAnterior, valorNovo, alteradoPor));
    }

    /** Vazio/nulo mantém o chave C atual; informado, precisa ser válido e não pode pertencer a outra pessoa. */
    private Area resolverReferenciaArea(UUID areaId) {
        if (areaId == null) {
            return null;
        }
        return areaRepository.findById(areaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + areaId));
    }

    private void aplicarChaveC(Pessoa pessoa, String informado) {
        String chaveC = ChaveC.normalizar(informado);
        if (chaveC == null) {
            return;
        }
        if (!ChaveC.valido(chaveC)) {
            throw new IllegalArgumentException("Chave C inválida: use C ou F seguido de 7 números");
        }
        pessoaRepository.findByChaveC(chaveC)
                .filter(outra -> !outra.equals(pessoa))
                .ifPresent(outra -> {
                    throw new EstadoInvalidoException("Esta chave C já pertence a outra pessoa cadastrada");
                });
        pessoa.setChaveC(chaveC);
    }

    private Cargo resolverCargo(UUID cargoId) {
        if (cargoId == null) {
            return null;
        }
        return cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo não encontrado: " + cargoId));
    }

    private Pessoa buscarEntidade(UUID id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + id));
    }

    private PessoaResponse paraResponse(Pessoa pessoa) {
        Cargo cargo = pessoa.getCargo();
        Pessoa aprovadoPor = pessoa.getAprovadoPor();
        Area referenciaArea = pessoa.getReferenciaArea();
        return new PessoaResponse(
                pessoa.getId(), pessoa.getNome(), pessoa.getNomeExibicao(), pessoa.getChaveC(), pessoa.getEmail(), pessoa.getFotoUrl(),
                pessoa.getArea().getDiretoria().getId(), pessoa.getArea().getDiretoria().getNome(),
                pessoa.getArea().getNucleo() != null ? pessoa.getArea().getNucleo().getId() : null,
                pessoa.getArea().getNucleo() != null ? pessoa.getArea().getNucleo().getNome() : null,
                pessoa.getArea().getId(), pessoa.getArea().getNome(),
                cargo != null ? cargo.getId() : null,
                cargo != null ? cargo.getNome() : null,
                pessoa.getStatus(), pessoa.getPerfil(),
                referenciaArea != null ? referenciaArea.getId() : null,
                referenciaArea != null ? referenciaArea.getNome() : null,
                pessoa.getAuthUserId(),
                aprovadoPor != null ? aprovadoPor.getId() : null,
                aprovadoPor != null ? aprovadoPor.getNome() : null,
                pessoa.getAprovadoEm(),
                pessoa.getCreatedAt(), pessoa.getUpdatedAt(),
                pessoa.getVinculoSugerido() != null
        );
    }
}