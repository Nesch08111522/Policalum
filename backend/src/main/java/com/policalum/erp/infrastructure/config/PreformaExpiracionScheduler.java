package com.policalum.erp.infrastructure.config;

import com.policalum.erp.application.service.PreformaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Marca como EXPIRADAS las preformas ABIERTAS vencidas cada 5 minutos (además de la
 * expiración al consultarlas), para que listados y dashboard siempre cuadren.
 * Intervalo configurable con policalum.preformas.expiracion-ms.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class PreformaExpiracionScheduler {

    private final PreformaService preformaService;

    @Scheduled(fixedDelayString = "${policalum.preformas.expiracion-ms:300000}",
               initialDelayString = "${policalum.preformas.expiracion-delay-ms:60000}")
    public void expirarPreformasVencidas() {
        try {
            preformaService.expirarVencidas();
        } catch (Exception e) {
            // Nunca tumbar el hilo del scheduler; se reintenta en la siguiente ejecución
            log.error("Error expirando preformas vencidas", e);
        }
    }
}
