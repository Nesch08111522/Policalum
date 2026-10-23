package com.policalum.erp.application.service;

/**
 * Punto de extensión de la API de SUNAT.
 * La implementación actual no llama a ningún servicio externo.
 * Cuando llegue la API, se reemplaza este componente sin cambiar el panel.
 */
public interface SunatFacturacionClient {

    EnlaceSunat enlace();

    /** Emisión electrónica. La implementación pendiente explica que la API aún no está conectada. */
    void emitir();

    /** Consulta de validez de un comprobante. La implementación pendiente no contacta a SUNAT. */
    void consultarValidez(String serie, String documentoCliente);

    record EnlaceSunat(String codigo, String mensaje) {}
}
