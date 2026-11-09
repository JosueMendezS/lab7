package com.expresofast.dto;

import jakarta.validation.constraints.NotBlank;

public record CambioEstadoDTO(
        @NotBlank(message = "El nuevo estado es obligatorio") String nuevoEstado,
        String observaciones) {
}