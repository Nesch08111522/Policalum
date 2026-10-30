package com.policalum.erp.application.service.serviceImpl;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Tope suave por IP para los POST públicos. No sustituye un límite en el proxy. */
@Component
public class LimitePublico {

    private static final long VENTANA_MS = 60_000L;
    private static final int MAXIMO = 12;
    private final ConcurrentHashMap<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public boolean permitir(String clave) {
        long ahora = System.currentTimeMillis();
        Ventana ventana = ventanas.compute(clave, (k, actual) -> {
            if (actual == null || ahora - actual.inicio > VENTANA_MS) {
                return new Ventana(ahora, 1);
            }
            actual.conteo++;
            return actual;
        });
        return ventana.conteo <= MAXIMO;
    }

    private static final class Ventana {
        private final long inicio;
        private int conteo;

        private Ventana(long inicio, int conteo) {
            this.inicio = inicio;
            this.conteo = conteo;
        }
    }
}
