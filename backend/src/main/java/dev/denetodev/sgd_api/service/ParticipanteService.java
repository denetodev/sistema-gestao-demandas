package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.ParticipanteRequest;
import dev.denetodev.sgd_api.dto.response.ParticipanteResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ParticipanteService {

    private final DemandaRepository demandaRepository;
    private final PessoaRepository pessoaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;

    public ParticipanteService(
            DemandaRepository demandaRepository,
            PessoaRepository pessoaRepository,
            PessoaDemandaRepository pessoaDemandaRepository
    ) {
        this.demandaRepository = demandaRepository;
        this.pessoaRepository = pessoaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
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

    public ParticipanteResponse adicionar(UUID demandaId, ParticipanteRequest request) {
        Demanda demanda = demandaRepository.findById(demandaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + demandaId));
        Pessoa pessoa = pessoaRepository.findById(request.pessoaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + request.pessoaId()));

        PessoaDemanda vinculo = pessoaDemandaRepository
                .findByPessoaIdAndDemandaId(pessoa.getId(), demandaId)
                .orElse(null);

        if (vinculo != null) {
            if (vinculo.getDataSaida() == null) {
                throw new EstadoInvalidoException("Pessoa já é participante ativo dessa demanda");
            }
            // reabre o vínculo existente em vez de inserir de novo —
            // a constraint única (pessoa_id, demanda_id) não permite duplicar a linha
            vinculo.setDataSaida(null);
            if (request.papel() != null) {
                vinculo.setPapel(request.papel());
            }
        } else {
            vinculo = new PessoaDemanda(pessoa, demanda);
            if (request.papel() != null) {
                vinculo.setPapel(request.papel());
            }
            vinculo = pessoaDemandaRepository.save(vinculo);
        }

        return paraResponse(vinculo);
    }

    public void remover(UUID demandaId, UUID participanteId) {
        PessoaDemanda vinculo = pessoaDemandaRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado: " + participanteId));

        if (!vinculo.getDemanda().getId().equals(demandaId)) {
            throw new RecursoNaoEncontradoException("Participante não encontrado nessa demanda: " + participanteId);
        }

        if (vinculo.getDataSaida() == null) {
            vinculo.setDataSaida(LocalDate.now());
        }
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