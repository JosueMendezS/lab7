package com.expresofast.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expresofast.dto.BitacoraResponseDTO;
import com.expresofast.dto.CambioEstadoDTO;
import com.expresofast.dto.EnvioRequestDTO;
import com.expresofast.dto.EnvioResponseDTO;
import com.expresofast.dto.TarifaResponseDTO;
import com.expresofast.service.EnvioService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/envios")
@CrossOrigin(origins = "*")
public class EnvioController {

    private final EnvioService envioService;

    public EnvioController(EnvioService envioService) {
        this.envioService = envioService;
    }

    @GetMapping("/optimizados")
    public ResponseEntity<List<EnvioResponseDTO>> obtenerEnviosOptimizados() {
        return ResponseEntity.ok(envioService.obtenerEnviosOptimizados());
    }

    @GetMapping("/tarifa")
    public ResponseEntity<TarifaResponseDTO> calcularTarifa(@RequestParam("pesoKg") double pesoKg,
            @RequestParam("distanciaKm") double distanciaKm) {
        double tarifa = envioService.calcularTarifa(pesoKg, distanciaKm);
        return ResponseEntity.ok(new TarifaResponseDTO(pesoKg, distanciaKm, tarifa));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EnvioResponseDTO> obtenerPorId(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(envioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<EnvioResponseDTO> registrarEnvio(@Valid @RequestBody EnvioRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(envioService.crearEnvio(dto));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<EnvioResponseDTO> actualizarEstado(@PathVariable("id") Integer id,
            @Valid @RequestBody CambioEstadoDTO dto) {
        return ResponseEntity.ok(envioService.actualizarEstado(id, dto.nuevoEstado(), dto.observaciones()));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<EnvioResponseDTO> cancelarEnvio(@PathVariable("id") Integer id,
            @RequestParam(value = "motivo", required = false) String motivo) {
        return ResponseEntity.ok(envioService.cancelarEnvio(id, motivo));
    }

    @GetMapping("/{id}/bitacora")
    public ResponseEntity<List<BitacoraResponseDTO>> obtenerBitacora(@PathVariable("id") Integer id) {
        return ResponseEntity.ok(envioService.obtenerBitacora(id));
    }
}
