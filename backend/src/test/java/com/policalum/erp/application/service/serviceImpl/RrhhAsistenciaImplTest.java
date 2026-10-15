package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AsistenciaQrDTO;
import com.policalum.erp.application.dto.request.RrhhAsistenciaRequestDTO;
import com.policalum.erp.application.dto.response.AsistenciaEstadoDTO;
import com.policalum.erp.application.dto.response.DashboardAsistenciaDTO;
import com.policalum.erp.application.dto.response.RrhhAsistenciaResponseDTO;
import com.policalum.erp.application.dto.response.TurnoDelDiaDTO;
import com.policalum.erp.application.mapper.RrhhAsistenciaMapper;
import com.policalum.erp.application.service.TotpService;
import com.policalum.erp.application.service.TurnoTrabajoService;
import com.policalum.erp.domain.enums.EstadoAsistencia;
import com.policalum.erp.domain.enums.TipoMarcacion;
import com.policalum.erp.domain.model.RrhhAsistencia;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhAsistenciaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

/** Módulo 8: marcación QR/manual, tardanza (por turno o regla 08:00 + 10 min) y horas trabajadas. */
@ExtendWith(MockitoExtension.class)
class RrhhAsistenciaImplTest {

    @Mock RrhhAsistenciaRepository asistenciaRepository;
    @Mock UserRepository userRepository;
    @Mock TotpService totpService;
    @Mock RrhhAsistenciaMapper asistenciaMapper;
    @Mock TurnoTrabajoService turnoTrabajoService;
    @InjectMocks RrhhAsistenciaImpl service;

    private static final String SECRETO = "SECRETO-PUBLICO-TEST";
    /** Día pasado fijo para las marcaciones manuales: el resultado no depende de la hora en que corre el test. */
    private static final LocalDate AYER = LocalDate.now().minusDays(1);

    User empleado;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "secretKeyPublico", SECRETO);
        empleado = User.builder().id(UUID.randomUUID()).email("ana@policalum.com").activo(true).build();
        lenient().when(userRepository.findById(empleado.getId())).thenReturn(Optional.of(empleado));
        lenient().when(asistenciaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(turnoTrabajoService.turnoDelDia(any(), any())).thenReturn(Optional.empty());
    }

    private RrhhAsistencia guardada() {
        ArgumentCaptor<RrhhAsistencia> c = ArgumentCaptor.forClass(RrhhAsistencia.class);
        verify(asistenciaRepository).save(c.capture());
        return c.getValue();
    }

    private RrhhAsistenciaRequestDTO manual(LocalDateTime fechaHora, TipoMarcacion tipo) {
        return new RrhhAsistenciaRequestDTO(empleado.getId(), fechaHora, tipo, null, null);
    }

    private void marcasPrevias(boolean entrada, boolean salida) {
        lenient().when(asistenciaRepository.yaMarcoEntradaHoy(eq(empleado.getId()), any(), any())).thenReturn(entrada);
        lenient().when(asistenciaRepository.yaMarcoSalidaHoy(eq(empleado.getId()), any(), any())).thenReturn(salida);
    }

    private AsistenciaQrDTO qrPublico(String codigo) {
        return new AsistenciaQrDTO("POLICALUM|PUBLICO|123456|" + codigo, empleado.getId(), 30L);
    }

    // ================================================================ manual: estado (regla sin turno)

    @Test
    void manual_entradaDentroDeTolerancia_presente() {
        marcasPrevias(false, false);

        service.procesarMarcacionManual(manual(AYER.atTime(8, 10), TipoMarcacion.ENTRADA));

        RrhhAsistencia a = guardada();
        assertEquals(EstadoAsistencia.PRESENTE, a.getEstado(), "08:10 exacto todavía está en tolerancia");
        assertEquals(LocalTime.of(8, 10), a.getHoraEntrada());
        assertEquals(AYER, a.getFecha());
        assertNull(a.getHoraSalida());
        assertEquals("Registro Manual por Admin", a.getDispositivoInfo());
    }

    @Test
    void manual_entradaUnMinutoDespuesDeTolerancia_tardanza() {
        marcasPrevias(false, false);

        service.procesarMarcacionManual(manual(AYER.atTime(8, 10, 1), TipoMarcacion.ENTRADA));

        assertEquals(EstadoAsistencia.TARDANZA, guardada().getEstado());
    }

    @Test
    void manual_entradaConTurno_usaHoraYToleranciaDelTurno() {
        marcasPrevias(false, false);
        UUID turnoId = UUID.randomUUID();
        when(turnoTrabajoService.turnoDelDia(empleado.getId(), AYER.atTime(14, 20)))
                .thenReturn(Optional.of(new TurnoDelDiaDTO(turnoId, "Tarde", LocalTime.of(14, 0), 15)));

        service.procesarMarcacionManual(manual(AYER.atTime(14, 20), TipoMarcacion.ENTRADA));

        RrhhAsistencia a = guardada();
        assertEquals(EstadoAsistencia.TARDANZA, a.getEstado(), "20 min > 15 de tolerancia del turno");
        assertEquals(turnoId, a.getTurnoTrabajoId());
    }

    @Test
    void manual_entradaConTurnoEnTolerancia_presenteAunqueSeaPasadas8() {
        marcasPrevias(false, false);
        when(turnoTrabajoService.turnoDelDia(any(), any()))
                .thenReturn(Optional.of(new TurnoDelDiaDTO(UUID.randomUUID(), "Tarde", LocalTime.of(14, 0), 15)));

        service.procesarMarcacionManual(manual(AYER.atTime(14, 15), TipoMarcacion.ENTRADA));

        assertEquals(EstadoAsistencia.PRESENTE, guardada().getEstado());
    }

    @Test
    void manual_salida_calculaHorasTrabajadasDesdeLaEntradaDelDia() {
        marcasPrevias(true, false);
        RrhhAsistencia entrada = new RrhhAsistencia();
        entrada.setHoraEntrada(LocalTime.of(8, 0));
        when(asistenciaRepository.findUltimaEntradaDelDia(eq(empleado.getId()), eq(AYER.atStartOfDay()), eq(AYER.plusDays(1).atStartOfDay())))
                .thenReturn(entrada);

        service.procesarMarcacionManual(manual(AYER.atTime(17, 30), TipoMarcacion.SALIDA));

        RrhhAsistencia a = guardada();
        assertEquals(new BigDecimal("9.50"), a.getHorasTrabajadas());
        assertEquals(LocalTime.of(8, 0), a.getHoraEntrada());
        assertEquals(LocalTime.of(17, 30), a.getHoraSalida());
        assertEquals(EstadoAsistencia.PRESENTE, a.getEstado(), "la salida nunca es tardanza");
        verify(turnoTrabajoService, never()).turnoDelDia(any(), any());
    }

    @Test
    void manual_salidaSinEntradaRecuperable_noCalculaHoras() {
        marcasPrevias(true, false);
        when(asistenciaRepository.findUltimaEntradaDelDia(any(), any(), any())).thenReturn(null);

        service.procesarMarcacionManual(manual(AYER.atTime(17, 0), TipoMarcacion.SALIDA));

        assertNull(guardada().getHorasTrabajadas());
    }

    @Test
    void manual_dispositivoInformado_seRespeta() {
        marcasPrevias(false, false);

        service.procesarMarcacionManual(new RrhhAsistenciaRequestDTO(empleado.getId(), AYER.atTime(8, 0), TipoMarcacion.ENTRADA, "Tablet RRHH", null));

        assertEquals("Tablet RRHH", guardada().getDispositivoInfo());
    }

    // ================================================================ manual: validaciones

    @Test
    void manual_fechaFutura_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.procesarMarcacionManual(
                manual(LocalDateTime.now().plusHours(1), TipoMarcacion.ENTRADA)));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void manual_entradaDuplicada_lanzaIllegalState() {
        marcasPrevias(true, false);

        assertThrows(IllegalStateException.class, () -> service.procesarMarcacionManual(manual(AYER.atTime(9, 0), TipoMarcacion.ENTRADA)));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void manual_salidaSinEntrada_lanzaIllegalState() {
        marcasPrevias(false, false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.procesarMarcacionManual(manual(AYER.atTime(17, 0), TipoMarcacion.SALIDA)));
        assertTrue(ex.getMessage().contains("sin haber marcado ENTRADA"));
    }

    @Test
    void manual_salidaDuplicada_lanzaIllegalState() {
        marcasPrevias(true, true);

        assertThrows(IllegalStateException.class, () -> service.procesarMarcacionManual(manual(AYER.atTime(18, 0), TipoMarcacion.SALIDA)));
    }

    @Test
    void manual_validaContraElDiaDeLaMarcacionNoContraHoy() {
        marcasPrevias(false, false);

        service.procesarMarcacionManual(manual(AYER.atTime(8, 0), TipoMarcacion.ENTRADA));

        verify(asistenciaRepository).yaMarcoEntradaHoy(empleado.getId(), AYER.atStartOfDay(), AYER.plusDays(1).atStartOfDay());
    }

    @Test
    void manual_usuarioInactivo_lanzaIllegalState() {
        empleado.setActivo(false);

        assertThrows(IllegalStateException.class, () -> service.procesarMarcacionManual(manual(AYER.atTime(8, 0), TipoMarcacion.ENTRADA)));
    }

    @Test
    void manual_usuarioInexistente_lanzaNotFound() {
        UUID otro = UUID.randomUUID();
        when(userRepository.findById(otro)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.procesarMarcacionManual(
                new RrhhAsistenciaRequestDTO(otro, AYER.atTime(8, 0), TipoMarcacion.ENTRADA, null, null)));
    }

    // ================================================================ QR público

    @Test
    void qrPublico_valido_primeraMarcaDelDiaEsEntradaYValidaConSecretoPublico() {
        marcasPrevias(false, false);
        when(totpService.verify(SECRETO, 123456L, "999111")).thenReturn(true);
        // turno que empezó hace 2 horas: tardanza sin importar la hora real de ejecución
        when(turnoTrabajoService.turnoDelDia(eq(empleado.getId()), any()))
                .thenReturn(Optional.of(new TurnoDelDiaDTO(UUID.randomUUID(), "X", LocalTime.now().minusHours(2), 10)));

        service.procesarMarcacionQr(qrPublico("999111"));

        RrhhAsistencia a = guardada();
        assertEquals(TipoMarcacion.ENTRADA, a.getTipoMarcacion());
        assertEquals(EstadoAsistencia.TARDANZA, a.getEstado());
        assertEquals("Escaneo QR Móvil", a.getDispositivoInfo());
        assertSame(empleado, a.getEmpleado());
    }

    @Test
    void qrPublico_conEntradaPrevia_registraSalida() {
        marcasPrevias(true, false);
        when(totpService.verify(any(), anyLong(), any())).thenReturn(true);

        service.procesarMarcacionQr(qrPublico("1"));

        assertEquals(TipoMarcacion.SALIDA, guardada().getTipoMarcacion());
    }

    /**
     * Comportamiento actual: tras ENTRADA y SALIDA el servicio intenta una "nueva entrada",
     * pero validarMarcacion la rechaza porque ya hay ENTRADA hoy. El log de determinarTipoMarcacion
     * ("Nueva entrada") nunca se cumple. Ver hallazgos de la auditoría.
     */
    @Test
    void qrPublico_terceraMarcaDelDia_esRechazada() {
        marcasPrevias(true, true);
        when(totpService.verify(any(), anyLong(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.procesarMarcacionQr(qrPublico("1")));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void qrPublico_codigoInvalido_lanzaIllegalArgumentSinBuscarUsuario() {
        when(totpService.verify(SECRETO, 123456L, "000000")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.procesarMarcacionQr(qrPublico("000000")));
        verifyNoInteractions(userRepository);
    }

    @Test
    void qrPublico_formatoIncompleto_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.procesarMarcacionQr(
                new AsistenciaQrDTO("POLICALUM|PUBLICO|123", empleado.getId(), null)));
        verifyNoInteractions(totpService);
    }

    @Test
    void qrPublico_ventanaNoNumerica_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.procesarMarcacionQr(
                new AsistenciaQrDTO("POLICALUM|PUBLICO|abc|123", empleado.getId(), null)));
    }

    @Test
    void qrPublico_sinEmpleado_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.procesarMarcacionQr(
                new AsistenciaQrDTO("POLICALUM|PUBLICO|1|2", null, null)));
    }

    @Test
    void qrPublico_usuarioInactivo_lanzaIllegalState() {
        empleado.setActivo(false);
        when(totpService.verify(any(), anyLong(), any())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.procesarMarcacionQr(qrPublico("1")));
        verify(asistenciaRepository, never()).save(any());
    }

    // ================================================================ QR token personal (formato antiguo)

    @Test
    void qrToken_valido_registraEntrada() {
        marcasPrevias(false, false);
        when(totpService.validarTokenYObtenerEmpleadoId("dG9rZW4=")).thenReturn(empleado.getId());

        service.procesarMarcacionQr(new AsistenciaQrDTO("dG9rZW4=", empleado.getId(), null));

        assertEquals(TipoMarcacion.ENTRADA, guardada().getTipoMarcacion());
    }

    @Test
    void qrToken_deOtroEmpleado_lanzaSecurityException() {
        when(totpService.validarTokenYObtenerEmpleadoId("x")).thenReturn(UUID.randomUUID());

        assertThrows(SecurityException.class, () -> service.procesarMarcacionQr(new AsistenciaQrDTO("x", empleado.getId(), null)));
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void qrToken_expirado_lanzaIllegalArgument() {
        when(totpService.validarTokenYObtenerEmpleadoId("x")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.procesarMarcacionQr(new AsistenciaQrDTO("x", empleado.getId(), null)));
    }

    // ================================================================ CRUD y consultas

    @Test
    void actualizarMarcacionManual_aplicaCambiosYMarcaComoModificada() {
        UUID id = UUID.randomUUID();
        RrhhAsistencia existente = new RrhhAsistencia();
        RrhhAsistenciaRequestDTO r = manual(AYER.atTime(8, 30), TipoMarcacion.ENTRADA);
        when(asistenciaRepository.findById(id)).thenReturn(Optional.of(existente));

        service.actualizarMarcacionManual(id, r);

        verify(asistenciaMapper).updateEntityFromDto(r, existente);
        assertEquals("Modificado por Admin (Manual)", existente.getDispositivoInfo());
        verify(asistenciaRepository).save(existente);
    }

    @Test
    void actualizarMarcacionManual_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(asistenciaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarMarcacionManual(id, manual(AYER.atTime(8, 0), TipoMarcacion.ENTRADA)));
    }

    @Test
    void eliminarMarcacion_existente_borra() {
        UUID id = UUID.randomUUID();
        when(asistenciaRepository.existsById(id)).thenReturn(true);

        service.eliminarMarcacion(id);

        verify(asistenciaRepository).deleteById(id);
    }

    @Test
    void eliminarMarcacion_inexistente_lanzaNotFoundSinBorrar() {
        UUID id = UUID.randomUUID();
        when(asistenciaRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.eliminarMarcacion(id));
        verify(asistenciaRepository, never()).deleteById(any());
    }

    @Test
    void buscarPorId_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(asistenciaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(id));
    }

    @Test
    void listarAsistencias_eligeLaConsultaSegunLosFiltros() {
        Pageable p = PageRequest.of(0, 10);
        LocalDateTime ini = AYER.atStartOfDay(), fin = AYER.plusDays(1).atStartOfDay();
        UUID emp = empleado.getId();
        when(asistenciaRepository.findByEmpleadoIdAndFechaHoraBetween(emp, ini, fin, p)).thenReturn(Page.empty());
        when(asistenciaRepository.findByEmpleadoId(emp, p)).thenReturn(Page.empty());
        when(asistenciaRepository.findByFechaHoraBetween(ini, fin, p)).thenReturn(Page.empty());
        when(asistenciaRepository.findAllConEmpleados(p)).thenReturn(Page.empty());

        service.listarAsistencias(emp, ini, fin, p);
        service.listarAsistencias(emp, ini, null, p);   // rango incompleto: solo por empleado
        service.listarAsistencias(null, ini, fin, p);
        service.listarAsistencias(null, null, fin, p);  // rango incompleto y sin empleado: todas

        verify(asistenciaRepository).findByEmpleadoIdAndFechaHoraBetween(emp, ini, fin, p);
        verify(asistenciaRepository).findByEmpleadoId(emp, p);
        verify(asistenciaRepository).findByFechaHoraBetween(ini, fin, p);
        verify(asistenciaRepository).findAllConEmpleados(p);
    }

    @Test
    void misAsistencias_sinEmpleado_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.misAsistencias(null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    void misAsistencias_empleadoInexistente_lanzaNotFound() {
        UUID otro = UUID.randomUUID();
        when(userRepository.findById(otro)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.misAsistencias(otro, null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(asistenciaRepository);
    }

    @Test
    void misAsistencias_conRango_filtraPorFechas() {
        Pageable p = PageRequest.of(0, 10);
        LocalDateTime ini = AYER.atStartOfDay(), fin = AYER.plusDays(1).atStartOfDay();
        when(asistenciaRepository.findByEmpleadoIdAndFechaHoraBetween(empleado.getId(), ini, fin, p)).thenReturn(Page.empty());

        service.misAsistencias(empleado.getId(), ini, fin, p);

        verify(asistenciaRepository, never()).findByEmpleadoId(any(), any());
    }

    @Test
    void obtenerDashboard_global_ausentesSonActivosMenosPresentes() {
        when(asistenciaRepository.countEmpleadosActivos()).thenReturn(12L);
        when(asistenciaRepository.countPresentesHoy(any(), any())).thenReturn(9L);
        when(asistenciaRepository.countTardanzasHoy(any(), any())).thenReturn(2L);
        when(asistenciaRepository.findUltimasAsistencias()).thenReturn(List.of(new RrhhAsistencia(), new RrhhAsistencia()));

        DashboardAsistenciaDTO d = service.obtenerDashboard(null);

        assertEquals(12L, d.totalEmpleados());
        assertEquals(9L, d.presentesHoy());
        assertEquals(3L, d.ausentesHoy());
        assertEquals(2L, d.tardanzasHoy());
        assertEquals(2, d.asistenciasRecientes().size());
    }

    @Test
    void obtenerDashboard_porEmpleado_traeSusUltimas5() {
        when(asistenciaRepository.findByEmpleadoId(eq(empleado.getId()), any())).thenReturn(Page.empty());

        service.obtenerDashboard(empleado.getId());

        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(asistenciaRepository).findByEmpleadoId(eq(empleado.getId()), c.capture());
        assertEquals(5, c.getValue().getPageSize());
        verify(asistenciaRepository, never()).findUltimasAsistencias();
    }

    @Test
    void obtenerUltimaMarcacion_sinMarcas_devuelveNull() {
        when(asistenciaRepository.findUltimaMarcacion(empleado.getId())).thenReturn(null);

        assertNull(service.obtenerUltimaMarcacion(empleado.getId()));
        verifyNoInteractions(asistenciaMapper);
    }

    @Test
    void obtenerUltimaMarcacion_conMarca_mapea() {
        RrhhAsistencia a = new RrhhAsistencia();
        RrhhAsistenciaResponseDTO dto = mock(RrhhAsistenciaResponseDTO.class);
        when(asistenciaRepository.findUltimaMarcacion(empleado.getId())).thenReturn(a);
        when(asistenciaMapper.toResponseDto(a)).thenReturn(dto);

        assertSame(dto, service.obtenerUltimaMarcacion(empleado.getId()));
    }

    @Test
    void obtenerEstadoDelDia_reflejaMarcasYMensajes() {
        marcasPrevias(true, false);

        AsistenciaEstadoDTO e = service.obtenerEstadoDelDia(empleado.getId());

        assertTrue(e.yaMarcoEntrada());
        assertFalse(e.yaMarcoSalida());
        assertTrue(e.mensajeEntrada().contains("ENTRADA registrada"));
        assertTrue(e.mensajeSalida().contains("No ha marcado SALIDA"));
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void listarTodasYBuscarPorEmpleado_delegan() {
        Pageable p = PageRequest.of(0, 10);
        when(asistenciaRepository.findAll(p)).thenReturn(new PageImpl<>(List.of(new RrhhAsistencia(), new RrhhAsistencia())));
        when(asistenciaRepository.findByEmpleadoId(empleado.getId(), p)).thenReturn(new PageImpl<>(List.of(new RrhhAsistencia())));

        assertEquals(2, service.listarTodas(p).getTotalElements());
        assertEquals(1, service.buscarPorEmpleado(empleado.getId(), p).getTotalElements());
    }

    @Test
    void buscarPorId_existente_mapea() {
        UUID id = UUID.randomUUID();
        RrhhAsistencia a = new RrhhAsistencia();
        when(asistenciaRepository.findById(id)).thenReturn(Optional.of(a));

        service.buscarPorId(id);

        verify(asistenciaMapper).toResponseDto(a);
    }
}
