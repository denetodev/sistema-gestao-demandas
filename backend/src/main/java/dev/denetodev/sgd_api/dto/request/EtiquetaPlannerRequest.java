package dev.denetodev.sgd_api.dto.request;

/** O texto exato da etiqueta da pessoa no Planner (ex.: "CRIATIVO - Thais"). Vazio ou nulo limpa. */
public record EtiquetaPlannerRequest(String etiqueta) {
}
