package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.FotoEmprestimoResponse;
import dev.denetodev.sgd_api.entity.Emprestimo;
import dev.denetodev.sgd_api.entity.EmprestimoFoto;
import dev.denetodev.sgd_api.entity.MomentoFoto;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.EmprestimoFotoRepository;
import dev.denetodev.sgd_api.repository.EmprestimoRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.EquipamentoPermissao;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

/**
 * Fotos opcionais do equipamento na saída e na devolução. Mesmas regras das evidências: só JPEG e PNG até 5 MB,
 * o tipo vem dos bytes e o caminho no bucket é gerado aqui. Quem registra e remove é quem opera o módulo.
 */
@Service
@Transactional
public class EmprestimoFotoService {

    static final int TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;
    static final int MAXIMO_POR_MOMENTO = 6;
    private static final int VALIDADE_URL_SEGUNDOS = 300;

    private final EmprestimoRepository emprestimoRepository;
    private final EmprestimoFotoRepository fotoRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final ArquivoStorage storage;

    public EmprestimoFotoService(
            EmprestimoRepository emprestimoRepository,
            EmprestimoFotoRepository fotoRepository,
            CurrentPessoaResolver currentPessoaResolver,
            ArquivoStorage storage
    ) {
        this.emprestimoRepository = emprestimoRepository;
        this.fotoRepository = fotoRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<FotoEmprestimoResponse> listar(Jwt jwt, UUID emprestimoId) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirAcesso(usuario);
        Emprestimo emprestimo = buscarEmprestimo(emprestimoId);
        if (!EquipamentoPermissao.opera(usuario) && emprestimo.getStatus() != StatusEmprestimo.RETIRADO) {
            throw new AccessDeniedException("Sem permissão para ver as fotos deste empréstimo");
        }
        return fotoRepository.findByEmprestimoIdOrderByCreatedAtAsc(emprestimoId).stream().map(this::paraResponse).toList();
    }

    public FotoEmprestimoResponse enviar(Jwt jwt, UUID emprestimoId, MomentoFoto momento, MultipartFile arquivo) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(usuario);
        Emprestimo emprestimo = buscarEmprestimo(emprestimoId);
        exigirMomentoCompativel(emprestimo, momento);
        if (fotoRepository.countByEmprestimoIdAndMomento(emprestimoId, momento) >= MAXIMO_POR_MOMENTO) {
            throw new EstadoInvalidoException("Limite de " + MAXIMO_POR_MOMENTO + " fotos por etapa atingido");
        }
        byte[] bytes = lerBytes(arquivo);
        String mime = EvidenciaService.detectarMime(bytes);

        String caminho = "emprestimos/" + UUID.randomUUID() + (mime.equals("image/png") ? ".png" : ".jpg");
        storage.enviar(caminho, bytes, mime);
        try {
            EmprestimoFoto foto = fotoRepository.save(new EmprestimoFoto(emprestimo, momento, caminho, mime, bytes.length, usuario));
            return paraResponse(foto);
        } catch (RuntimeException e) {
            storage.remover(caminho); // não deixa arquivo órfão se o banco recusar
            throw e;
        }
    }

    public void remover(Jwt jwt, UUID emprestimoId, UUID fotoId) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        EquipamentoPermissao.exigirOperacao(usuario);
        EmprestimoFoto foto = fotoRepository.findById(fotoId)
                .filter(f -> f.getEmprestimo().getId().equals(emprestimoId))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Foto não encontrada: " + fotoId));
        fotoRepository.delete(foto);
        storage.remover(foto.getArquivoPath());
    }

    /** Foto de saída vale da autorização até o fim; foto de devolução, depois que o equipamento saiu. */
    private void exigirMomentoCompativel(Emprestimo e, MomentoFoto momento) {
        StatusEmprestimo s = e.getStatus();
        boolean ok = momento == MomentoFoto.SAIDA
                ? (s == StatusEmprestimo.AUTORIZADO || s == StatusEmprestimo.RETIRADO || s == StatusEmprestimo.DEVOLVIDO)
                : (s == StatusEmprestimo.RETIRADO || s == StatusEmprestimo.DEVOLVIDO);
        if (!ok) {
            throw new EstadoInvalidoException("Não dá para anexar foto de "
                    + (momento == MomentoFoto.SAIDA ? "saída" : "devolução") + " a um empréstimo " + s);
        }
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de imagem");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Imagem acima de 5 MB");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }

    private Emprestimo buscarEmprestimo(UUID id) {
        return emprestimoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado: " + id));
    }

    private FotoEmprestimoResponse paraResponse(EmprestimoFoto f) {
        return new FotoEmprestimoResponse(f.getId(), f.getMomento(), storage.urlAssinada(f.getArquivoPath(), VALIDADE_URL_SEGUNDOS),
                VALIDADE_URL_SEGUNDOS, f.getArquivoTamanho(), f.getCreatedBy().getNome(), f.getCreatedAt());
    }
}
