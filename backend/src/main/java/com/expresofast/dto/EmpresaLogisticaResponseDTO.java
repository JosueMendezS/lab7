package com.expresofast.dto;

import java.time.LocalDateTime;

public record EmpresaLogisticaResponseDTO(
        Integer id,
        String nombre,
        String cedulaJuridica,
        String telefono,
        LocalDateTime fechaRegistro) {
}
