package com.policalum.erp.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.policalum.erp.application.dto.request.DiaAtencionDTO;
import com.policalum.erp.application.dto.response.AtencionPublicaDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AtencionHorarioTest {

    @Test
    void diceSiHoyEstaAbiertoCerradoONoSeAtiende() {
        List<DiaAtencionDTO> dias = List.of(
                new DiaAtencionDTO("LUNES", true, "08:00", "18:00"),
                new DiaAtencionDTO("MARTES", true, "08:00", "18:00"),
                new DiaAtencionDTO("MIERCOLES", true, "08:00", "18:00"),
                new DiaAtencionDTO("JUEVES", true, "08:00", "18:00"),
                new DiaAtencionDTO("VIERNES", true, "08:00", "18:00"),
                new DiaAtencionDTO("SABADO", true, "08:00", "13:00"),
                new DiaAtencionDTO("DOMINGO", false, "", ""));

        AtencionPublicaDTO abierto = AtencionHorario.evaluar(dias, LocalDateTime.of(2026, 11, 3, 10, 0));
        assertTrue(abierto.abiertoAhora());
        assertTrue(abierto.texto().contains("Estamos abiertos"));

        AtencionPublicaDTO cerrado = AtencionHorario.evaluar(dias, LocalDateTime.of(2026, 11, 3, 18, 0));
        assertFalse(cerrado.abiertoAhora());
        assertTrue(cerrado.texto().contains("Ya cerramos"));

        AtencionPublicaDTO domingo = AtencionHorario.evaluar(dias, LocalDateTime.of(2026, 11, 8, 10, 0));
        assertFalse(domingo.atiendeHoy());
        assertTrue(domingo.texto().contains("no atendemos"));
    }
}
