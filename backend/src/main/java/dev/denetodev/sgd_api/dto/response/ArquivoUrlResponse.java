package dev.denetodev.sgd_api.dto.response;

/** URL assinada para ver o arquivo de uma evidência; some depois de {@code validadeSegundos}. */
public record ArquivoUrlResponse(String url, int validadeSegundos) {
}
