package com.expresofast.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expresofast.data.EmpresaLogisticaRepository;
import com.expresofast.data.VehiculoRepository;
import com.expresofast.domain.EmpresaLogistica;
import com.expresofast.dto.EmpresaLogisticaRequestDTO;
import com.expresofast.dto.EmpresaLogisticaResponseDTO;
import com.expresofast.exception.DuplicateResourceException;
import com.expresofast.exception.OperacionNoPermitidaException;
import com.expresofast.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmpresaLogisticaService - pruebas unitarias con Mockito")
class EmpresaLogisticaServiceTest {

    @Mock
    private EmpresaLogisticaRepository empresaLogisticaRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;

    @InjectMocks
    private EmpresaLogisticaService empresaLogisticaService;

    private EmpresaLogistica empresa;
    private EmpresaLogisticaRequestDTO solicitud;

    @BeforeEach
    void setUp() {
        empresa = new EmpresaLogistica();
        empresa.setId(1);
        empresa.setNombre("Transportes del Atlantico");
        empresa.setCedulaJuridica("3-101-123456");
        empresa.setTelefono("2574-0000");
        empresa.setFechaRegistro(LocalDateTime.of(2026, 1, 15, 8, 0));

        solicitud = new EmpresaLogisticaRequestDTO("Transportes del Atlantico", "3-101-123456", "2574-0000");
    }

    // --------------------------------------------------------- registrarEmpresa

    @Test
    @DisplayName("registrarEmpresa: datos validos -> retorna DTO con fecha de registro")
    void registrarEmpresa_DatosValidos_RetornaDTO() {
        when(empresaLogisticaRepository.save(any(EmpresaLogistica.class))).thenAnswer(invocacion -> {
            EmpresaLogistica guardada = invocacion.getArgument(0);
            guardada.setId(20);
            return guardada;
        });

        EmpresaLogisticaResponseDTO resultado = empresaLogisticaService.registrarEmpresa(solicitud);

        assertAll(
                () -> assertEquals(20, resultado.id()),
                () -> assertEquals("Transportes del Atlantico", resultado.nombre()),
                () -> assertEquals("3-101-123456", resultado.cedulaJuridica()),
                () -> assertNotNull(resultado.fechaRegistro()));

        ArgumentCaptor<EmpresaLogistica> captor = ArgumentCaptor.forClass(EmpresaLogistica.class);
        verify(empresaLogisticaRepository).save(captor.capture());
        assertEquals("2574-0000", captor.getValue().getTelefono());
    }

    @Test
    @DisplayName("registrarEmpresa: nombre repetido -> DuplicateResourceException")
    void registrarEmpresa_NombreDuplicado_LanzaExcepcion() {
        when(empresaLogisticaRepository.existsByNombre("Transportes del Atlantico")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> empresaLogisticaService.registrarEmpresa(solicitud));

        verify(empresaLogisticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarEmpresa: cedula juridica repetida -> DuplicateResourceException")
    void registrarEmpresa_CedulaDuplicada_LanzaExcepcion() {
        when(empresaLogisticaRepository.existsByNombre("Transportes del Atlantico")).thenReturn(false);
        when(empresaLogisticaRepository.existsByCedulaJuridica("3-101-123456")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> empresaLogisticaService.registrarEmpresa(solicitud));

        verify(empresaLogisticaRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerTodas: retorna todas las empresas mapeadas")
    void obtenerTodas_RetornaListaDTO() {
        EmpresaLogistica otra = new EmpresaLogistica();
        otra.setId(2);
        otra.setNombre("Logistica Caribe");
        when(empresaLogisticaRepository.findAll()).thenReturn(List.of(empresa, otra));

        List<EmpresaLogisticaResponseDTO> resultado = empresaLogisticaService.obtenerTodas();

        assertEquals(2, resultado.size());
        assertEquals("Logistica Caribe", resultado.get(1).nombre());
    }

    @Test
    @DisplayName("obtenerPorId: empresa existente -> retorna DTO")
    void obtenerPorId_EmpresaExistente_RetornaDTO() {
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.of(empresa));

        assertEquals("3-101-123456", empresaLogisticaService.obtenerPorId(1).cedulaJuridica());
    }

    @Test
    @DisplayName("obtenerPorId: empresa inexistente -> ResourceNotFoundException")
    void obtenerPorId_EmpresaInexistente_LanzaExcepcion() {
        when(empresaLogisticaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empresaLogisticaService.obtenerPorId(99));
    }

    // --------------------------------------------------------------- actualizar

    @Test
    @DisplayName("actualizar: datos validos -> modifica la empresa")
    void actualizar_DatosValidos_ActualizaEmpresa() {
        EmpresaLogisticaRequestDTO cambios = new EmpresaLogisticaRequestDTO("Atlantico Express", "3-101-123456",
                "2574-1111");
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.of(empresa));
        when(empresaLogisticaRepository.existsByNombreAndIdNot("Atlantico Express", 1)).thenReturn(false);
        when(empresaLogisticaRepository.existsByCedulaJuridicaAndIdNot("3-101-123456", 1)).thenReturn(false);
        when(empresaLogisticaRepository.save(empresa)).thenReturn(empresa);

        EmpresaLogisticaResponseDTO resultado = empresaLogisticaService.actualizar(1, cambios);

        assertEquals("Atlantico Express", resultado.nombre());
        assertEquals("2574-1111", resultado.telefono());
    }

    @Test
    @DisplayName("actualizar: nombre de otra empresa -> DuplicateResourceException")
    void actualizar_NombreDeOtraEmpresa_LanzaExcepcion() {
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.of(empresa));
        when(empresaLogisticaRepository.existsByNombreAndIdNot("Transportes del Atlantico", 1)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> empresaLogisticaService.actualizar(1, solicitud));

        verify(empresaLogisticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: cedula de otra empresa -> DuplicateResourceException")
    void actualizar_CedulaDeOtraEmpresa_LanzaExcepcion() {
        when(empresaLogisticaRepository.findById(1)).thenReturn(Optional.of(empresa));
        when(empresaLogisticaRepository.existsByNombreAndIdNot("Transportes del Atlantico", 1)).thenReturn(false);
        when(empresaLogisticaRepository.existsByCedulaJuridicaAndIdNot("3-101-123456", 1)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> empresaLogisticaService.actualizar(1, solicitud));

        verify(empresaLogisticaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizar: empresa inexistente -> ResourceNotFoundException")
    void actualizar_EmpresaInexistente_LanzaExcepcion() {
        when(empresaLogisticaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> empresaLogisticaService.actualizar(99, solicitud));
    }

    // ----------------------------------------------------------------- eliminar

    @Test
    @DisplayName("eliminar: empresa sin vehiculos -> se elimina")
    void eliminar_EmpresaSinVehiculos_Elimina() {
        when(empresaLogisticaRepository.existsById(1)).thenReturn(true);
        when(vehiculoRepository.existsByEmpresaId(1)).thenReturn(false);

        empresaLogisticaService.eliminar(1);

        verify(empresaLogisticaRepository).deleteById(1);
    }

    @Test
    @DisplayName("eliminar: empresa con vehiculos -> OperacionNoPermitidaException")
    void eliminar_EmpresaConVehiculos_LanzaExcepcion() {
        when(empresaLogisticaRepository.existsById(1)).thenReturn(true);
        when(vehiculoRepository.existsByEmpresaId(1)).thenReturn(true);

        assertThrows(OperacionNoPermitidaException.class, () -> empresaLogisticaService.eliminar(1));

        verify(empresaLogisticaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar: empresa inexistente -> ResourceNotFoundException")
    void eliminar_EmpresaInexistente_LanzaExcepcion() {
        when(empresaLogisticaRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> empresaLogisticaService.eliminar(99));

        verifyNoInteractions(vehiculoRepository);
    }
}
