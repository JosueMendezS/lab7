package com.expresofast.exception;

/**
 * Se lanza cuando se intenta registrar un recurso cuyo identificador natural
 * (placa, codigo de rastreo, cedula juridica, etc.) ya existe.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
