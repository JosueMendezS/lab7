package com.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EnvioRequestDTO(
        @NotBlank(message = "El código de rastreo es obligatorio") @Pattern(regexp = "^EXP-\\d{4}$", message = "Formato inválido. Ejemplo: EXP-1234") String codigoRastreo,

        @NotBlank(message = "La dirección de destino es obligatoria") String direccionDestino,

        @NotNull(message = "El peso es obligatorio") @Positive(message = "El peso debe ser mayor a cero") BigDecimal pesoKg,

        @NotNull(message = "El costo es obligatorio") @Positive(message = "El costo debe ser mayor a cero") BigDecimal costo,

        @NotNull(message = "Debe indicar el ID del vehículo") Integer vehiculoId,

        @NotNull(message = "Debe indicar el ID del conductor") Integer conductorId) {
}