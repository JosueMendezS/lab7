package com.expresofast.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record VehiculoRequestDTO(
        @NotBlank(message = "La placa es obligatoria") String placa,

        @NotNull(message = "La capacidad es obligatoria") @Positive(message = "La capacidad debe ser mayor a cero") BigDecimal capacidadKg,

        @NotBlank(message = "El estado es obligatorio") @Pattern(regexp = "DISPONIBLE|EN_RUTA|MANTENIMIENTO", message = "Estado invalido. Valores permitidos: DISPONIBLE, EN_RUTA, MANTENIMIENTO") String estado,

        @NotNull(message = "Debe indicar el ID de la empresa") Integer empresaId) {
}