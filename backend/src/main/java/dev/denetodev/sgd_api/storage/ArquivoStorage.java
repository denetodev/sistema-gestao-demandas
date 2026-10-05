package dev.denetodev.sgd_api.storage;

/** Guarda arquivos de evidência fora do banco. Hoje a implementação é o Supabase Storage. */
public interface ArquivoStorage {

    /** Sobe o arquivo no caminho informado (o chamador gera o caminho, nunca o usuário). */
    void enviar(String caminho, byte[] conteudo, String contentType);

    /** URL temporária de leitura; o bucket é privado. */
    String urlAssinada(String caminho, int validadeSegundos);

    /** Remove o arquivo; falha de limpeza não deve derrubar quem chamou. */
    void remover(String caminho);
}
