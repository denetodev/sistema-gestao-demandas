package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.VinculoSugeridoResponse;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.util.Cpf;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Resumo do cadastro importado que uma conta nova pediu para assumir (só Admin e Gestor chegam aqui). */
@Service
@Transactional(readOnly = true)
public class VinculoSugeridoService {

    private final PessoaRepository pessoaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final AtividadeRepository atividadeRepository;
    private final PecaRepository pecaRepository;

    public VinculoSugeridoService(
            PessoaRepository pessoaRepository,
            PessoaDemandaRepository pessoaDemandaRepository,
            AtividadeRepository atividadeRepository,
            PecaRepository pecaRepository
    ) {
        this.pessoaRepository = pessoaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.atividadeRepository = atividadeRepository;
        this.pecaRepository = pecaRepository;
    }

    public VinculoSugeridoResponse resumo(UUID pendenteId) {
        Pessoa pendente = pessoaRepository.findById(pendenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + pendenteId));
        Pessoa sugerida = pendente.getVinculoSugerido();
        if (sugerida == null) {
            throw new RecursoNaoEncontradoException("Esta conta não tem vínculo sugerido");
        }
        boolean jaTemConta = sugerida.getAuthUserId() != null;
        return new VinculoSugeridoResponse(
                sugerida.getId(), sugerida.getNome(), sugerida.getNomeExibicao(),
                sugerida.getArea().getNome(),
                sugerida.getCargo() != null ? sugerida.getCargo().getNome() : null,
                Cpf.mascarar(sugerida.getCpf()), sugerida.getStatus(), jaTemConta,
                pessoaDemandaRepository.countByPessoa_Id(sugerida.getId()),
                atividadeRepository.countByPessoa_Id(sugerida.getId()),
                pecaRepository.countByPessoa_Id(sugerida.getId()),
                pendente.getNome(), pendente.getEmail(),
                impedimento(pendente, sugerida, jaTemConta)
        );
    }

    private String impedimento(Pessoa pendente, Pessoa sugerida, boolean jaTemConta) {
        if (pendente.getAprovadoEm() != null || pendente.getStatus() == StatusPessoa.REJEITADO) {
            return "Esta conta já foi decidida.";
        }
        if (jaTemConta) {
            return "Essa pessoa já tem uma conta vinculada.";
        }
        if (sugerida.getStatus() != StatusPessoa.ATIVO) {
            return "Reative a pessoa antes de vincular uma conta a ela.";
        }
        if (sugerida.getAprovadoEm() == null) {
            return "O cadastro importado ainda não foi aprovado.";
        }
        return null;
    }
}
