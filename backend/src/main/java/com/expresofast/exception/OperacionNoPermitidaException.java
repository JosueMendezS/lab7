package com.expresofast.exception;

/**
 * Se lanza cuando una operacion entra en conflicto con el estado actual de un
 * recurso (por ejemplo, eliminar una empresa que aun tiene vehiculos).
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String message) {
        super(message);
    }
}
