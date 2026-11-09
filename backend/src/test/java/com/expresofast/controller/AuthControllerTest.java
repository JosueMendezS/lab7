package com.expresofast.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.expresofast.config.SecurityConfig;
import com.expresofast.dto.AuthRequestDTO;
import com.expresofast.dto.AuthResponseDTO;
import com.expresofast.security.JwtAuthenticationFilter;
import com.expresofast.security.JwtTokenProvider;
import com.expresofast.security.RestAccessDeniedHandler;
import com.expresofast.security.RestAuthenticationEntryPoint;
import com.expresofast.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Prueba de corte del endpoint publico de autenticacion. /api/auth/** esta
 * permitido sin token, por lo que se valida el contrato HTTP del login.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class })
@DisplayName("AuthController - pruebas de corte con MockMvc")
class AuthControllerTest {

    private static final String TOKEN_JWT =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGVzIjpbIlJPTEVfQURNSU4iXX0.firma";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/login con credenciales correctas -> 200 OK con el token JWT")
    void login_CredencialesCorrectas_Retorna200ConToken() throws Exception {
        when(authService.login(any(AuthRequestDTO.class)))
                .thenReturn(new AuthResponseDTO(TOKEN_JWT, "admin", List.of("ROLE_ADMIN"), 3_600_000L));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequestDTO("admin", "Password123!"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").value(TOKEN_JWT))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.expirationTime").value(3_600_000));
    }

    @Test
    @DisplayName("POST /api/auth/login con credenciales incorrectas -> 401 Unauthorized")
    void login_CredencialesIncorrectas_Retorna401() throws Exception {
        when(authService.login(any(AuthRequestDTO.class)))
                .thenThrow(new BadCredentialsException("Usuario o contraseña incorrectos"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new AuthRequestDTO("admin", "incorrecta"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Usuario o contrasena incorrectos."))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/login con campos vacios -> 400 Bad Request")
    void login_CamposVacios_Retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasSize(2)))
                .andExpect(jsonPath("$.errores[*].campo", hasItems("username", "password")));

        verifyNoInteractions(authService);
    }
}
