package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.entity.Campanha;
import dev.denetodev.sgd_api.entity.Cliente;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Projeto;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.CampanhaRepository;
import dev.denetodev.sgd_api.repository.ClienteRepository;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.ProjetoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DemandaService {

    private final DemandaRepository demandaRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final ClienteRepository clienteRepository;
    private final ProjetoRepository projetoRepository;
    private final CampanhaRepository campanhaRepository;

    public DemandaService(
            DemandaRepository demandaRepository,
            DiretoriaRepository diretoriaRepository,
            ClienteRepository clienteRepository,
            ProjetoRepository projetoRepository,
            CampanhaRepository campanhaRepository
    ) {
        this.demandaRepository = demandaRepository;
        this.diretoriaRepository = diretoriaRepository;
        this.clienteRepository = clienteRepository;
        this.projetoRepository = projetoRepository;
        this.campanhaRepository = campanhaRepository;
    }

    @Transactional(readOnly = true)
    public List<DemandaResponse> listarTodas() {
        return demandaRepository.findAll().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DemandaResponse buscarPorId(UUID id) {
        Demanda demanda = demandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + id));
        return paraResponse(demanda);
    }

    public DemandaResponse criar(DemandaRequest request) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));

        Demanda demanda = new Demanda(request.titulo(), diretoria);
        demanda.setDescricao(request.descricao());
        demanda.setCodigo(request.codigo());
        demanda.setDataPrazo(request.dataPrazo());
        demanda.setValor(request.valor());
        demanda.setObservacoes(request.observacoes());

        if (request.prioridade() != null) {
            demanda.setPrioridade(request.prioridade());
        }

        if (request.clienteId() != null) {
            Cliente cliente = clienteRepository.findById(request.clienteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + request.clienteId()));
            demanda.setCliente(cliente);
        }

        if (request.projetoId() != null) {
            Projeto projeto = projetoRepository.findById(request.projetoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + request.projetoId()));
            demanda.setProjeto(projeto);
        }

        if (request.campanhaId() != null) {
            Campanha campanha = campanhaRepository.findById(request.campanhaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Campanha não encontrada: " + request.campanhaId()));
            demanda.setCampanha(campanha);
        }

        Demanda salva = demandaRepository.save(demanda);
        return paraResponse(salva);
    }

    private DemandaResponse paraResponse(Demanda demanda) {
        Cliente cliente = demanda.getCliente();
        Projeto projeto = demanda.getProjeto();
        Campanha campanha = demanda.getCampanha();

        return new DemandaResponse(
                demanda.getId(),
                demanda.getTitulo(),
                demanda.getDescricao(),
                demanda.getCodigo(),
                demanda.getDiretoria().getId(),
                demanda.getDiretoria().getNome(),
                cliente != null ? cliente.getId() : null,
                cliente != null ? cliente.getNome() : null,
                projeto != null ? projeto.getId() : null,
                projeto != null ? projeto.getNome() : null,
                campanha != null ? campanha.getId() : null,
                campanha != null ? campanha.getNome() : null,
                demanda.getPrioridade(),
                demanda.getStatus(),
                demanda.getDataCriacao(),
                demanda.getDataPrazo(),
                demanda.getDataEntregaReal(),
                demanda.getValor(),
                demanda.getObservacoes(),
                demanda.getCreatedAt(),
                demanda.getUpdatedAt()
        );
    }
}