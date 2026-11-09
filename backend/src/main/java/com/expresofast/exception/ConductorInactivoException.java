package com.expresofast.exception;

/**
 * Se lanza al intentar asignar a un vehiculo un conductor que esta inactivo.
 */
public class ConductorInactivoException extends OperacionNoPermitidaException {

    public ConductorInactivoException(String message) {
        super(message);
    }
}
