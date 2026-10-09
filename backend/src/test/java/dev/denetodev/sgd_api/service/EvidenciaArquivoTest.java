package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.EvidenciaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvidenciaArquivoTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    /** Storage em memória que guarda o que foi enviado e removido. */
    static class StorageFalso implements ArquivoStorage {
        final List<String> enviados = new ArrayList<>();
        final List<String> removidos = new ArrayList<>();
        String mimeEnviado;

        public void enviar(String caminho, byte[] conteudo, String contentType) {
            enviados.add(caminho);
            mimeEnviado = contentType;
        }

        public String urlAssinada(String caminho, int validadeSegundos) {
            return "https://storage.exemplo/" + caminho + "?token=abc";
        }

        public void remover(String caminho) {
            removidos.add(caminho);
        }
    }

    private EvidenciaRepository evidenciaRepository;
    private AtividadeRepository atividadeRepository;
    private PermissaoService permissaoService;
    private CurrentPessoaResolver resolver;
    private StorageFalso storage;
    private EvidenciaService service;
    private final Jwt jwt = mock(Jwt.class);

    private final UUID atividadeId = UUID.randomUUID();
    private Pessoa usuario;

    @BeforeEach
    void setUp() {
        evidenciaRepository = mock(EvidenciaRepository.class);
        atividadeRepository = mock(AtividadeRepository.class);
        permissaoService = mock(PermissaoService.class);
        resolver = mock(CurrentPessoaResolver.class);
        storage = new StorageFalso();
        service = new EvidenciaService(evidenciaRepository, atividadeRepository, mock(PecaRepository.class),
                resolver, permissaoService, storage);

        usuario = mock(Pessoa.class);
        when(usuario.getId()).thenReturn(UUID.randomUUID());
        when(resolver.resolver(jwt)).thenReturn(usuario);

        Atividade atividade = mock(Atividade.class);
        when(atividadeRepository.findById(atividadeId)).thenReturn(Optional.of(atividade));
        when(permissaoService.podeGerenciar(any(), any())).thenReturn(true);
        when(evidenciaRepository.save(any(Evidencia.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MockMultipartFile arquivo(String tipoDeclarado, byte[] bytes) {
        return new MockMultipartFile("arquivo", "foto.png", tipoDeclarado, bytes);
    }

    @Test
    void aceitaPngEJpegDetectadosPelosBytes() {
        EvidenciaResponse png = service.criarComArquivo(jwt, atividadeId, null, " print final ", arquivo("image/png", PNG));
        assertEquals(TipoEvidencia.IMAGEM, png.tipo());
        assertEquals("image/png", png.arquivoMime());
        assertEquals("print final", png.descricao());
        assertTrue(storage.enviados.get(0).endsWith(".png"));

        // o tipo declarado pelo navegador não manda: bytes de JPEG viram image/jpeg
        EvidenciaResponse jpg = service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", JPEG));
        assertEquals("image/jpeg", jpg.arquivoMime());
        assertTrue(storage.enviados.get(1).endsWith(".jpg"));
        assertEquals("image/jpeg", storage.mimeEnviado);
    }

    @Test
    void recusaArquivoQueNaoEImagemMesmoDeclaradoComoPng() {
        byte[] script = "<script>alert(1)</script>".getBytes();

        assertThrows(IllegalArgumentException.class,
                () -> service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", script)));
        assertTrue(storage.enviados.isEmpty());
    }

    @Test
    void recusaGifEArquivoVazioEAcimaDe5Mb() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a', 0, 0};
        assertThrows(IllegalArgumentException.class,
                () -> service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/gif", gif)));
        assertThrows(IllegalArgumentException.class,
                () -> service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", new byte[0])));

        byte[] grande = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);
        assertThrows(IllegalArgumentException.class,
                () -> service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", grande)));
    }

    @Test
    void exigeVinculoEPermissaoSobreOItem() {
        assertThrows(IllegalArgumentException.class,
                () -> service.criarComArquivo(jwt, null, null, null, arquivo("image/png", PNG)));

        when(permissaoService.podeGerenciar(any(), any())).thenReturn(false);
        assertThrows(AccessDeniedException.class,
                () -> service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", PNG)));
        assertTrue(storage.enviados.isEmpty());
    }

    @Test
    void arquivoNaoFicaOrfaoSeOBancoRecusar() {
        when(evidenciaRepository.save(any(Evidencia.class))).thenThrow(new IllegalStateException("falha no banco"));

        assertThrows(IllegalStateException.class,
                () -> service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", PNG)));

        assertEquals(storage.enviados, storage.removidos);
    }

    @Test
    void linkExternoPrecisaSerHttpEImagemNaoPassaPeloCriarComum() {
        EvidenciaResponse ok = service.criar(jwt, new EvidenciaRequest(atividadeId, null, TipoEvidencia.LINK, " https://exemplo.com/x ", null));
        assertEquals(TipoEvidencia.LINK, ok.tipo());

        assertThrows(IllegalArgumentException.class,
                () -> service.criar(jwt, new EvidenciaRequest(atividadeId, null, TipoEvidencia.LINK, "javascript:alert(1)", null)));
        assertThrows(IllegalArgumentException.class,
                () -> service.criar(jwt, new EvidenciaRequest(atividadeId, null, TipoEvidencia.IMAGEM, "foto.png", null)));
    }

    @Test
    void urlAssinadaSoParaEvidenciaComArquivoERemoverLimpaOStorage() {
        EvidenciaResponse criada = service.criarComArquivo(jwt, atividadeId, null, null, arquivo("image/png", PNG));
        String caminho = storage.enviados.get(0);

        Evidencia comArquivo = mock(Evidencia.class);
        UUID id = UUID.randomUUID();
        when(comArquivo.getArquivoPath()).thenReturn(caminho);
        when(evidenciaRepository.findById(id)).thenReturn(Optional.of(comArquivo));
        assertTrue(service.urlDoArquivo(id).url().contains(caminho));

        Evidencia link = mock(Evidencia.class);
        UUID idLink = UUID.randomUUID();
        when(evidenciaRepository.findById(idLink)).thenReturn(Optional.of(link));
        assertThrows(dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException.class, () -> service.urlDoArquivo(idLink));

        when(permissaoService.podeGerenciarEvidencia(any(), any())).thenReturn(true);
        service.remover(jwt, id);
        assertEquals(List.of(caminho), storage.removidos);
        assertNotNull(criada);
    }
}
