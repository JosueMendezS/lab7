package com.expresofast.dto;

import java.math.BigDecimal;

public record VehiculoResponseDTO(
        Integer id,
        String placa,
        BigDecimal capacidadKg,
        String estado,
        Integer empresaId,
        String nombreEmpresa,
        Integer conductorId,
        String nombreConductor) {
}
