package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.ControleEquipamento;

import java.util.UUID;

public record CategoriaEquipamentoResponse(UUID id, String nome, ControleEquipamento controle, boolean ativo) {
}
