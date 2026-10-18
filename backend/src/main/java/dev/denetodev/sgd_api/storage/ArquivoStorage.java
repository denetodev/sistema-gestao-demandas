package dev.denetodev.sgd_api.storage;

/** Guarda arquivos de evidência fora do banco. Hoje a implementação é o Supabase Storage. */
public interface ArquivoStorage {

    /** Sobe o arquivo no caminho informado (o chamador gera o caminho, nunca o usuário). */
    void enviar(String caminho, byte[] conteudo, String contentType);

    /** URL temporária de leitura; o bucket é privado. */
    String urlAssinada(String caminho, int validadeSegundos);

    /** Remove o arquivo; falha de limpeza não deve derrubar quem chamou. */
    void remover(String caminho);

    /**
     * Foto de perfil: vai para um bucket público (avatares), porque a imagem aparece em listas e no topo da tela
     * e uma tag de imagem não manda o token. Devolve a URL pública permanente.
     */
    String enviarPublico(String caminho, byte[] conteudo, String contentType);

    /** Remove uma foto pública pelo caminho (o que vem depois do nome do bucket na URL). */
    void removerPublico(String caminho);

    /** O caminho de uma URL pública nossa, ou nulo se a URL não for do bucket de avatares. */
    String caminhoPublico(String url);
}
