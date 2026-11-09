package com.expresofast.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmpresaLogisticaRequestDTO(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 100, message = "El nombre admite maximo 100 caracteres") String nombre,

        @NotBlank(message = "La cedula juridica es obligatoria") @Size(max = 20, message = "La cedula juridica admite maximo 20 caracteres") String cedulaJuridica,

        @NotBlank(message = "El telefono es obligatorio") @Size(max = 20, message = "El telefono admite maximo 20 caracteres") String telefono) {
}
