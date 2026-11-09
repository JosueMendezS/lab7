package com.expresofast.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
@DisplayName("VehiculoService - pruebas unitarias con Mockito")
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private EmpresaLogisticaRepository empresaLogisticaRepository;
    @Mock
    private ConductorRepository conductorRepository;

    @InjectMocks
    private VehiculoService vehiculoService;

    private EmpresaLogistica empresa;
    private Vehiculo vehiculo;
    private Conductor conductor;
    private VehiculoRequestDTO solicitud;

    @BeforeEach
    void setUp() {
        empresa = new EmpresaLogistica();
        empresa.setId(1);
        empresa.setNombre("Transportes del Atlantico");

        vehiculo = new Vehiculo();
        vehiculo.setId(7);
        vehiculo.setPlaca("CL-123456");
        vehiculo.setCapacidadKg(new BigDecimal("1500.00"));
        vehiculo.setEstado("DISPONIBLE");
        vehiculo.setEmpresa(empresa);

        conductor = new Conductor();
        conductor.setId(3);
        conductor.setNombre("Ana");
        conductor.setApellidos("Solano Brenes");
        conductor.setActivo(Boolean.TRUE);

        solicitud = new VehiculoRequestDTO("CL-123456", new BigDecimal("1500.00"), "DISPONIBLE", 1);
    }

    // -------------------------------------------------------- registrarVehiculo

    @Test
    @DisplayName("registrarVehiculo: datos validos -> retorna DTO con la empresa asociada")
    void registrarVehiculo_DatosValidos_RetornaDTO() {
        when(vehiculoRepository.existsByPlaca("CL-123456")).thenReturn(false);
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.of(empresa));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocacion -> {
            Vehiculo guardado = invocacion.getArgument(0);
            guardado.setId(50);
            return guardado;
        });

        VehiculoResponseDTO resultado = vehiculoService.registrarVehiculo(solicitud);

        assertAll(
                () -> assertEquals(50, resultado.id()),
                () -> assertEquals("CL-123456", resultado.placa()),
                () -> assertEquals(1, resultado.empresaId()),
                () -> assertEquals("Transportes del Atlantico", resultado.nombreEmpresa()),
                () -> assertNull(resultado.conductorId()));
    }

    @Test
    @DisplayName("registrarVehiculo: placa ya registrada -> DuplicateResourceException")
    void registrarVehiculo_PlacaDuplicada_LanzaExcepcion() {
        when(vehiculoRepository.existsByPlaca("CL-123456")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> vehiculoService.registrarVehiculo(solicitud));

        assertTrue(ex.getMessage().contains("CL-123456"));
        verify(vehiculoRepository).existsByPlaca("CL-123456");
        verify(vehiculoRepository, never()).save(any());
        verifyNoInteractions(empresaLogisticaRepository);
    }

    @Test
    @DisplayName("registrarVehiculo: empresa inexistente -> ResourceNotFoundException")
    void registrarVehiculo_EmpresaInexistente_LanzaExcepcion() {
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.registrarVehiculo(solicitud));

        verify(vehiculoRepository, never()).save(any());
    }

    // --------------------------------------------------------- asignarConductor

    @Test
    @DisplayName("asignarConductor: conductor inactivo -> ConductorInactivoException")
    void asignarConductor_ConductorInactivo_LanzaExcepcion() {
        conductor.setActivo(Boolean.FALSE);
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(3)).thenReturn(Optional.of(conductor));

        ConductorInactivoException ex = assertThrows(ConductorInactivoException.class,
                () -> vehiculoService.asignarConductor(7, 3));

        assertTrue(ex.getMessage().contains("inactivo"));
        assertNull(vehiculo.getConductor());
        verify(vehiculoRepository, never()).save(any());
    }

    @ParameterizedTest(name = "[{index}] activo={0}")
    @NullSource
    @ValueSource(booleans = false)
    @DisplayName("asignarConductor: estado activo falso o desconocido se trata como inactivo")
    void asignarConductor_EstadoActivoNoVerdadero_LanzaExcepcion(Boolean activo) {
        conductor.setActivo(activo);
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(3)).thenReturn(Optional.of(conductor));

        assertThrows(ConductorInactivoException.class, () -> vehiculoService.asignarConductor(7, 3));
    }

    @Test
    @DisplayName("asignarConductor: conductor activo -> queda asignado al vehiculo")
    void asignarConductor_ConductorActivo_AsignaConductor() {
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(3)).thenReturn(Optional.of(conductor));
        when(vehiculoRepository.save(vehiculo)).thenReturn(vehiculo);

        VehiculoResponseDTO resultado = vehiculoService.asignarConductor(7, 3);

        assertEquals(3, resultado.conductorId());
        assertEquals("Ana Solano Brenes", resultado.nombreConductor());
        assertEquals(conductor, vehiculo.getConductor());
    }

    @Test
    @DisplayName("asignarConductor: vehiculo inexistente -> ResourceNotFoundException")
    void asignarConductor_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(7)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.asignarConductor(7, 3));

        verifyNoInteractions(conductorRepository);
    }

    @Test
    @DisplayName("asignarConductor: conductor inexistente -> ResourceNotFoundException")
    void asignarConductor_ConductorInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(3)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.asignarConductor(7, 3));
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerTodos: mapea vehiculos con y sin empresa asociada")
    void obtenerTodos_RetornaListaDTO() {
        Vehiculo sinEmpresa = new Vehiculo();
        sinEmpresa.setId(8);
        sinEmpresa.setPlaca("CL-999999");
        when(vehiculoRepository.findAll()).thenReturn(List.of(vehiculo, sinEmpresa));

        List<VehiculoResponseDTO> resultado = vehiculoService.obtenerTodos();

        assertEquals(2, resultado.size());
        assertEquals("Transportes del Atlantico", resultado.get(0).nombreEmpresa());
        assertNull(resultado.get(1).empresaId());
        assertNull(resultado.get(1).nombreEmpresa());
    }

    @Test
    @DisplayName("obtenerPorId: vehiculo existente -> retorna DTO")
    void obtenerPorId_VehiculoExistente_RetornaDTO() {
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));

        assertEquals("CL-123456", vehiculoService.obtenerPorId(7).placa());
    }

    @Test
    @DisplayName("obtenerPorId: vehiculo inexistente -> ResourceNotFoundException")
    void obtenerPorId_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.obtenerPorId(99));
    }

    // --------------------------------------------------------------- actualizar

    @Test
    @DisplayName("actualizar: datos validos -> modifica placa, capacidad y estado")
    void actualizar_DatosValidos_ActualizaVehiculo() {
        VehiculoRequestDTO cambios = new VehiculoRequestDTO("CL-654321", new BigDecimal("2000.00"),
                "MANTENIMIENTO", 1);
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByPlacaAndIdNot("CL-654321", 7)).thenReturn(false);
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.of(empresa));

        VehiculoResponseDTO resultado = vehiculoService.actualizar(7, cambios);

        assertEquals("CL-654321", resultado.placa());
        assertEquals("MANTENIMIENTO", resultado.estado());
        assertEquals(0, new BigDecimal("2000.00").compareTo(resultado.capacidadKg()));
    }

    @Test
    @DisplayName("actualizar: placa usada por otro vehiculo -> DuplicateResourceException")
    void actualizar_PlacaDeOtroVehiculo_LanzaExcepcion() {
        when(vehiculoRepository.findById(7)).thenReturn(Optional.of(vehiculo));
        when(vehiculoRepository.existsByPlacaAndIdNot("CL-123456", 7)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> vehiculoService.actualizar(7, solicitud));

        verifyNoInteractions(empresaLogisticaRepository);
    }

    @Test
    @DisplayName("actualizar: vehiculo inexistente -> ResourceNotFoundException")
    void actualizar_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.actualizar(99, solicitud));
    }

    // ----------------------------------------------------------------- eliminar

    @Test
    @DisplayName("eliminar: vehiculo existente -> se elimina")
    void eliminar_VehiculoExistente_Elimina() {
        when(vehiculoRepository.existsById(7)).thenReturn(true);

        vehiculoService.eliminar(7);

        verify(vehiculoRepository).deleteById(7);
    }

    @Test
    @DisplayName("eliminar: vehiculo inexistente -> ResourceNotFoundException")
    void eliminar_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> vehiculoService.eliminar(99));

        verify(vehiculoRepository, never()).deleteById(any());
    }
}
