package com.policalum.erp.application.service;

/** Numeración correlativa sin huecos por clave (ej. por tipo de documento y año). */
public interface CorrelativoService {

    /**
     * Reserva y devuelve el siguiente número de la clave (1, 2, 3...).
     * Debe llamarse dentro de una transacción: el número queda bloqueado hasta el commit,
     * y si la transacción falla no se consume.
     */
    long siguiente(String clave);
}
