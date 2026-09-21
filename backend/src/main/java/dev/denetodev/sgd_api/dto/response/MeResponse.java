package dev.denetodev.sgd_api.dto.response;

public record MeResponse(
        boolean vinculado,
        PessoaResponse pessoa // null quando vinculado = false
) {
}