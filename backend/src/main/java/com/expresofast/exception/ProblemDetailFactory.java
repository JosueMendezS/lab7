package com.expresofast.exception;

import java.net.URI;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Construye respuestas de error con el formato estandar RFC 7807
 * (application/problem+json): type, title, status, detail e instance.
 */
public final class ProblemDetailFactory {

    public static final String TIPO_BASE = "https://expresofast.com/errores/";

    private ProblemDetailFactory() {
    }

    public static ProblemDetail crear(HttpStatus status, String tipo, String titulo,
            String detalle, String instancia) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setType(URI.create(TIPO_BASE + tipo));
        problema.setTitle(titulo);
        if (instancia != null) {
            problema.setInstance(URI.create(instancia));
        }
        problema.setProperty("timestamp", LocalDateTime.now().toString());
        return problema;
    }
}
