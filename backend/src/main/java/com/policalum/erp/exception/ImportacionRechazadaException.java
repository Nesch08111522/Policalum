package com.policalum.erp.exception;

import java.util.List;

/**
 * La importación no se confirma. La transacción que la envuelve hace rollback completo.
 */
public class ImportacionRechazadaException extends RuntimeException {

    private final List<ErrorFila> errores;

    public ImportacionRechazadaException(List<ErrorFila> errores) {
        super("La importación se revirtió por completo. Ninguna fila fue guardada.");
        this.errores = List.copyOf(errores);
    }

    public List<ErrorFila> getErrores() {
        return errores;
    }

    public record ErrorFila(int fila, String motivo) {
    }
}
