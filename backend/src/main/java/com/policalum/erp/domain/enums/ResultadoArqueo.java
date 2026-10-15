package com.policalum.erp.domain.enums;

/** Resultado del arqueo de un turno de caja (Módulo 12). */
public enum ResultadoArqueo {
    /** Turno aún abierto: el esperado es a la fecha. */
    ABIERTO,
    CUADRADO,
    FALTANTE,
    SOBRANTE,
    /** Turno cerrado sin monto declarado (registros antiguos). */
    SIN_DECLARAR
}
