package com.expresofast.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expresofast.data.ConductorRepository;
import com.expresofast.data.EmpresaLogisticaRepository;
import com.expresofast.data.VehiculoRepository;
import com.expresofast.domain.Conductor;
import com.expresofast.domain.EmpresaLogistica;
import com.expresofast.domain.Vehiculo;
import com.expresofast.dto.VehiculoRequestDTO;
import com.expresofast.dto.VehiculoResponseDTO;
import com.expresofast.exception.ConductorInactivoException;
import com.expresofast.exception.DuplicateResourceException;
import com.expresofast.exception.ResourceNotFoundException;

@Service
public class VehiculoService {

    private final VehiculoRepository vehiculoRepository;
    private final EmpresaLogisticaRepository empresaLogisticaRepository;
    private final ConductorRepository conductorRepository;

    public VehiculoService(VehiculoRepository vehiculoRepository,
            EmpresaLogisticaRepository empresaLogisticaRepository,
            ConductorRepository conductorRepository) {
        this.vehiculoRepository = vehiculoRepository;
        this.empresaLogisticaRepository = empresaLogisticaRepository;
        this.conductorRepository = conductorRepository;
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponseDTO> obtenerTodos() {
        return vehiculoRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public VehiculoResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarVehiculo(id));
    }

    @Transactional
    public VehiculoResponseDTO registrarVehiculo(VehiculoRequestDTO request) {
        if (vehiculoRepository.existsByPlaca(request.placa())) {
            throw new DuplicateResourceException("Ya existe un vehiculo con la placa " + request.placa());
        }

        EmpresaLogistica empresa = buscarEmpresa(request.empresaId());

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPlaca(request.placa());
        vehiculo.setCapacidadKg(request.capacidadKg());
        vehiculo.setEstado(request.estado());
        vehiculo.setEmpresa(empresa);

        return toResponseDTO(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public VehiculoResponseDTO actualizar(Integer id, VehiculoRequestDTO request) {
        Vehiculo vehiculo = buscarVehiculo(id);

        if (vehiculoRepository.existsByPlacaAndIdNot(request.placa(), id)) {
            throw new DuplicateResourceException("Ya existe otro vehiculo con la placa " + request.placa());
        }

        EmpresaLogistica empresa = buscarEmpresa(request.empresaId());

        vehiculo.setPlaca(request.placa());
        vehiculo.setCapacidadKg(request.capacidadKg());
        vehiculo.setEstado(request.estado());
        vehiculo.setEmpresa(empresa);

        return toResponseDTO(vehiculo);
    }

    @Transactional
    public VehiculoResponseDTO asignarConductor(Integer vehiculoId, Integer conductorId) {
        Vehiculo vehiculo = buscarVehiculo(vehiculoId);

        Conductor conductor = conductorRepository.findById(conductorId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el conductor con ID " + conductorId));

        if (!Boolean.TRUE.equals(conductor.getActivo())) {
            throw new ConductorInactivoException(
                    "El conductor " + conductor.getNombre() + " " + conductor.getApellidos()
                            + " esta inactivo y no puede asignarse al vehiculo " + vehiculo.getPlaca() + ".");
        }

        vehiculo.setConductor(conductor);
        return toResponseDTO(vehiculoRepository.save(vehiculo));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!vehiculoRepository.existsById(id)) {
            throw new ResourceNotFoundException("No existe el vehiculo con ID " + id);
        }
        vehiculoRepository.deleteById(id);
    }

    private Vehiculo buscarVehiculo(Integer id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el vehiculo con ID " + id));
    }

    private EmpresaLogistica buscarEmpresa(Integer empresaId) {
        return empresaLogisticaRepository.findById(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe la empresa logistica con ID " + empresaId));
    }

    private VehiculoResponseDTO toResponseDTO(Vehiculo vehiculo) {
        EmpresaLogistica empresa = vehiculo.getEmpresa();
        Conductor conductor = vehiculo.getConductor();

        return new VehiculoResponseDTO(
                vehiculo.getId(),
                vehiculo.getPlaca(),
                vehiculo.getCapacidadKg(),
                vehiculo.getEstado(),
                empresa != null ? empresa.getId() : null,
                empresa != null ? empresa.getNombre() : null,
                conductor != null ? conductor.getId() : null,
                conductor != null ? conductor.getNombre() + " " + conductor.getApellidos() : null);
    }
}
