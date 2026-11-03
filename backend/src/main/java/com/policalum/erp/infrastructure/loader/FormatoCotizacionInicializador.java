package com.policalum.erp.infrastructure.loader;

import com.policalum.erp.application.service.serviceImpl.FormatoCotizacionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Publica la utilidad en cero y la movilidad de los distritos de Trujillo si todavía no existen. */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class FormatoCotizacionInicializador implements ApplicationRunner {

    private final FormatoCotizacionService formato;

    @Override
    public void run(ApplicationArguments args) {
        try {
            formato.asegurar();
        } catch (RuntimeException e) {
            log.error("No se pudo publicar el formato de cotización", e);
        }
    }
}
