package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.FotoEmprestimoResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.EmprestimoFotoRepository;
import dev.denetodev.sgd_api.repository.EmprestimoRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmprestimoFotoServiceTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};

    private EmprestimoRepository emprestimoRepository;
    private EmprestimoFotoRepository fotoRepository;
    private CurrentPessoaResolver resolver;
    private ArquivoStorage storage;
    private EmprestimoFotoService service;
    private final Jwt jwt = mock(Jwt.class);

    private Pessoa operador;
    private Pessoa consulta;
    private Emprestimo emprestimo;

    @BeforeEach
    void preparar() {
        emprestimoRepository = mock(EmprestimoRepository.class);
        fotoRepository = mock(EmprestimoFotoRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        storage = mock(ArquivoStorage.class);
        service = new EmprestimoFotoService(emprestimoRepository, fotoRepository, resolver, storage);

        operador = pessoa("Operador", PapelEquipamentos.OPERADOR_DE_ESTUDIO, false);
        consulta = pessoa("Consulta", null, true);
        emprestimo = new Emprestimo("João", OffsetDateTime.now().plusDays(1), operador, 1);
        ReflectionTestUtils.setField(emprestimo, "id", UUID.randomUUID());
        emprestimo.setStatus(StatusEmprestimo.AUTORIZADO);

        when(emprestimoRepository.findById(emprestimo.getId())).thenReturn(Optional.of(emprestimo));
        when(fotoRepository.save(any(EmprestimoFoto.class))).thenAnswer(inv -> inv.getArgument(0));
        when(storage.urlAssinada(anyString(), anyInt())).thenReturn("https://assinada/foto");
        agir(operador);
    }

    private Pessoa pessoa(String nome, PapelEquipamentos papel, boolean equipeLiberada) {
        Area area = new Area(null, "Equipe " + nome);
        Pessoa p = new Pessoa(nome, area);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(PerfilPessoa.PROFISSIONAL);
        p.setPapelEquipamentos(papel);
        return p;
    }

    private void agir(Pessoa quem) {
        when(resolver.resolver(any())).thenReturn(quem);
    }

    private MockMultipartFile imagem() {
        return new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", JPEG);
    }

    @Test
    void operadorSobeFotoDeSaidaEOCaminhoEGeradoNoServidor() {
        FotoEmprestimoResponse r = service.enviar(jwt, emprestimo.getId(), MomentoFoto.SAIDA, imagem());

        assertEquals(MomentoFoto.SAIDA, r.momento());
        assertEquals("https://assinada/foto", r.url());
        verify(storage).enviar(argThat(c -> c.startsWith("emprestimos/") && c.endsWith(".jpg") && !c.contains("foto")),
                eq(JPEG), eq("image/jpeg"));
    }

    @Test
    void arquivoQueNaoEJpegNemPngERecusadoSemSubirNada() {
        MockMultipartFile texto = new MockMultipartFile("arquivo", "x.jpg", "image/jpeg", "nao sou imagem".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.SAIDA, texto));
        verify(storage, never()).enviar(any(), any(), any());
    }

    @Test
    void quemSoConsultaNaoSobeNemRemoveFoto() {
        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.SAIDA, imagem()));
        assertThrows(AccessDeniedException.class, () -> service.remover(jwt, emprestimo.getId(), UUID.randomUUID()));
    }

    @Test
    void fotoDeDevolucaoSoDepoisQueOEquipamentoSaiu() {
        assertThrows(EstadoInvalidoException.class, () -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.DEVOLUCAO, imagem()));

        emprestimo.setStatus(StatusEmprestimo.RETIRADO);
        assertDoesNotThrow(() -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.DEVOLUCAO, imagem()));
    }

    @Test
    void naoAnexaFotoDeSaidaAEmprestimoAindaAguardandoAutorizacao() {
        emprestimo.setStatus(StatusEmprestimo.AGUARDANDO_AUTORIZACAO);
        assertThrows(EstadoInvalidoException.class, () -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.SAIDA, imagem()));
    }

    @Test
    void limiteDeFotosPorEtapa() {
        when(fotoRepository.countByEmprestimoIdAndMomento(emprestimo.getId(), MomentoFoto.SAIDA))
                .thenReturn((long) EmprestimoFotoService.MAXIMO_POR_MOMENTO);
        assertThrows(EstadoInvalidoException.class, () -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.SAIDA, imagem()));
    }

    @Test
    void seOBancoRecusarOArquivoNaoFicaOrfao() {
        when(fotoRepository.save(any(EmprestimoFoto.class))).thenThrow(new RuntimeException("banco"));
        assertThrows(RuntimeException.class, () -> service.enviar(jwt, emprestimo.getId(), MomentoFoto.SAIDA, imagem()));
        verify(storage).remover(argThat(c -> c.startsWith("emprestimos/")));
    }


    @Test
    void profissionalSemPapelNaoVeAsFotos() {
        agir(consulta);
        emprestimo.setStatus(StatusEmprestimo.RETIRADO);
        assertThrows(AccessDeniedException.class, () -> service.listar(jwt, emprestimo.getId()));
    }

    @Test
    void removerApagaDoBancoEDoStorage() {
        EmprestimoFoto foto = new EmprestimoFoto(emprestimo, MomentoFoto.SAIDA, "emprestimos/abc.jpg", "image/jpeg", 5, operador);
        UUID fotoId = UUID.randomUUID();
        ReflectionTestUtils.setField(foto, "id", fotoId);
        when(fotoRepository.findById(fotoId)).thenReturn(Optional.of(foto));

        service.remover(jwt, emprestimo.getId(), fotoId);

        verify(fotoRepository).delete(foto);
        verify(storage).remover("emprestimos/abc.jpg");
    }

    @Test
    void naoRemoveFotoDeOutroEmprestimo() {
        Emprestimo outro = new Emprestimo("Maria", OffsetDateTime.now().plusDays(1), operador, 1);
        ReflectionTestUtils.setField(outro, "id", UUID.randomUUID());
        EmprestimoFoto foto = new EmprestimoFoto(outro, MomentoFoto.SAIDA, "emprestimos/x.jpg", "image/jpeg", 5, operador);
        UUID fotoId = UUID.randomUUID();
        ReflectionTestUtils.setField(foto, "id", fotoId);
        when(fotoRepository.findById(fotoId)).thenReturn(Optional.of(foto));

        assertThrows(RuntimeException.class, () -> service.remover(jwt, emprestimo.getId(), fotoId));
        verify(storage, never()).remover(any());
    }
}
