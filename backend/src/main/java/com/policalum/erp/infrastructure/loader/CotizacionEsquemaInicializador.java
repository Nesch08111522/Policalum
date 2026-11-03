package com.policalum.erp.infrastructure.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Hibernate no reescribe un CHECK ya creado. El de estado se quedó sin PENDIENTE ni NO_COMPRADO
 * cuando el enum creció, y el portal no podía guardar la cotización.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class CotizacionEsquemaInicializador implements ApplicationRunner {

    private static final String ESTADOS = "'PENDIENTE','EMITIDA','ACEPTADA','RECHAZADA','NO_COMPRADO','VENCIDA','CONVERTIDA'";
    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        try {
            String definicion = jdbc.query("""
                    SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'cotizaciones_estado_check'
                    """, (rs, row) -> rs.getString(1)).stream().findFirst().orElse("");
            if (definicion.contains("PENDIENTE") && definicion.contains("NO_COMPRADO")) return;
            jdbc.execute("ALTER TABLE cotizaciones DROP CONSTRAINT IF EXISTS cotizaciones_estado_check");
            jdbc.execute("ALTER TABLE cotizaciones ADD CONSTRAINT cotizaciones_estado_check CHECK (estado IN (" + ESTADOS + "))");
            log.info("El estado de cotización ya acepta PENDIENTE y NO_COMPRADO");
        } catch (RuntimeException e) {
            log.error("No se pudo actualizar la restricción de estado de cotizaciones", e);
        }
    }
}
