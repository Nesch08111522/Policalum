package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.domain.model.Correlativo;
import com.policalum.erp.infrastructure.repository.CorrelativoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CorrelativoServiceImplTest {

    @Mock CorrelativoRepository repository;
    @InjectMocks CorrelativoServiceImpl service;

    @Test
    void siguiente_creaSiNoExisteBloqueaEIncrementa() {
        Correlativo c = Correlativo.builder().clave("DESP-2026").ultimo(41L).build();
        when(repository.findParaActualizar("DESP-2026")).thenReturn(Optional.of(c));

        assertEquals(42L, service.siguiente("DESP-2026"));
        assertEquals(42L, c.getUltimo());
        InOrder orden = inOrder(repository);
        orden.verify(repository).crearSiNoExiste("DESP-2026");
        orden.verify(repository).findParaActualizar("DESP-2026");
        orden.verify(repository).save(c);
    }

    @Test
    void siguiente_claveRecienCreada_empiezaEnUno() {
        when(repository.findParaActualizar("DESP-2027")).thenReturn(Optional.of(Correlativo.builder().clave("DESP-2027").ultimo(0L).build()));
        assertEquals(1L, service.siguiente("DESP-2027"));
    }

    @Test
    void siguiente_noSePudoLeer_lanzaIllegalState() {
        when(repository.findParaActualizar("X")).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.siguiente("X"));
    }
}
