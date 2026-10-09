package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Evidencia;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.entity.TipoEvidencia;
import dev.denetodev.sgd_api.dto.response.ArquivoUrlResponse;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.UncheckedIOException;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.EvidenciaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EvidenciaService {

    private static final int TAMANHO_MAXIMO_BYTES = 5 * 1024 * 1024;
    private static final int VALIDADE_URL_SEGUNDOS = 300;

    private final EvidenciaRepository evidenciaRepository;
    private final AtividadeRepository atividadeRepository;
    private final PecaRepository pecaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;
    private final ArquivoStorage storage;

    public EvidenciaService(
            EvidenciaRepository evidenciaRepository,
            AtividadeRepository atividadeRepository,
            PecaRepository pecaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService,
            ArquivoStorage storage
    ) {
        this.evidenciaRepository = evidenciaRepository;
        this.atividadeRepository = atividadeRepository;
        this.pecaRepository = pecaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listarTodas() {
        return evidenciaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvidenciaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public EvidenciaResponse criar(Jwt jwt, EvidenciaRequest request) {
        if (request.atividadeId() == null && request.pecaId() == null) {
            throw new IllegalArgumentException("Evidência precisa estar vinculada a uma atividade ou a uma peça");
        }

        if (request.tipo() == TipoEvidencia.IMAGEM) {
            throw new IllegalArgumentException("Imagem é enviada pelo upload (POST /evidencias/arquivo)");
        }
        if (request.tipo() == TipoEvidencia.LINK) {
            validarLink(request.conteudo());
        }

        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Atividade atividade = Resolvers.resolverOuNulo(request.atividadeId(), atividadeRepository, "Atividade");
        Peca peca = Resolvers.resolverOuNulo(request.pecaId(), pecaRepository, "Peça");
        exigirPermissaoParaAnexar(usuario, atividade, peca);

        Evidencia evidencia = new Evidencia(request.tipo());
        evidencia.setAtividade(atividade);
        evidencia.setPeca(peca);
        evidencia.setConteudo(request.conteudo());
        evidencia.setDescricao(request.descricao());
        evidencia.setCreatedBy(usuario.getId());

        return paraResponse(evidenciaRepository.save(evidencia));
    }

    /**
     * Evidência em imagem: só JPEG e PNG, até 5 MB. O tipo vem dos bytes do arquivo, não do que o
     * navegador declarou, e o caminho no bucket é gerado aqui (nada do nome original é usado).
     */
    public EvidenciaResponse criarComArquivo(Jwt jwt, UUID atividadeId, UUID pecaId, String descricao, MultipartFile arquivo) {
        if (atividadeId == null && pecaId == null) {
            throw new IllegalArgumentException("Evidência precisa estar vinculada a uma atividade ou a uma peça");
        }
        byte[] bytes = lerBytes(arquivo);
        String mime = detectarMime(bytes);

        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Atividade atividade = Resolvers.resolverOuNulo(atividadeId, atividadeRepository, "Atividade");
        Peca peca = Resolvers.resolverOuNulo(pecaId, pecaRepository, "Peça");
        exigirPermissaoParaAnexar(usuario, atividade, peca);

        String caminho = "evidencias/" + UUID.randomUUID() + (mime.equals("image/png") ? ".png" : ".jpg");
        storage.enviar(caminho, bytes, mime);

        try {
            Evidencia evidencia = new Evidencia(TipoEvidencia.IMAGEM);
            evidencia.setAtividade(atividade);
            evidencia.setPeca(peca);
            evidencia.setDescricao(descricao == null || descricao.isBlank() ? null : descricao.trim());
            evidencia.setArquivoPath(caminho);
            evidencia.setArquivoMime(mime);
            evidencia.setArquivoTamanho(bytes.length);
            evidencia.setCreatedBy(usuario.getId());
            return paraResponse(evidenciaRepository.save(evidencia));
        } catch (RuntimeException e) {
            storage.remover(caminho); // não deixa arquivo órfão se o banco recusar
            throw e;
        }
    }

    /** URL temporária (5 minutos) para ver a imagem; o bucket é privado. */
    @Transactional(readOnly = true)
    public ArquivoUrlResponse urlDoArquivo(UUID id) {
        Evidencia evidencia = buscarEntidade(id);
        if (evidencia.getArquivoPath() == null) {
            throw new RecursoNaoEncontradoException("Esta evidência não tem arquivo");
        }
        return new ArquivoUrlResponse(storage.urlAssinada(evidencia.getArquivoPath(), VALIDADE_URL_SEGUNDOS), VALIDADE_URL_SEGUNDOS);
    }

    public void remover(Jwt jwt, UUID id) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Evidencia evidencia = buscarEntidade(id);

        if (!permissaoService.podeGerenciarEvidencia(usuario, evidencia)) {
            throw new AccessDeniedException("Sem permissão para remover esta evidência");
        }

        evidenciaRepository.delete(evidencia);
        if (evidencia.getArquivoPath() != null) {
            storage.remover(evidencia.getArquivoPath());
        }
    }

    private void exigirPermissaoParaAnexar(Pessoa usuario, Atividade atividade, Peca peca) {
        boolean podePelaAtividade = atividade == null || permissaoService.podeGerenciar(usuario, atividade.getPessoa());
        boolean podePelaPeca = peca == null || permissaoService.podeGerenciar(usuario, peca.getPessoa());
        if (!podePelaAtividade || !podePelaPeca) {
            throw new AccessDeniedException("Sem permissão para anexar evidência a este item");
        }
    }

    private static void validarLink(String conteudo) {
        String link = conteudo == null ? "" : conteudo.trim().toLowerCase();
        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            throw new IllegalArgumentException("Link externo deve começar com http:// ou https://");
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

    /** JPEG começa com FF D8 FF; PNG com 89 50 4E 47 0D 0A 1A 0A. */
    static String detectarMime(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (b.length >= png.length) {
            boolean igual = true;
            for (int i = 0; i < png.length; i++) {
                igual &= b[i] == png[i];
            }
            if (igual) {
                return "image/png";
            }
        }
        throw new IllegalArgumentException("Só são aceitas imagens JPEG ou PNG");
    }

    private Evidencia buscarEntidade(UUID id) {
        return evidenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada: " + id));
    }

    private EvidenciaResponse paraResponse(Evidencia e) {
        Atividade atividade = e.getAtividade();
        Peca peca = e.getPeca();
        return new EvidenciaResponse(
                e.getId(),
                atividade != null ? atividade.getId() : null,
                peca != null ? peca.getId() : null,
                e.getTipo(), e.getConteudo(), e.getDescricao(),
                e.getArquivoMime(), e.getArquivoTamanho(),
                e.getCreatedAt()
        );
    }
}