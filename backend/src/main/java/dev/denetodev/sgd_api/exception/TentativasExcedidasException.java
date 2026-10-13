package dev.denetodev.sgd_api.exception;

/** Muitas tentativas seguidas numa operação sensível (vira HTTP 429). */
public class TentativasExcedidasException extends RuntimeException {

    public TentativasExcedidasException(String mensagem) {
        super(mensagem);
    }
}
