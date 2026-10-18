package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.storage.ArquivoStorage;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Foto de perfil da própria pessoa. JPEG ou PNG até 2 MB (o navegador já reduz a imagem antes de enviar).
 * Cada envio ganha um nome novo, para o navegador não mostrar a foto antiga em cache, e a anterior é apagada.
 */
@Service
@Transactional
public class PessoaFotoService {

    static final int TAMANHO_MAXIMO_BYTES = 2 * 1024 * 1024;

    private final CurrentPessoaResolver currentPessoaResolver;
    private final ArquivoStorage storage;
    private final PessoaService pessoaService;

    public PessoaFotoService(CurrentPessoaResolver currentPessoaResolver, ArquivoStorage storage, PessoaService pessoaService) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.storage = storage;
        this.pessoaService = pessoaService;
    }

    public PessoaResponse enviar(Jwt jwt, MultipartFile arquivo) {
        Pessoa pessoa = currentPessoaResolver.resolver(jwt);
        byte[] bytes = lerBytes(arquivo);
        String mime = EvidenciaService.detectarMime(bytes);

        String caminho = "perfis/" + pessoa.getId() + "-" + UUID.randomUUID() + (mime.equals("image/png") ? ".png" : ".jpg");
        String anterior = storage.caminhoPublico(pessoa.getFotoUrl());
        pessoa.setFotoUrl(storage.enviarPublico(caminho, bytes, mime));
        if (anterior != null) {
            storage.removerPublico(anterior);
        }
        return pessoaService.paraResposta(pessoa);
    }

    public PessoaResponse remover(Jwt jwt) {
        Pessoa pessoa = currentPessoaResolver.resolver(jwt);
        String anterior = storage.caminhoPublico(pessoa.getFotoUrl());
        pessoa.setFotoUrl(null);
        if (anterior != null) {
            storage.removerPublico(anterior);
        }
        return pessoaService.paraResposta(pessoa);
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de imagem");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new IllegalArgumentException("Imagem acima de 2 MB");
        }
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }
}
