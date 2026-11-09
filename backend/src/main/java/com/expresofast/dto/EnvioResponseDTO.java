package com.expresofast.dto;

import java.math.BigDecimal;

public record EnvioResponseDTO(
        Integer id,
        String codigoRastreo,
        String direccionDestino,
        BigDecimal pesoKg,
        BigDecimal costo,
        String estadoEnvio,
        String placaVehiculo,
        String nombreConductor) {
}