package com.expresofast.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.expresofast.dto.AuthRequestDTO;
import com.expresofast.dto.AuthResponseDTO;
import com.expresofast.security.JwtTokenProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - pruebas unitarias con Mockito")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login: credenciales correctas -> retorna token JWT, usuario y roles")
    void login_CredencialesCorrectas_RetornaToken() {
        Authentication autenticado = new UsernamePasswordAuthenticationToken("admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(autenticado);
        when(jwtTokenProvider.generarToken(autenticado)).thenReturn("jwt.token.firmado");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponseDTO respuesta = authService.login(new AuthRequestDTO("admin", "Password123!"));

        assertAll(
                () -> assertEquals("jwt.token.firmado", respuesta.token()),
                () -> assertEquals("admin", respuesta.username()),
                () -> assertEquals(List.of("ROLE_ADMIN"), respuesta.roles()),
                () -> assertEquals(3_600_000L, respuesta.expirationTime()));
    }

    @Test
    @DisplayName("login: credenciales incorrectas -> BadCredentialsException y no se genera token")
    void login_CredencialesIncorrectas_LanzaExcepcion() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(new AuthRequestDTO("admin", "incorrecta")));

        assertEquals("Usuario o contraseña incorrectos", ex.getMessage());
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("login: usuario deshabilitado -> se responde igual que credenciales invalidas")
    void login_UsuarioDeshabilitado_LanzaBadCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new DisabledException("User is disabled"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new AuthRequestDTO("inactivo", "Password123!")));

        verifyNoInteractions(jwtTokenProvider);
    }
}
