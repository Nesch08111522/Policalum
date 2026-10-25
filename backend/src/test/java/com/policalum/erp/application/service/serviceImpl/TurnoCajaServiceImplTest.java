package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AperturaCajaRequestDTO;
import com.policalum.erp.application.dto.request.CierreCajaRequestDTO;
import com.policalum.erp.application.dto.response.ResumenCierreCajaDTO;
import com.policalum.erp.application.mapper.TurnoCajaMapper;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Caja: apertura única por usuario/sede, cierre con FOR UPDATE y arqueo (esperado vs declarado). */
@ExtendWith(MockitoExtension.class)
class TurnoCajaServiceImplTest {

    @Mock TurnoCajaRepository turnoRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock PagoVentaRepository pagoRepository;
    @Mock MovimientoCajaChicaRepository cajaChicaRepository;
    @Mock TurnoCajaMapper mapper;
    @InjectMocks TurnoCajaServiceImpl service;

    private static final String EMAIL = "cajero@policalum.com";
    User cajero;
    Sede sede;

    @BeforeEach
    void setUp() {
        cajero = User.builder().id(UUID.randomUUID()).email(EMAIL).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenReturn(Optional.of(cajero));
        sede = Sede.builder().id(UUID.randomUUID()).build();
        lenient().when(sedeRepository.findById(sede.getId())).thenReturn(Optional.of(sede));
        lenient().when(turnoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TurnoCaja turnoAbierto(String inicial) {
        TurnoCaja t = TurnoCaja.builder().id(UUID.randomUUID()).user(cajero).sede(sede).estado(EstadoTurno.ABIERTO)
                .montoInicial(new BigDecimal(inicial)).build();
        lenient().when(turnoRepository.findByIdParaActualizar(t.getId())).thenReturn(Optional.of(t));
        return t;
    }

    // ---------------------------------------------------------------- abrir

    @Test
    void abrir_sinTurnoPrevio_creaTurnoAbiertoConMontoInicial() {
        when(turnoRepository.findByUser_IdAndSede_IdAndEstado(cajero.getId(), sede.getId(), EstadoTurno.ABIERTO)).thenReturn(Optional.empty());

        service.abrir(new AperturaCajaRequestDTO(sede.getId(), new BigDecimal("150.00"), "Sencillo"));

        ArgumentCaptor<TurnoCaja> c = ArgumentCaptor.forClass(TurnoCaja.class);
        verify(turnoRepository).save(c.capture());
        assertEquals(EstadoTurno.ABIERTO, c.getValue().getEstado());
        assertEquals(new BigDecimal("150.00"), c.getValue().getMontoInicial());
        assertSame(cajero, c.getValue().getUser());
        assertSame(sede, c.getValue().getSede());
        assertNotNull(c.getValue().getFechaApertura());
        assertEquals("Sencillo", c.getValue().getObservaciones());
        verify(mapper).toResponseDto(c.getValue());
    }

    @Test
    void abrir_yaTieneTurnoAbiertoEnLaSede_lanzaIllegalState() {
        when(turnoRepository.findByUser_IdAndSede_IdAndEstado(cajero.getId(), sede.getId(), EstadoTurno.ABIERTO))
                .thenReturn(Optional.of(TurnoCaja.builder().build()));

        assertThrows(IllegalStateException.class, () -> service.abrir(new AperturaCajaRequestDTO(sede.getId(), BigDecimal.ZERO, null)));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void abrir_sedeInexistente_lanzaNotFound() {
        UUID otra = UUID.randomUUID();
        when(sedeRepository.findById(otra)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.abrir(new AperturaCajaRequestDTO(otra, BigDecimal.ZERO, null)));
    }

    // ---------------------------------------------------------------- cerrar

    @Test
    void cerrar_cuadrado_calculaEsperadoYDiferenciaCero() {
        TurnoCaja t = turnoAbierto("100.00");
        t.setTotalRetirosSocios(new BigDecimal("50.00"));
        t.setTotalReembolsos(new BigDecimal("20.00"));
        when(pagoRepository.sumMontoByTurnoAndMetodo(t.getId(), MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("400.00"));

        ResumenCierreCajaDTO r = service.cerrar(t.getId(), new CierreCajaRequestDTO(new BigDecimal("430.00"), null));

        // 100 + 400 − 50 − 20 = 430
        assertEquals(new BigDecimal("430.00"), r.esperado());
        assertEquals(new BigDecimal("400.00"), r.ventasEfectivo());
        assertEquals(new BigDecimal("50.00"), r.retirosSocios());
        assertEquals(new BigDecimal("20.00"), r.reembolsosEfectivo());
        assertEquals(0, r.diferencia().signum());
        assertEquals(EstadoTurno.CERRADO, t.getEstado());
        assertNotNull(t.getFechaCierre());
        assertEquals(new BigDecimal("430.00"), t.getMontoFinalDeclarado());
        verify(turnoRepository).findByIdParaActualizar(t.getId());
        verify(turnoRepository, never()).findById(any());
        verify(turnoRepository).save(t);
    }

    @Test
    void cerrar_faltante_diferenciaNegativa() {
        TurnoCaja t = turnoAbierto("100.00");
        when(pagoRepository.sumMontoByTurnoAndMetodo(t.getId(), MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("200.00"));

        ResumenCierreCajaDTO r = service.cerrar(t.getId(), new CierreCajaRequestDTO(new BigDecimal("290.50"), null));

        assertEquals(new BigDecimal("-9.50"), r.diferencia());
    }

    @Test
    void cerrar_sinVentasEfectivoYCamposNulos_tomaCero() {
        TurnoCaja t = turnoAbierto("80.00");
        when(pagoRepository.sumMontoByTurnoAndMetodo(t.getId(), MetodoPago.EFECTIVO)).thenReturn(null);

        ResumenCierreCajaDTO r = service.cerrar(t.getId(), new CierreCajaRequestDTO(new BigDecimal("80.00"), null));

        assertEquals(BigDecimal.ZERO, r.ventasEfectivo());
        assertEquals(BigDecimal.ZERO, r.retirosSocios(), "turnos anteriores al Módulo 5 tienen el total en null");
        assertEquals(new BigDecimal("80.00"), r.esperado());
    }

    @Test
    void cerrar_retirosMayoresQueCaja_esperadoNegativoNoBloqueaElCierre() {
        TurnoCaja t = turnoAbierto("0.00");
        t.setTotalRetirosSocios(new BigDecimal("30.00"));
        when(pagoRepository.sumMontoByTurnoAndMetodo(t.getId(), MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("10.00"));

        ResumenCierreCajaDTO r = service.cerrar(t.getId(), new CierreCajaRequestDTO(BigDecimal.ZERO, null));

        assertEquals(new BigDecimal("-20.00"), r.esperado());
        assertEquals(EstadoTurno.CERRADO, t.getEstado());
    }

    @Test
    void cerrar_conObservacionesPrevias_agregaSinReemplazar() {
        TurnoCaja t = turnoAbierto("0.00");
        t.setObservaciones("[RETIRO] socio A S/ 50");
        when(pagoRepository.sumMontoByTurnoAndMetodo(any(), any())).thenReturn(BigDecimal.ZERO);

        service.cerrar(t.getId(), new CierreCajaRequestDTO(BigDecimal.ZERO, "todo ok"));

        assertEquals("[RETIRO] socio A S/ 50\n[CIERRE] todo ok", t.getObservaciones());
    }

    @Test
    void cerrar_sinObservacionesPrevias_guardaLasDelCierre() {
        TurnoCaja t = turnoAbierto("0.00");
        when(pagoRepository.sumMontoByTurnoAndMetodo(any(), any())).thenReturn(BigDecimal.ZERO);

        service.cerrar(t.getId(), new CierreCajaRequestDTO(BigDecimal.ZERO, "todo ok"));

        assertEquals("todo ok", t.getObservaciones());
    }

    @Test
    void cerrar_observacionEnBlanco_noTocaLasExistentes() {
        TurnoCaja t = turnoAbierto("0.00");
        t.setObservaciones("previa");
        when(pagoRepository.sumMontoByTurnoAndMetodo(any(), any())).thenReturn(BigDecimal.ZERO);

        service.cerrar(t.getId(), new CierreCajaRequestDTO(BigDecimal.ZERO, "   "));

        assertEquals("previa", t.getObservaciones());
    }

    @Test
    void cerrar_turnoYaCerrado_lanzaIllegalState() {
        TurnoCaja t = turnoAbierto("0.00");
        t.setEstado(EstadoTurno.CERRADO);

        assertThrows(IllegalStateException.class, () -> service.cerrar(t.getId(), new CierreCajaRequestDTO(BigDecimal.ZERO, null)));
        verify(turnoRepository, never()).save(any());
    }

    @Test
    void cerrar_turnoDeOtroUsuario_lanzaIllegalState() {
        TurnoCaja t = turnoAbierto("0.00");
        t.setUser(User.builder().id(UUID.randomUUID()).build());

        assertThrows(IllegalStateException.class, () -> service.cerrar(t.getId(), new CierreCajaRequestDTO(BigDecimal.ZERO, null)));
        assertEquals(EstadoTurno.ABIERTO, t.getEstado());
    }

    @Test
    void cerrar_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(turnoRepository.findByIdParaActualizar(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.cerrar(id, new CierreCajaRequestDTO(BigDecimal.ZERO, null)));
    }

    // ---------------------------------------------------------------- resumen

    @Test
    void resumen_turnoAbierto_sinDeclaradoNiDiferencia() {
        TurnoCaja t = TurnoCaja.builder().id(UUID.randomUUID()).montoInicial(new BigDecimal("50.00")).build();
        when(turnoRepository.findById(t.getId())).thenReturn(Optional.of(t));
        when(pagoRepository.sumMontoByTurnoAndMetodo(t.getId(), MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("25.00"));

        ResumenCierreCajaDTO r = service.resumen(t.getId());

        assertEquals(new BigDecimal("75.00"), r.esperado());
        assertNull(r.declarado());
        assertNull(r.diferencia());
    }

    @Test
    void resumen_turnoCerrado_usaElDeclaradoGuardado() {
        TurnoCaja t = TurnoCaja.builder().id(UUID.randomUUID()).montoInicial(new BigDecimal("50.00"))
                .montoFinalDeclarado(new BigDecimal("80.00")).estado(EstadoTurno.CERRADO).build();
        when(turnoRepository.findById(t.getId())).thenReturn(Optional.of(t));
        when(pagoRepository.sumMontoByTurnoAndMetodo(t.getId(), MetodoPago.EFECTIVO)).thenReturn(new BigDecimal("25.00"));

        assertEquals(new BigDecimal("5.00"), service.resumen(t.getId()).diferencia());
    }

    @Test
    void resumen_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(turnoRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resumen(id));
    }
}
