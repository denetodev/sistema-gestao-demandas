package dev.denetodev.sgd_api.storage;

/** O Storage não está configurado ou não respondeu: vira 503 na API. */
public class ArmazenamentoIndisponivelException extends RuntimeException {

    public ArmazenamentoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
