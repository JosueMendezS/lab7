package com.expresofast.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.expresofast.config.SecurityConfig;
import com.expresofast.dto.BitacoraResponseDTO;
import com.expresofast.dto.EnvioRequestDTO;
import com.expresofast.dto.EnvioResponseDTO;
import com.expresofast.exception.CapacidadExcedidaException;
import com.expresofast.exception.InvalidStateTransitionException;
import com.expresofast.exception.ResourceNotFoundException;
import com.expresofast.security.JwtAuthenticationFilter;
import com.expresofast.security.JwtTokenProvider;
import com.expresofast.security.RestAccessDeniedHandler;
import com.expresofast.security.RestAuthenticationEntryPoint;
import com.expresofast.service.EnvioService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prueba de corte (slice) de la capa web de envios. Se carga la configuracion
 * de seguridad real (RBAC) pero el JWT se simula: el proveedor de tokens es un
 * mock y los usuarios se inyectan con {@link WithMockUser}.
 */
@WebMvcTest(controllers = EnvioController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class })
@DisplayName("EnvioController - pruebas de corte con MockMvc")
class EnvioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnvioService envioService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private EnvioResponseDTO envioDTO(String estado) {
        return new EnvioResponseDTO(10, "EXP-0010", "Turrialba centro", new BigDecimal("15.00"),
                new BigDecimal("7500.00"), estado, "CL-123456", "Jose Fernandez");
    }

    private EnvioRequestDTO solicitudValida() {
        return new EnvioRequestDTO("EXP-1234", "Paraiso, Cartago", new BigDecimal("15.00"),
                new BigDecimal("7500.00"), 1, 2);
    }

    // ------------------------------------------------------- GET /api/envios/{id}

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("GET /api/envios/{id} existente -> 200 OK con el JSON del envio")
    void obtenerPorId_EnvioExistente_Retorna200() throws Exception {
        when(envioService.obtenerPorId(10)).thenReturn(envioDTO("PENDIENTE"));

        mockMvc.perform(get("/api/envios/{id}", 10))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.codigoRastreo").value("EXP-0010"))
                .andExpect(jsonPath("$.direccionDestino").value("Turrialba centro"))
                .andExpect(jsonPath("$.pesoKg").value(15.00))
                .andExpect(jsonPath("$.estadoEnvio").value("PENDIENTE"))
                .andExpect(jsonPath("$.placaVehiculo").value("CL-123456"))
                .andExpect(jsonPath("$.nombreConductor").value("Jose Fernandez"));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("GET /api/envios/{id} inexistente -> 404 Not Found en formato RFC 7807")
    void obtenerPorId_EnvioInexistente_Retorna404ProblemDetail() throws Exception {
        when(envioService.obtenerPorId(99))
                .thenThrow(new ResourceNotFoundException("No existe el envio con ID 99"));

        mockMvc.perform(get("/api/envios/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type", endsWith("/recurso-no-encontrado")))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No existe el envio con ID 99"))
                .andExpect(jsonPath("$.instance").value("/api/envios/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/envios/{id} sin autenticacion -> 401 Unauthorized")
    void obtenerPorId_SinAutenticacion_Retorna401() throws Exception {
        mockMvc.perform(get("/api/envios/{id}", 10))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(envioService);
    }

    @Test
    @WithMockUser(username = "conductor1", roles = "CONDUCTOR")
    @DisplayName("GET /api/envios/optimizados -> 200 OK con la lista de envios")
    void obtenerEnviosOptimizados_Retorna200Lista() throws Exception {
        when(envioService.obtenerEnviosOptimizados())
                .thenReturn(List.of(envioDTO("PENDIENTE"), envioDTO("EN_TRANSITO")));

        mockMvc.perform(get("/api/envios/optimizados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].estadoEnvio").value("EN_TRANSITO"));
    }

    // ------------------------------------------------------------ POST /api/envios

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("POST /api/envios con payload valido -> 201 Created")
    void registrarEnvio_PayloadValido_Retorna201() throws Exception {
        when(envioService.crearEnvio(any(EnvioRequestDTO.class))).thenReturn(envioDTO("PENDIENTE"));

        mockMvc.perform(post("/api/envios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoRastreo").value("EXP-0010"))
                .andExpect(jsonPath("$.estadoEnvio").value("PENDIENTE"));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("POST /api/envios con campos nulos o vacios -> 400 Bad Request con la lista de errores")
    void registrarEnvio_PayloadInvalido_Retorna400() throws Exception {
        String payloadInvalido = """
                {
                  "codigoRastreo": "",
                  "direccionDestino": "   ",
                  "pesoKg": -5,
                  "costo": 0,
                  "vehiculoId": null,
                  "conductorId": null
                }
                """;

        mockMvc.perform(post("/api/envios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payloadInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Error de validacion"))
                .andExpect(jsonPath("$.instance").value("/api/envios"))
                .andExpect(jsonPath("$.errores", hasSize(greaterThanOrEqualTo(6))))
                .andExpect(jsonPath("$.errores[*].campo", hasItems(
                        "codigoRastreo", "direccionDestino", "pesoKg", "costo", "vehiculoId", "conductorId")))
                .andExpect(jsonPath("$.errores[*].mensaje", hasItems("El peso debe ser mayor a cero")));

        verifyNoInteractions(envioService);
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("POST /api/envios con JSON mal formado -> 400 Bad Request")
    void registrarEnvio_JsonMalFormado_Retorna400() throws Exception {
        mockMvc.perform(post("/api/envios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"codigoRastreo\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(envioService);
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("POST /api/envios que excede la capacidad del vehiculo -> 400 Bad Request")
    void registrarEnvio_CapacidadExcedida_Retorna400() throws Exception {
        when(envioService.crearEnvio(any(EnvioRequestDTO.class)))
                .thenThrow(new CapacidadExcedidaException("El peso supera la capacidad del vehiculo CL-123456"));

        mockMvc.perform(post("/api/envios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Capacidad del vehiculo excedida"))
                .andExpect(jsonPath("$.detail").value("El peso supera la capacidad del vehiculo CL-123456"));
    }

    @Test
    @WithMockUser(username = "conductor1", roles = "CONDUCTOR")
    @DisplayName("POST /api/envios con rol CONDUCTOR -> 403 Forbidden (RBAC)")
    void registrarEnvio_RolSinPermiso_Retorna403() throws Exception {
        mockMvc.perform(post("/api/envios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(solicitudValida())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(envioService);
    }

    // ----------------------------------------------------------- cambios de estado

    @Test
    @WithMockUser(username = "conductor1", roles = "CONDUCTOR")
    @DisplayName("PATCH /api/envios/{id}/estado valido -> 200 OK")
    void actualizarEstado_TransicionValida_Retorna200() throws Exception {
        when(envioService.actualizarEstado(10, "EN_TRANSITO", "Sale de bodega"))
                .thenReturn(envioDTO("EN_TRANSITO"));

        mockMvc.perform(patch("/api/envios/{id}/estado", 10)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevoEstado\":\"EN_TRANSITO\",\"observaciones\":\"Sale de bodega\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoEnvio").value("EN_TRANSITO"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("PATCH /api/envios/{id}/estado con transicion invalida -> 400 Bad Request")
    void actualizarEstado_TransicionInvalida_Retorna400() throws Exception {
        when(envioService.actualizarEstado(10, "EN_TRANSITO", null))
                .thenThrow(new InvalidStateTransitionException("Transicion de estado no permitida"));

        mockMvc.perform(patch("/api/envios/{id}/estado", 10)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nuevoEstado\":\"EN_TRANSITO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Transicion de estado no permitida"));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("PATCH /api/envios/{id}/cancelar de un envio en ruta -> 400 Bad Request")
    void cancelarEnvio_EnvioEnTransito_Retorna400() throws Exception {
        when(envioService.cancelarEnvio(10, null))
                .thenThrow(new InvalidStateTransitionException("El envio ya se encuentra en ruta"));

        mockMvc.perform(patch("/api/envios/{id}/cancelar", 10))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", endsWith("/transicion-invalida")));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("PATCH /api/envios/{id}/cancelar de un envio pendiente -> 200 OK")
    void cancelarEnvio_EnvioPendiente_Retorna200() throws Exception {
        when(envioService.cancelarEnvio(10, "Cliente desiste")).thenReturn(envioDTO("CANCELADO"));

        mockMvc.perform(patch("/api/envios/{id}/cancelar", 10).param("motivo", "Cliente desiste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoEnvio").value("CANCELADO"));
    }

    // ------------------------------------------------------- bitacora y tarifa

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    @DisplayName("GET /api/envios/{id}/bitacora -> 200 OK con el historial")
    void obtenerBitacora_Retorna200() throws Exception {
        when(envioService.obtenerBitacora(10)).thenReturn(List.of(new BitacoraResponseDTO(
                1, "PENDIENTE", "EN_TRANSITO", LocalDateTime.of(2026, 11, 9, 8, 30), "conductor1", "Salida")));

        mockMvc.perform(get("/api/envios/{id}/bitacora", 10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuario").value("conductor1"))
                .andExpect(jsonPath("$[0].estadoNuevo").value("EN_TRANSITO"));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("GET /api/envios/tarifa -> 200 OK con la tarifa calculada")
    void calcularTarifa_ParametrosValidos_Retorna200() throws Exception {
        when(envioService.calcularTarifa(5.0, 10.0)).thenReturn(2500.0);

        mockMvc.perform(get("/api/envios/tarifa").param("pesoKg", "5.0").param("distanciaKm", "10.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pesoKg").value(5.0))
                .andExpect(jsonPath("$.distanciaKm").value(10.0))
                .andExpect(jsonPath("$.tarifa").value(2500.0));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("GET /api/envios/tarifa con peso invalido -> 400 Bad Request")
    void calcularTarifa_PesoInvalido_Retorna400() throws Exception {
        when(envioService.calcularTarifa(0.0, 10.0))
                .thenThrow(new IllegalArgumentException("El peso debe ser mayor a cero."));

        mockMvc.perform(get("/api/envios/tarifa").param("pesoKg", "0").param("distanciaKm", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El peso debe ser mayor a cero."));
    }

    @Test
    @WithMockUser(username = "operador1", roles = "OPERADOR")
    @DisplayName("GET /api/envios/tarifa sin parametros -> 400 Bad Request")
    void calcularTarifa_SinParametros_Retorna400() throws Exception {
        mockMvc.perform(get("/api/envios/tarifa"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(envioService);
    }
}
