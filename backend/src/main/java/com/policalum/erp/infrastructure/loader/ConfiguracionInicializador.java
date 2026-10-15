package com.policalum.erp.infrastructure.loader;

import com.policalum.erp.application.service.ConfiguracionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Crea al iniciar las filas de configuración que falten (Módulo 14), con los valores de fábrica.
 * Un fallo aquí no detiene la aplicación: los servicios usan los valores de fábrica si la fila no existe.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfiguracionInicializador implements ApplicationRunner {

    private final ConfiguracionService configuracionService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            configuracionService.inicializarSiFalta();
        } catch (RuntimeException e) {
            log.error("No se pudo inicializar la configuración global; se usarán los valores de fábrica", e);
        }
    }
}
