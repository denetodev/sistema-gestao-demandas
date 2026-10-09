package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AtualizarPapelRequest;
import dev.denetodev.sgd_api.dto.request.ParticipanteRequest;
import dev.denetodev.sgd_api.dto.response.ParticipanteResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Participantes de uma demanda. Quem gerencia o time:
 * - Gestor/Admin e o Responsável principal da demanda: qualquer participante;
 * - Referência de Equipe: participantes da área que referencia;
 * - qualquer outra pessoa: só o próprio vínculo (mudar o próprio papel ou sair).
 * Só Gestor/Admin e o Responsável principal atribuem o papel RESPONSAVEL_PRINCIPAL.
 * Visualizador não altera nada.
 */
@Service
@Transactional
public class ParticipanteService {

    private final DemandaRepository demandaRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;

    public ParticipanteService(
            DemandaRepository demandaRepository,
            PessoaRepository pessoaRepository,
            PessoaDemandaRepository pessoaDemandaRepository,
            CurrentPessoaResolver currentPessoaResolver
    ) {
        this.demandaRepository = demandaRepository;
        this.pessoaRepository = pessoaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
    }

    @Transactional(readOnly = true)
    public List<ParticipanteResponse> listarPorDemanda(UUID demandaId) {
        if (!demandaRepository.existsById(demandaId)) {
            throw new RecursoNaoEncontradoException("Demanda não encontrada: " + demandaId);
        }
        return pessoaDemandaRepository.findByDemandaId(demandaId).stream()
                .map(this::paraResponse)
                .toList();
    }

    public ParticipanteResponse adicionar(Jwt jwt, UUID demandaId, ParticipanteRequest request) {
        return adicionar(currentPessoaResolver.resolver(jwt), demandaId, request);
    }

    ParticipanteResponse adicionar(Pessoa usuario, UUID demandaId, ParticipanteRequest request) {
        Demanda demanda = demandaRepository.findById(demandaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + demandaId));
        Pessoa pessoa = pessoaRepository.findById(request.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + request.pessoaId()));

        if (!podeGerenciar(usuario, demandaId, pessoa)) {
            throw new AccessDeniedException("Você não pode adicionar essa pessoa ao time da demanda");
        }
        exigirPermissaoParaPapel(usuario, demandaId, request.papel());

        boolean jaAtivo = pessoaDemandaRepository
                .findByPessoaIdAndDemandaIdAndDataSaidaIsNull(pessoa.getId(), demandaId)
                .isPresent();

        if (jaAtivo) {
            throw new EstadoInvalidoException("Pessoa já é participante ativo dessa demanda");
        }

        // sempre cria uma linha nova — preserva o histórico de entradas/saídas
        // anteriores em vez de reabrir e sobrescrever um vínculo antigo
        PessoaDemanda vinculo = new PessoaDemanda(pessoa, demanda);
        if (request.papel() != null) {
            vinculo.setPapel(request.papel());
        }
        vinculo = pessoaDemandaRepository.save(vinculo);

        return paraResponse(vinculo);
    }

    public ParticipanteResponse atualizarPapel(Jwt jwt, UUID demandaId, UUID participanteId, AtualizarPapelRequest request) {
        return atualizarPapel(currentPessoaResolver.resolver(jwt), demandaId, participanteId, request);
    }

    ParticipanteResponse atualizarPapel(Pessoa usuario, UUID demandaId, UUID participanteId, AtualizarPapelRequest request) {
        PessoaDemanda vinculo = buscarVinculo(demandaId, participanteId);
        if (vinculo.getDataSaida() != null) {
            throw new EstadoInvalidoException("Participante já saiu da demanda");
        }
        if (!podeGerenciar(usuario, demandaId, vinculo.getPessoa())) {
            throw new AccessDeniedException("Você não pode alterar o papel dessa pessoa");
        }
        exigirPermissaoParaPapel(usuario, demandaId, request.papel());

        vinculo.setPapel(request.papel());
        return paraResponse(vinculo);
    }

    public void remover(Jwt jwt, UUID demandaId, UUID participanteId) {
        remover(currentPessoaResolver.resolver(jwt), demandaId, participanteId);
    }

    void remover(Pessoa usuario, UUID demandaId, UUID participanteId) {
        PessoaDemanda vinculo = buscarVinculo(demandaId, participanteId);
        if (!podeGerenciar(usuario, demandaId, vinculo.getPessoa())) {
            throw new AccessDeniedException("Você não pode remover essa pessoa do time da demanda");
        }

        if (vinculo.getDataSaida() == null) {
            vinculo.setDataSaida(LocalDate.now());
        }
    }

    // ---- regras ----

    private boolean podeGerenciar(Pessoa usuario, UUID demandaId, Pessoa alvo) {
        if (usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            return false;
        }
        if (ehAdminOuGestor(usuario) || ehResponsavelPrincipal(usuario, demandaId)) {
            return true;
        }
        if (usuario.getId().equals(alvo.getId())) {
            return true; // o próprio vínculo
        }
        return usuario.getReferenciaArea() != null
                && alvo.getArea() != null
                && usuario.getReferenciaArea().getId().equals(alvo.getArea().getId());
    }

    private void exigirPermissaoParaPapel(Pessoa usuario, UUID demandaId, PapelPessoaDemanda papel) {
        if (papel == PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL
                && !ehAdminOuGestor(usuario)
                && !ehResponsavelPrincipal(usuario, demandaId)) {
            throw new AccessDeniedException("Só Gestor, Admin ou o Responsável principal atribuem o papel de Responsável principal");
        }
    }

    private boolean ehResponsavelPrincipal(Pessoa usuario, UUID demandaId) {
        return pessoaDemandaRepository
                .findByPessoaIdAndDemandaIdAndDataSaidaIsNull(usuario.getId(), demandaId)
                .map(v -> v.getPapel() == PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL)
                .orElse(false);
    }

    private static boolean ehAdminOuGestor(Pessoa usuario) {
        return usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR;
    }

    private PessoaDemanda buscarVinculo(UUID demandaId, UUID participanteId) {
        PessoaDemanda vinculo = pessoaDemandaRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado: " + participanteId));

        if (!vinculo.getDemanda().getId().equals(demandaId)) {
            throw new RecursoNaoEncontradoException("Participante não encontrado nessa demanda: " + participanteId);
        }
        return vinculo;
    }

    private ParticipanteResponse paraResponse(PessoaDemanda vinculo) {
        return new ParticipanteResponse(
                vinculo.getId(),
                vinculo.getPessoa().getId(),
                vinculo.getPessoa().getNome(),
                vinculo.getPapel(),
                vinculo.getDataEntrada(),
                vinculo.getDataSaida(),
                vinculo.getObservacao()
        );
    }
}
