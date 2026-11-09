package com.expresofast.dto;
 
import java.util.List;

import com.expresofast.domain.BitacoraEnvio;
import com.expresofast.domain.Envio;
 
/**
 * Traduce entre las entidades JPA del dominio (Envio, BitacoraEnvio) y los
 * DTOs planos que se exponen al cliente, evitando ciclos de serializacion
 * JSON y desacoplando totalmente la capa de presentacion del dominio.
 */
public final class EnvioMapper {
 
    private EnvioMapper() {
    }
 
    public static EnvioResponseDTO toResponseDTO(Envio envio) {
        String placaVehiculo = envio.getVehiculo() != null ? envio.getVehiculo().getPlaca() : null;
        String nombreConductor = envio.getConductor() != null
                ? envio.getConductor().getNombre() + " " + envio.getConductor().getApellidos()
                : null;
 
        return new EnvioResponseDTO(
                envio.getId(),
                envio.getCodigoRastreo(),
                envio.getDireccionDestino(),
                envio.getPesoKg(),
                envio.getCosto(),
                envio.getEstadoEnvio(),
                placaVehiculo,
                nombreConductor);
    }
 
    public static List<EnvioResponseDTO> toResponseDTOList(List<Envio> envios) {
        return envios.stream().map(EnvioMapper::toResponseDTO).toList();
    }
 
    public static BitacoraResponseDTO toBitacoraResponseDTO(BitacoraEnvio bitacora) {
        String nombreUsuario = bitacora.getUsuario() != null
                ? bitacora.getUsuario().getUsername()
                : null;
 
        return new BitacoraResponseDTO(
                bitacora.getId(),
                bitacora.getEstadoAnterior(),
                bitacora.getEstadoNuevo(),
                bitacora.getFechaCambio(),
                nombreUsuario,
                bitacora.getObservaciones());
    }
 
    public static List<BitacoraResponseDTO> toBitacoraResponseDTOList(List<BitacoraEnvio> bitacoras) {
        return bitacoras.stream().map(EnvioMapper::toBitacoraResponseDTO).toList();
    }
}
