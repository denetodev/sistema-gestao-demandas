package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.ClienteRequest;
import dev.denetodev.sgd_api.dto.response.ClienteResponse;
import dev.denetodev.sgd_api.entity.Cliente;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarTodas() {
        return clienteRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public ClienteResponse criar(ClienteRequest request) {
        Cliente cliente = new Cliente(request.nome(), request.tipo());
        cliente.setObservacao(request.observacao());
        return paraResponse(clienteRepository.save(cliente));
    }

    public ClienteResponse atualizar(UUID id, ClienteRequest request) {
        Cliente cliente = buscarEntidade(id);
        cliente.setNome(request.nome());
        cliente.setTipo(request.tipo());
        cliente.setObservacao(request.observacao());
        return paraResponse(cliente);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private Cliente buscarEntidade(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private ClienteResponse paraResponse(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getTipo(), c.getObservacao(), c.isAtivo(), c.getCreatedAt(), c.getUpdatedAt());
    }
}