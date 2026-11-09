package com.expresofast.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.expresofast.data.BitacoraEnvioRepository;
import com.expresofast.data.ConductorRepository;
import com.expresofast.data.EnvioRepository;
import com.expresofast.data.UsuarioRepository;
import com.expresofast.data.VehiculoRepository;
import com.expresofast.domain.BitacoraEnvio;
import com.expresofast.domain.Conductor;
import com.expresofast.domain.Envio;
import com.expresofast.domain.Usuario;
import com.expresofast.domain.Vehiculo;
import com.expresofast.dto.BitacoraResponseDTO;
import com.expresofast.dto.EnvioRequestDTO;
import com.expresofast.dto.EnvioResponseDTO;
import com.expresofast.exception.CapacidadExcedidaException;
import com.expresofast.exception.DuplicateResourceException;
import com.expresofast.exception.InvalidStateTransitionException;
import com.expresofast.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnvioService - pruebas unitarias con Mockito")
class EnvioServiceTest {

    private static final Integer ENVIO_ID = 10;

    @Mock
    private EnvioRepository envioRepository;
    @Mock
    private VehiculoRepository vehiculoRepository;
    @Mock
    private ConductorRepository conductorRepository;
    @Mock
    private BitacoraEnvioRepository bitacoraEnvioRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private EnvioService envioService;

    private Vehiculo vehiculo;
    private Conductor conductor;

    @BeforeEach
    void setUp() {
        vehiculo = new Vehiculo();
        vehiculo.setId(1);
        vehiculo.setPlaca("CL-123456");
        vehiculo.setCapacidadKg(new BigDecimal("500.00"));
        vehiculo.setEstado("DISPONIBLE");

        conductor = new Conductor();
        conductor.setId(2);
        conductor.setNombre("Jose");
        conductor.setApellidos("Fernandez");
        conductor.setActivo(Boolean.TRUE);
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------ helpers

    private EnvioRequestDTO solicitud(String pesoKg) {
        return new EnvioRequestDTO("EXP-1234", "Paraiso, Cartago", new BigDecimal(pesoKg),
                new BigDecimal("7500.00"), vehiculo.getId(), conductor.getId());
    }

    private Envio envioEnEstado(String estado) {
        Envio envio = new Envio();
        envio.setId(ENVIO_ID);
        envio.setCodigoRastreo("EXP-0010");
        envio.setDireccionDestino("Turrialba centro");
        envio.setPesoKg(new BigDecimal("15.00"));
        envio.setCosto(new BigDecimal("7500.00"));
        envio.setEstadoEnvio(estado);
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);
        return envio;
    }

    private Usuario autenticarComo(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, null, List.of()));
        Usuario usuario = new Usuario();
        usuario.setId(3);
        usuario.setUsername(username);
        when(usuarioRepository.findByUsername(username)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    // --------------------------------------------------------------- crearEnvio

    @Test
    @DisplayName("crearEnvio: datos validos -> retorna DTO con estado inicial PENDIENTE")
    void crearEnvio_DatosValidos_RetornaEnvioDTO() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(2)).thenReturn(Optional.of(conductor));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> {
            Envio guardado = invocacion.getArgument(0);
            guardado.setId(100);
            return guardado;
        });

        EnvioResponseDTO resultado = envioService.crearEnvio(solicitud("15.00"));

        assertNotNull(resultado);
        assertAll(
                () -> assertEquals(100, resultado.id()),
                () -> assertEquals("EXP-1234", resultado.codigoRastreo()),
                () -> assertEquals("PENDIENTE", resultado.estadoEnvio()),
                () -> assertEquals("CL-123456", resultado.placaVehiculo()),
                () -> assertEquals("Jose Fernandez", resultado.nombreConductor()));

        ArgumentCaptor<Envio> captor = ArgumentCaptor.forClass(Envio.class);
        verify(envioRepository).save(captor.capture());
        assertEquals(EnvioService.PENDIENTE, captor.getValue().getEstadoEnvio());
        assertEquals(vehiculo, captor.getValue().getVehiculo());
        assertEquals(conductor, captor.getValue().getConductor());
    }

    @Test
    @DisplayName("crearEnvio: peso igual a la capacidad maxima -> se permite (limite)")
    void crearEnvio_PesoIgualACapacidad_SeRegistra() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(2)).thenReturn(Optional.of(conductor));
        when(envioRepository.save(any(Envio.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        EnvioResponseDTO resultado = envioService.crearEnvio(solicitud("500.00"));

        assertEquals(0, new BigDecimal("500.00").compareTo(resultado.pesoKg()));
    }

    @Test
    @DisplayName("crearEnvio: paquete supera la capacidad del vehiculo -> CapacidadExcedidaException")
    void crearEnvio_VehiculoSinCapacidad_LanzaExcepcion() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(2)).thenReturn(Optional.of(conductor));

        CapacidadExcedidaException ex = assertThrows(CapacidadExcedidaException.class,
                () -> envioService.crearEnvio(solicitud("750.00")));

        assertTrue(ex.getMessage().contains("CL-123456"));
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEnvio: codigo de rastreo repetido -> DuplicateResourceException")
    void crearEnvio_CodigoRastreoDuplicado_LanzaExcepcion() {
        when(envioRepository.existsByCodigoRastreo("EXP-1234")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> envioService.crearEnvio(solicitud("15.00")));

        verifyNoInteractions(vehiculoRepository, conductorRepository);
        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearEnvio: vehiculo inexistente -> ResourceNotFoundException")
    void crearEnvio_VehiculoInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> envioService.crearEnvio(solicitud("15.00")));

        verifyNoInteractions(conductorRepository);
    }

    @Test
    @DisplayName("crearEnvio: conductor inexistente -> ResourceNotFoundException")
    void crearEnvio_ConductorInexistente_LanzaExcepcion() {
        when(vehiculoRepository.findById(1)).thenReturn(Optional.of(vehiculo));
        when(conductorRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> envioService.crearEnvio(solicitud("15.00")));

        verify(envioRepository, never()).save(any());
    }

    // ------------------------------------------------------------- consultas

    @Test
    @DisplayName("obtenerPorId: envio existente -> retorna DTO")
    void obtenerPorId_EnvioExistente_RetornaDTO() {
        when(envioRepository.findByIdOptimizado(ENVIO_ID)).thenReturn(Optional.of(envioEnEstado("PENDIENTE")));

        EnvioResponseDTO resultado = envioService.obtenerPorId(ENVIO_ID);

        assertEquals("EXP-0010", resultado.codigoRastreo());
        assertEquals("Turrialba centro", resultado.direccionDestino());
    }

    @Test
    @DisplayName("obtenerPorId: envio inexistente -> ResourceNotFoundException")
    void obtenerPorId_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.findByIdOptimizado(99)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> envioService.obtenerPorId(99));

        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    @DisplayName("obtenerEnviosOptimizados: mapea todas las entidades a DTO")
    void obtenerEnviosOptimizados_RetornaListaDTO() {
        when(envioRepository.findAllOptimizado())
                .thenReturn(List.of(envioEnEstado("PENDIENTE"), envioEnEstado("ENTREGADO")));

        List<EnvioResponseDTO> resultado = envioService.obtenerEnviosOptimizados();

        assertEquals(2, resultado.size());
        assertEquals("ENTREGADO", resultado.get(1).estadoEnvio());
    }

    // ------------------------------------------------------- actualizarEstado

    @Test
    @DisplayName("actualizarEstado: PENDIENTE -> EN_TRANSITO registra la bitacora con el usuario autenticado")
    void actualizarEstado_TransicionValida_RegistraBitacora() {
        Usuario usuario = autenticarComo("conductor1");
        Envio envio = envioEnEstado("PENDIENTE");
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envio));
        when(envioRepository.save(envio)).thenReturn(envio);

        EnvioResponseDTO resultado = envioService.actualizarEstado(ENVIO_ID, " en_transito ", "Sale de bodega");

        assertEquals("EN_TRANSITO", resultado.estadoEnvio());

        ArgumentCaptor<BitacoraEnvio> captor = ArgumentCaptor.forClass(BitacoraEnvio.class);
        verify(bitacoraEnvioRepository).save(captor.capture());
        BitacoraEnvio bitacora = captor.getValue();
        assertAll(
                () -> assertEquals("PENDIENTE", bitacora.getEstadoAnterior()),
                () -> assertEquals("EN_TRANSITO", bitacora.getEstadoNuevo()),
                () -> assertEquals(usuario, bitacora.getUsuario()),
                () -> assertEquals("Sale de bodega", bitacora.getObservaciones()),
                () -> assertNotNull(bitacora.getFechaCambio()));
    }

    @Test
    @DisplayName("actualizarEstado: ENTREGADO no puede volver a EN_TRANSITO")
    void actualizarEstado_TransicionInvalida_LanzaExcepcion() {
        Envio envio = envioEnEstado("ENTREGADO");
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envio));

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> envioService.actualizarEstado(ENVIO_ID, "EN_TRANSITO", "Reintento"));

        assertTrue(ex.getMessage().contains("ENTREGADO -> EN_TRANSITO"));
        assertEquals("ENTREGADO", envio.getEstadoEnvio());
        verify(envioRepository, never()).save(any());
        verifyNoInteractions(bitacoraEnvioRepository);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} debe rechazarse")
    @CsvSource({
            "ENTREGADO,   EN_TRANSITO",
            "ENTREGADO,   PENDIENTE",
            "ENTREGADO,   CANCELADO",
            "CANCELADO,   PENDIENTE",
            "CANCELADO,   EN_TRANSITO",
            "EN_TRANSITO, PENDIENTE",
            "EN_TRANSITO, CANCELADO",
            "PENDIENTE,   ENTREGADO",
            "PENDIENTE,   PENDIENTE"
    })
    @DisplayName("actualizarEstado: matriz de transiciones no permitidas")
    void actualizarEstado_MatrizTransicionesInvalidas_LanzaExcepcion(String estadoActual, String estadoNuevo) {
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envioEnEstado(estadoActual)));

        assertThrows(InvalidStateTransitionException.class,
                () -> envioService.actualizarEstado(ENVIO_ID, estadoNuevo, null));

        verifyNoInteractions(bitacoraEnvioRepository);
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} debe permitirse")
    @CsvSource({
            "PENDIENTE,   EN_TRANSITO",
            "PENDIENTE,   CANCELADO",
            "EN_TRANSITO, ENTREGADO"
    })
    @DisplayName("actualizarEstado: matriz de transiciones permitidas")
    void actualizarEstado_MatrizTransicionesValidas_ActualizaEstado(String estadoActual, String estadoNuevo) {
        autenticarComo("admin");
        Envio envio = envioEnEstado(estadoActual);
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envio));
        when(envioRepository.save(envio)).thenReturn(envio);

        EnvioResponseDTO resultado = envioService.actualizarEstado(ENVIO_ID, estadoNuevo, null);

        assertEquals(estadoNuevo, resultado.estadoEnvio());
        verify(bitacoraEnvioRepository).save(any(BitacoraEnvio.class));
    }

    @ParameterizedTest(name = "[{index}] estado ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = { "PERDIDO", "EN_BODEGA" })
    @DisplayName("actualizarEstado: estado desconocido, nulo o vacio -> InvalidStateTransitionException")
    void actualizarEstado_EstadoDesconocido_LanzaExcepcion(String estadoNuevo) {
        assertThrows(InvalidStateTransitionException.class,
                () -> envioService.actualizarEstado(ENVIO_ID, estadoNuevo, null));

        verifyNoInteractions(envioRepository, bitacoraEnvioRepository);
    }

    @Test
    @DisplayName("actualizarEstado: envio inexistente -> ResourceNotFoundException")
    void actualizarEstado_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.actualizarEstado(99, "EN_TRANSITO", null));
    }

    @Test
    @DisplayName("actualizarEstado: sin usuario autenticado no se puede registrar la bitacora")
    void actualizarEstado_SinUsuarioAutenticado_LanzaExcepcion() {
        Envio envio = envioEnEstado("PENDIENTE");
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envio));

        assertThrows(InvalidStateTransitionException.class,
                () -> envioService.actualizarEstado(ENVIO_ID, "EN_TRANSITO", null));

        verifyNoInteractions(bitacoraEnvioRepository);
    }

    @Test
    @DisplayName("actualizarEstado: usuario autenticado que no existe en BD -> ResourceNotFoundException")
    void actualizarEstado_UsuarioNoRegistrado_LanzaExcepcion() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("fantasma", null, List.of()));
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envioEnEstado("PENDIENTE")));

        assertThrows(ResourceNotFoundException.class,
                () -> envioService.actualizarEstado(ENVIO_ID, "EN_TRANSITO", null));

        verifyNoInteractions(bitacoraEnvioRepository);
    }

    // ----------------------------------------------------------- cancelarEnvio

    @Test
    @DisplayName("cancelarEnvio: un envio EN_TRANSITO (en ruta) no se puede cancelar")
    void cancelarEnvio_EnvioEnTransito_LanzaExcepcion() {
        Envio envio = envioEnEstado("EN_TRANSITO");
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envio));

        InvalidStateTransitionException ex = assertThrows(InvalidStateTransitionException.class,
                () -> envioService.cancelarEnvio(ENVIO_ID, "Cliente desiste"));

        assertTrue(ex.getMessage().contains("en ruta"));
        assertEquals("EN_TRANSITO", envio.getEstadoEnvio());
        verify(envioRepository, never()).save(any());
        verifyNoInteractions(bitacoraEnvioRepository);
    }

    @Test
    @DisplayName("cancelarEnvio: un envio PENDIENTE pasa a CANCELADO y queda en bitacora")
    void cancelarEnvio_EnvioPendiente_CambiaACancelado() {
        autenticarComo("operador1");
        Envio envio = envioEnEstado("PENDIENTE");
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envio));
        when(envioRepository.save(envio)).thenReturn(envio);

        EnvioResponseDTO resultado = envioService.cancelarEnvio(ENVIO_ID, "Cliente desiste");

        assertEquals("CANCELADO", resultado.estadoEnvio());
        ArgumentCaptor<BitacoraEnvio> captor = ArgumentCaptor.forClass(BitacoraEnvio.class);
        verify(bitacoraEnvioRepository).save(captor.capture());
        assertEquals("Cliente desiste", captor.getValue().getObservaciones());
    }

    @ParameterizedTest(name = "[{index}] envio {0} no se puede cancelar")
    @ValueSource(strings = { "ENTREGADO", "CANCELADO" })
    @DisplayName("cancelarEnvio: un envio en estado final no se puede cancelar")
    void cancelarEnvio_EnvioEnEstadoFinal_LanzaExcepcion(String estadoFinal) {
        when(envioRepository.findById(ENVIO_ID)).thenReturn(Optional.of(envioEnEstado(estadoFinal)));

        assertThrows(InvalidStateTransitionException.class, () -> envioService.cancelarEnvio(ENVIO_ID, null));

        verify(envioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarEnvio: envio inexistente -> ResourceNotFoundException")
    void cancelarEnvio_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> envioService.cancelarEnvio(99, null));
    }

    // --------------------------------------------------------- obtenerBitacora

    @Test
    @DisplayName("obtenerBitacora: envio existente -> retorna historial mapeado")
    void obtenerBitacora_EnvioExistente_RetornaHistorial() {
        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        BitacoraEnvio registro = new BitacoraEnvio(1, envioEnEstado("EN_TRANSITO"), "PENDIENTE", "EN_TRANSITO",
                LocalDateTime.now(), usuario, "Salida");
        when(envioRepository.existsById(ENVIO_ID)).thenReturn(true);
        when(bitacoraEnvioRepository.findByEnvioId(ENVIO_ID)).thenReturn(List.of(registro));

        List<BitacoraResponseDTO> resultado = envioService.obtenerBitacora(ENVIO_ID);

        assertEquals(1, resultado.size());
        assertEquals("admin", resultado.get(0).usuario());
        assertEquals("EN_TRANSITO", resultado.get(0).estadoNuevo());
    }

    @Test
    @DisplayName("obtenerBitacora: envio inexistente -> ResourceNotFoundException")
    void obtenerBitacora_EnvioInexistente_LanzaExcepcion() {
        when(envioRepository.existsById(99)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> envioService.obtenerBitacora(99));

        verifyNoInteractions(bitacoraEnvioRepository);
    }
}
