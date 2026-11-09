package com.expresofast.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expresofast.data.EmpresaLogisticaRepository;
import com.expresofast.data.VehiculoRepository;
import com.expresofast.domain.EmpresaLogistica;
import com.expresofast.dto.EmpresaLogisticaRequestDTO;
import com.expresofast.dto.EmpresaLogisticaResponseDTO;
import com.expresofast.exception.DuplicateResourceException;
import com.expresofast.exception.OperacionNoPermitidaException;
import com.expresofast.exception.ResourceNotFoundException;

@Service
public class EmpresaLogisticaService {

    private final EmpresaLogisticaRepository empresaLogisticaRepository;
    private final VehiculoRepository vehiculoRepository;

    public EmpresaLogisticaService(EmpresaLogisticaRepository empresaLogisticaRepository,
            VehiculoRepository vehiculoRepository) {
        this.empresaLogisticaRepository = empresaLogisticaRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    @Transactional(readOnly = true)
    public List<EmpresaLogisticaResponseDTO> obtenerTodas() {
        return empresaLogisticaRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaLogisticaResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarEmpresa(id));
    }

    @Transactional
    public EmpresaLogisticaResponseDTO registrarEmpresa(EmpresaLogisticaRequestDTO request) {
        if (empresaLogisticaRepository.existsByNombre(request.nombre())) {
            throw new DuplicateResourceException("Ya existe una empresa logistica con el nombre " + request.nombre());
        }
        if (empresaLogisticaRepository.existsByCedulaJuridica(request.cedulaJuridica())) {
            throw new DuplicateResourceException(
                    "Ya existe una empresa logistica con la cedula juridica " + request.cedulaJuridica());
        }

        EmpresaLogistica empresa = new EmpresaLogistica();
        empresa.setNombre(request.nombre());
        empresa.setCedulaJuridica(request.cedulaJuridica());
        empresa.setTelefono(request.telefono());
        empresa.setFechaRegistro(LocalDateTime.now());

        return toResponseDTO(empresaLogisticaRepository.save(empresa));
    }

    @Transactional
    public EmpresaLogisticaResponseDTO actualizar(Integer id, EmpresaLogisticaRequestDTO request) {
        EmpresaLogistica empresa = buscarEmpresa(id);

        if (empresaLogisticaRepository.existsByNombreAndIdNot(request.nombre(), id)) {
            throw new DuplicateResourceException("Ya existe otra empresa logistica con el nombre " + request.nombre());
        }
        if (empresaLogisticaRepository.existsByCedulaJuridicaAndIdNot(request.cedulaJuridica(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe otra empresa logistica con la cedula juridica " + request.cedulaJuridica());
        }

        empresa.setNombre(request.nombre());
        empresa.setCedulaJuridica(request.cedulaJuridica());
        empresa.setTelefono(request.telefono());

        return toResponseDTO(empresaLogisticaRepository.save(empresa));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!empresaLogisticaRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe la empresa logistica con ID " + id);
        }
        if (vehiculoRepository.existsByEmpresaId(id)) {
            throw new OperacionNoPermitidaException(
                    "No se puede eliminar la empresa logistica " + id + " porque tiene vehiculos registrados.");
        }
        empresaLogisticaRepository.deleteById(id);
    }

    private EmpresaLogistica buscarEmpresa(Integer id) {
        return empresaLogisticaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe la empresa logistica con ID " + id));
    }

    private EmpresaLogisticaResponseDTO toResponseDTO(EmpresaLogistica empresa) {
        return new EmpresaLogisticaResponseDTO(
                empresa.getId(),
                empresa.getNombre(),
                empresa.getCedulaJuridica(),
                empresa.getTelefono(),
                empresa.getFechaRegistro());
    }
}
