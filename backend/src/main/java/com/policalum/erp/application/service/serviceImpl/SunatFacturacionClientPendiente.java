package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.service.SunatFacturacionClient;
import org.springframework.stereotype.Component;

/** Cliente de reserva hasta que se configure la API real de facturación electrónica. */
@Component
public class SunatFacturacionClientPendiente implements SunatFacturacionClient {

    static final String MENSAJE = "La API de SUNAT todavía no está conectada. El panel usa los comprobantes locales.";

    @Override
    public EnlaceSunat enlace() {
        return new EnlaceSunat("PENDIENTE", MENSAJE);
    }

    @Override
    public void emitir() {
        throw new IllegalStateException(MENSAJE);
    }

    @Override
    public void consultarValidez(String serie, String documentoCliente) {
        throw new IllegalStateException(MENSAJE);
    }
}
