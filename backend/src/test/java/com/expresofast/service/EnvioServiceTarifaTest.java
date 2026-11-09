package com.expresofast.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.expresofast.data.BitacoraEnvioRepository;
import com.expresofast.data.ConductorRepository;
import com.expresofast.data.EnvioRepository;
import com.expresofast.data.UsuarioRepository;
import com.expresofast.data.VehiculoRepository;

/**
 * Certificacion de la matriz de calculo de fletes (peso en kg y distancia en
 * km) mediante pruebas parametrizadas.
 *
 * Reglas: 500 por kg hasta 50 kg y 120 por kg (carga consolidada) por encima
 * de 50 kg; sin recargo hasta 50 km y 100 por cada km adicional.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EnvioService - calculo parametrizado de tarifas de envio")
class EnvioServiceTarifaTest {

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

    @ParameterizedTest
    @CsvSource({
            "5.0, 10.0, 2500.0",
            "15.0, 50.0, 7500.0",
            "100.0, 2.5, 12000.0"
    })
    @DisplayName("Debe calcular la tarifa correcta segun peso y distancia")
    void calcularTarifa_CasosVariados_CalculaCorrectamente(
            double pesoKg, double distanciaKm, double tarifaEsperada) {
        double tarifaCalculada = envioService.calcularTarifa(
                pesoKg, distanciaKm);
        assertEquals(tarifaEsperada, tarifaCalculada, 0.01);
    }

    @ParameterizedTest(name = "[{index}] {3}: peso={0} kg, distancia={1} km -> {2}")
    @CsvSource({
            "0.5,   0.0,   250.0,   paquete minimo sin recorrido",
            "50.0,  50.0,  25000.0, limite exacto de peso y distancia",
            "50.01, 0.0,   6001.2,  apenas sobre el limite de peso (consolidada)",
            "2.5,   50.5,  1300.0,  medio km sobre la zona sin recargo",
            "10.0,  80.0,  8000.0,  estandar con 30 km de recargo",
            "60.0,  100.0, 12200.0, consolidada con 50 km de recargo"
    })
    @DisplayName("Debe respetar los limites de peso y distancia de la matriz de fletes")
    void calcularTarifa_LimitesDeLaMatriz_CalculaCorrectamente(
            double pesoKg, double distanciaKm, double tarifaEsperada, String escenario) {
        assertEquals(tarifaEsperada, envioService.calcularTarifa(pesoKg, distanciaKm), 0.01, escenario);
    }

    @ParameterizedTest(name = "[{index}] peso={0} kg, distancia={1} km -> rechazado")
    @CsvSource({
            "0.0,    10.0, peso",
            "-5.0,   10.0, peso",
            "10.0,   -1.0, distancia",
            "-1.0,   -1.0, peso"
    })
    @DisplayName("Debe rechazar pesos no positivos y distancias negativas")
    void calcularTarifa_ParametrosInvalidos_LanzaExcepcion(
            double pesoKg, double distanciaKm, String campoInvalido) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> envioService.calcularTarifa(pesoKg, distanciaKm));

        assertTrue(ex.getMessage().toLowerCase().contains(campoInvalido));
    }
}
