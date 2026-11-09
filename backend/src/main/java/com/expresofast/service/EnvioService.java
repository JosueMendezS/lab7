package com.expresofast.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.expresofast.dto.EnvioMapper;
import com.expresofast.dto.EnvioRequestDTO;
import com.expresofast.dto.EnvioResponseDTO;
import com.expresofast.exception.CapacidadExcedidaException;
import com.expresofast.exception.DuplicateResourceException;
import com.expresofast.exception.InvalidStateTransitionException;
import com.expresofast.exception.ResourceNotFoundException;

@Service
public class EnvioService {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String EN_TRANSITO = "EN_TRANSITO";
    public static final String ENTREGADO = "ENTREGADO";
    public static final String CANCELADO = "CANCELADO";

    /**
     * Maquina de estados del envio. Un estado final (ENTREGADO o CANCELADO) no
     * admite transiciones, y un envio en ruta solo puede pasar a ENTREGADO.
     */
    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = Map.of(
            PENDIENTE, Set.of(EN_TRANSITO, CANCELADO),
            EN_TRANSITO, Set.of(ENTREGADO),
            ENTREGADO, Set.of(),
            CANCELADO, Set.of());

    // ---- Matriz de tarifas de flete ----
    /** Tarifa por kilogramo para paquetes estandar (hasta 50 kg). */
    public static final double TARIFA_POR_KG_ESTANDAR = 500.0;
    /** Tarifa por kilogramo para carga consolidada (mas de 50 kg). */
    public static final double TARIFA_POR_KG_CONSOLIDADA = 120.0;
    /** Peso limite de la tarifa estandar. */
    public static final double LIMITE_PESO_ESTANDAR_KG = 50.0;
    /** Distancia cubierta sin recargo (zona urbana / GAM). */
    public static final double DISTANCIA_SIN_RECARGO_KM = 50.0;
    /** Recargo por cada kilometro adicional fuera de la zona sin recargo. */
    public static final double RECARGO_POR_KM_ADICIONAL = 100.0;

    private final EnvioRepository envioRepository;
    private final VehiculoRepository vehiculoRepository;
    private final ConductorRepository conductorRepository;
    private final BitacoraEnvioRepository bitacoraEnvioRepository;
    private final UsuarioRepository usuarioRepository;

    public EnvioService(EnvioRepository envioRepository,
            VehiculoRepository vehiculoRepository,
            ConductorRepository conductorRepository,
            BitacoraEnvioRepository bitacoraEnvioRepository,
            UsuarioRepository usuarioRepository) {
        this.envioRepository = envioRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.conductorRepository = conductorRepository;
        this.bitacoraEnvioRepository = bitacoraEnvioRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EnvioResponseDTO> obtenerEnviosOptimizados() {
        return EnvioMapper.toResponseDTOList(envioRepository.findAllOptimizado());
    }

    @Transactional(readOnly = true)
    public EnvioResponseDTO obtenerPorId(Integer id) {
        Envio envio = envioRepository.findByIdOptimizado(id)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el envio con ID " + id));
        return EnvioMapper.toResponseDTO(envio);
    }

    @Transactional
    public EnvioResponseDTO crearEnvio(EnvioRequestDTO dto) {
        if (envioRepository.existsByCodigoRastreo(dto.codigoRastreo())) {
            throw new DuplicateResourceException(
                    "Ya existe un envio con el codigo de rastreo " + dto.codigoRastreo());
        }

        Vehiculo vehiculo = vehiculoRepository.findById(dto.vehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el vehiculo con ID " + dto.vehiculoId()));

        Conductor conductor = conductorRepository.findById(dto.conductorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el conductor con ID " + dto.conductorId()));

        if (dto.pesoKg().compareTo(vehiculo.getCapacidadKg()) > 0) {
            throw new CapacidadExcedidaException(
                    "El peso del envio (" + dto.pesoKg() + " kg) supera la capacidad maxima del vehiculo "
                            + vehiculo.getPlaca() + " (" + vehiculo.getCapacidadKg() + " kg).");
        }

        Envio envio = new Envio();
        envio.setCodigoRastreo(dto.codigoRastreo());
        envio.setDireccionDestino(dto.direccionDestino());
        envio.setPesoKg(dto.pesoKg());
        envio.setCosto(dto.costo());
        envio.setVehiculo(vehiculo);
        envio.setConductor(conductor);
        envio.setEstadoEnvio(PENDIENTE);

        return EnvioMapper.toResponseDTO(envioRepository.save(envio));
    }

    @Transactional
    public EnvioResponseDTO actualizarEstado(Integer envioId, String nuevoEstado, String observaciones) {
        String estadoNormalizado = nuevoEstado == null ? null : nuevoEstado.trim().toUpperCase();

        if (estadoNormalizado == null || !TRANSICIONES_PERMITIDAS.containsKey(estadoNormalizado)) {
            throw new InvalidStateTransitionException(
                    "Estado invalido: " + nuevoEstado + ". Valores permitidos: "
                            + TRANSICIONES_PERMITIDAS.keySet());
        }

        Envio envio = buscarEnvio(envioId);
        return EnvioMapper.toResponseDTO(aplicarCambioEstado(envio, estadoNormalizado, observaciones));
    }

    @Transactional
    public EnvioResponseDTO cancelarEnvio(Integer envioId, String motivo) {
        Envio envio = buscarEnvio(envioId);

        if (EN_TRANSITO.equals(envio.getEstadoEnvio())) {
            throw new InvalidStateTransitionException(
                    "No se puede cancelar el envio " + envio.getCodigoRastreo()
                            + " porque ya se encuentra en ruta (EN_TRANSITO).");
        }

        return EnvioMapper.toResponseDTO(aplicarCambioEstado(envio, CANCELADO, motivo));
    }

    @Transactional(readOnly = true)
    public List<BitacoraResponseDTO> obtenerBitacora(Integer envioId) {
        if (!envioRepository.existsById(envioId)) {
            throw new ResourceNotFoundException("No existe el envio con ID " + envioId);
        }
        return EnvioMapper.toBitacoraResponseDTOList(bitacoraEnvioRepository.findByEnvioId(envioId));
    }

    /**
     * Calcula la tarifa de flete (en colones) segun el peso y la distancia.
     * <ul>
     * <li>Cargo por peso: 500 por kg hasta 50 kg; 120 por kg (carga consolidada)
     * por encima de 50 kg.</li>
     * <li>Recargo por distancia: sin recargo hasta 50 km; 100 por cada km
     * adicional.</li>
     * </ul>
     */
    public double calcularTarifa(double pesoKg, double distanciaKm) {
        if (pesoKg <= 0) {
            throw new IllegalArgumentException("El peso debe ser mayor a cero.");
        }
        if (distanciaKm < 0) {
            throw new IllegalArgumentException("La distancia no puede ser negativa.");
        }

        double tarifaPorKg = pesoKg <= LIMITE_PESO_ESTANDAR_KG
                ? TARIFA_POR_KG_ESTANDAR
                : TARIFA_POR_KG_CONSOLIDADA;
        double cargoPorPeso = pesoKg * tarifaPorKg;

        double kmConRecargo = Math.max(0, distanciaKm - DISTANCIA_SIN_RECARGO_KM);
        double recargoDistancia = kmConRecargo * RECARGO_POR_KM_ADICIONAL;

        return Math.round((cargoPorPeso + recargoDistancia) * 100.0) / 100.0;
    }

    private Envio buscarEnvio(Integer envioId) {
        return envioRepository.findById(envioId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe el envio con ID " + envioId));
    }

    private Envio aplicarCambioEstado(Envio envio, String nuevoEstado, String observaciones) {
        String estadoAnterior = envio.getEstadoEnvio();

        if (!TRANSICIONES_PERMITIDAS.getOrDefault(estadoAnterior, Set.of()).contains(nuevoEstado)) {
            throw new InvalidStateTransitionException(
                    "Transicion de estado no permitida para el envio " + envio.getCodigoRastreo()
                            + ": " + estadoAnterior + " -> " + nuevoEstado);
        }

        envio.setEstadoEnvio(nuevoEstado);
        Envio actualizado = envioRepository.save(envio);

        BitacoraEnvio bitacora = new BitacoraEnvio();
        bitacora.setEnvio(actualizado);
        bitacora.setEstadoAnterior(estadoAnterior);
        bitacora.setEstadoNuevo(nuevoEstado);
        bitacora.setFechaCambio(LocalDateTime.now());
        bitacora.setUsuario(obtenerUsuarioAutenticado());
        bitacora.setObservaciones(observaciones);
        bitacoraEnvioRepository.save(bitacora);

        return actualizado;
    }

    private Usuario obtenerUsuarioAutenticado() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            throw new InvalidStateTransitionException(
                    "No hay un usuario autenticado para registrar el cambio de estado.");
        }
        String username = autenticacion.getName();
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }
}
