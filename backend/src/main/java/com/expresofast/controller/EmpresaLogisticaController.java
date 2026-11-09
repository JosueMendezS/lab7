package com.expresofast.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expresofast.dto.EmpresaLogisticaRequestDTO;
import com.expresofast.dto.EmpresaLogisticaResponseDTO;
import com.expresofast.service.EmpresaLogisticaService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaLogisticaController {

    private final EmpresaLogisticaService empresaLogisticaService;

    public EmpresaLogisticaController(EmpresaLogisticaService empresaLogisticaService) {
        this.empresaLogisticaService = empresaLogisticaService;
    }

    @GetMapping
    public ResponseEntity<List<EmpresaLogisticaResponseDTO>> obtenerTodas() {
        return ResponseEntity.ok(empresaLogisticaService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmpresaLogisticaResponseDTO> obtenerPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(empresaLogisticaService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EmpresaLogisticaResponseDTO> registrar(
            @Valid @RequestBody EmpresaLogisticaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(empresaLogisticaService.registrarEmpresa(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmpresaLogisticaResponseDTO> actualizar(@PathVariable Integer id,
            @Valid @RequestBody EmpresaLogisticaRequestDTO request) {
        return ResponseEntity.ok(empresaLogisticaService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        empresaLogisticaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
