package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.PapelPessoaDemanda;

import java.time.LocalDate;
import java.util.UUID;

public record ParticipanteResponse(
        UUID id,
        UUID pessoaId,
        String pessoaNome,
        PapelPessoaDemanda papel,
        LocalDate dataEntrada,
        LocalDate dataSaida,
        String observacao
) {
}