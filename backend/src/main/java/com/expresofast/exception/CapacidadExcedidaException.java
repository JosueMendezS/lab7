package com.expresofast.exception;

/**
 * Se lanza cuando el peso de un paquete supera la capacidad maxima de carga
 * del vehiculo al que se intenta asignar.
 */
public class CapacidadExcedidaException extends RuntimeException {

    public CapacidadExcedidaException(String message) {
        super(message);
    }
}
