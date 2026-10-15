package com.policalum.erp.infrastructure.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.service.PreformaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Módulo 18: el job de expiración nunca debe tumbar el hilo del scheduler. */
@ExtendWith(MockitoExtension.class)
class PreformaExpiracionSchedulerTest {

    @Mock PreformaService preformaService;
    @InjectMocks PreformaExpiracionScheduler scheduler;

    @Test
    void expirarPreformasVencidas_delegaEnElServicio() {
        when(preformaService.expirarVencidas()).thenReturn(2);

        scheduler.expirarPreformasVencidas();

        verify(preformaService).expirarVencidas();
    }

    @Test
    void expirarPreformasVencidas_errorDeBd_seTragaLaExcepcionParaReintentar() {
        when(preformaService.expirarVencidas()).thenThrow(new RuntimeException("conexión caída"));

        assertDoesNotThrow(() -> scheduler.expirarPreformasVencidas());
    }
}
