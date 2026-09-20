package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VincularAuthRequest(
        @NotNull(message = "authUserId é obrigatório") UUID authUserId
) {
}