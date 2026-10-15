package com.policalum.erp.application.service.serviceImpl;

import org.springframework.data.domain.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.projection.NominaTotalesPorEstadoProjection;
import com.policalum.erp.application.dto.response.NominaDashboardDTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.GenerarNominaRequestDTO;
import com.policalum.erp.application.dto.request.PagarNominaRequestDTO;
import com.policalum.erp.application.dto.response.BoletaPagoDTO;
import com.policalum.erp.application.dto.response.GeneracionNominaResponseDTO;
import com.policalum.erp.application.mapper.NominaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class NominaServiceImplTest {

    @Mock RrhhNominaRepository nominaRepository;
    @Mock RrhhLegajoDigitalRepository legajoRepository;
    @Mock RrhhComisionRepository comisionRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock NominaMapper mapper;
    @Mock AuditService auditService;
    @InjectMocks NominaServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    private static final String PERIODO = "2026-09";
    UUID sedeId = UUID.randomUUID();
    UUID nominaId = UUID.randomUUID();
    Sede sede;
    User actual;
    User empleado;

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(sedeId).nombre("Trujillo Centro").activa(true).build();
        actual = usuario("Miguel", RolNombre.ROLE_ADMIN, EMAIL);
        empleado = usuario("Ana", RolNombre.ROLE_MULTIFUNCIONAL, "ana@policalum.com");

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(nominaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== GENERAR =====================

    @Test
    void generar_planillaOnpConComision_calculaNetoCorrecto() {
        // given: 1500 + comisión 200 = 1700; ONP 13% = 221.00; neto 1479.00
        RrhhLegajoDigital legajo = legajo("PLANILLA", "1500.00", SistemaPension.ONP, null, LocalDate.of(2024, 1, 10));
        RrhhComision comision = comision(EstadoComision.APROBADA, "200.00");
        prepararGeneracion(legajo, Optional.of(comision));
        ArgumentCaptor<RrhhNomina> captor = ArgumentCaptor.forClass(RrhhNomina.class);

        // when
        GeneracionNominaResponseDTO r = service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        verify(nominaRepository).save(captor.capture());
        RrhhNomina n = captor.getValue();
        assertEquals(new BigDecimal("1500.00"), n.getSueldoBaseCalculado());
        assertEquals(new BigDecimal("200.00"), n.getTotalComisiones());
        assertEquals(new BigDecimal("221.00"), n.getDescuentoPension());
        assertEquals(new BigDecimal("1479.00"), n.getTotalNeto());
        assertEquals(new BigDecimal("13.00"), n.getTasaPension());
        assertEquals(30, n.getDiasComputados());
        assertSame(comision, n.getComision());
        assertSame(sede, n.getSede());
        assertSame(actual, n.getGeneradoPor());
        assertEquals(EstadoNomina.PENDIENTE, n.getEstado());
        assertEquals(1, r.generadas());
        assertEquals(new BigDecimal("1479.00"), r.totalNetoGenerado());
        verify(auditService).registrar(eq("rrhh_nominas"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void generar_afpConTasaDelLegajo_aplicaEsaTasa() {
        // given: 2000 × 11.37% = 227.40
        prepararGeneracion(legajo("PLANILLA", "2000.00", SistemaPension.AFP, "11.37", LocalDate.of(2024, 1, 1)), Optional.empty());
        ArgumentCaptor<RrhhNomina> captor = ArgumentCaptor.forClass(RrhhNomina.class);

        // when
        service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        verify(nominaRepository).save(captor.capture());
        assertEquals(new BigDecimal("227.40"), captor.getValue().getDescuentoPension());
        assertEquals(new BigDecimal("1772.60"), captor.getValue().getTotalNeto());
    }

    @Test
    void generar_reciboHonorarios_noDescuentaPension() {
        // given: aunque el legajo diga ONP, RxH no descuenta
        prepararGeneracion(legajo("RECIBO_HONORARIOS", "1200.00", SistemaPension.ONP, null, LocalDate.of(2024, 1, 1)), Optional.empty());
        ArgumentCaptor<RrhhNomina> captor = ArgumentCaptor.forClass(RrhhNomina.class);

        // when
        service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        verify(nominaRepository).save(captor.capture());
        assertEquals(SistemaPension.NINGUNO, captor.getValue().getSistemaPension());
        assertEquals(0, captor.getValue().getDescuentoPension().signum());
        assertEquals(new BigDecimal("1200.00"), captor.getValue().getTotalNeto());
    }

    @Test
    void generar_ingresoAMitadDeMes_prorrateaSueldo() {
        // given: ingresó el 16/09 → 15 días de 30 → 1500 × 15/30 = 750
        prepararGeneracion(legajo("PLANILLA", "1500.00", SistemaPension.NINGUNO, null, LocalDate.of(2026, 9, 16)), Optional.empty());
        ArgumentCaptor<RrhhNomina> captor = ArgumentCaptor.forClass(RrhhNomina.class);

        // when
        service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        verify(nominaRepository).save(captor.capture());
        assertEquals(15, captor.getValue().getDiasComputados());
        assertEquals(new BigDecimal("750.00"), captor.getValue().getSueldoBaseCalculado());
    }

    @Test
    void generar_legajoSinSistemaPension_generaConAdvertencia() {
        // given
        prepararGeneracion(legajo("PLANILLA", "1500.00", null, null, LocalDate.of(2024, 1, 1)), Optional.empty());
        ArgumentCaptor<RrhhNomina> captor = ArgumentCaptor.forClass(RrhhNomina.class);

        // when
        GeneracionNominaResponseDTO r = service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        verify(nominaRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getDescuentoPension().signum());
        assertTrue(captor.getValue().getObservaciones().contains("sin sistema de pensión"));
        assertEquals(1, r.advertencias().size());
    }

    @Test
    void generar_afpSinTasa_generaAdvertenciaYNoDescuenta() {
        // given
        prepararGeneracion(legajo("PLANILLA", "2000.00", SistemaPension.AFP, null, LocalDate.of(2024, 1, 1)), Optional.empty());
        ArgumentCaptor<RrhhNomina> captor = ArgumentCaptor.forClass(RrhhNomina.class);

        // when
        GeneracionNominaResponseDTO r = service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        verify(nominaRepository).save(captor.capture());
        assertEquals(0, captor.getValue().getDescuentoPension().signum());
        assertTrue(captor.getValue().getObservaciones().contains("AFP sin tasa"));
        assertEquals(1, r.advertencias().size());
    }

    @Test
    void generar_empleadoConNominaExistente_loOmite() {
        // given
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(comisionRepository.countByPeriodoAndEstadoAndSede_Id(PERIODO, EstadoComision.CALCULADA, sedeId)).thenReturn(0L);
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo("PLANILLA", "1500.00", SistemaPension.ONP, null, LocalDate.of(2024, 1, 1))));
        when(nominaRepository.existsByUser_IdAndPeriodo(empleado.getId(), PERIODO)).thenReturn(true);

        // when
        GeneracionNominaResponseDTO r = service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId));

        // then
        assertEquals(0, r.generadas());
        assertEquals(1, r.omitidas());
        verify(nominaRepository, never()).save(any());
    }

    @Test
    void generar_conComisionesSinAprobar_lanzaIllegalState() {
        // given
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(comisionRepository.countByPeriodoAndEstadoAndSede_Id(PERIODO, EstadoComision.CALCULADA, sedeId)).thenReturn(2L);

        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId)));
        assertTrue(ex.getMessage().contains("2 comisión"));
        verifyNoInteractions(legajoRepository);
    }

    @Test
    void generar_periodoFuturo_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generar(new GenerarNominaRequestDTO(YearMonth.now().plusMonths(1).toString(), sedeId)));
        verifyNoInteractions(sedeRepository, legajoRepository);
    }

    @Test
    void generar_usuarioNoAdmin_lanzaAccessDenied() {
        actual = usuario("Ana", RolNombre.ROLE_MULTIFUNCIONAL, EMAIL);
        assertThrows(AccessDeniedException.class, () -> service.generar(new GenerarNominaRequestDTO(PERIODO, sedeId)));
        verifyNoInteractions(legajoRepository, nominaRepository);
    }

    // ===================== FLUJO DE ESTADOS =====================

    @Test
    void aprobar_nominaPendiente_pasaAAprobada() {
        // given
        RrhhNomina n = nomina(EstadoNomina.PENDIENTE, null);
        when(nominaRepository.findByIdParaActualizar(nominaId)).thenReturn(Optional.of(n));

        // when
        service.aprobar(nominaId);

        // then
        assertEquals(EstadoNomina.APROBADA, n.getEstado());
        assertSame(actual, n.getAprobadoPor());
        assertNotNull(n.getFechaAprobacion());
    }

    @Test
    void aprobar_nominaYaPagada_lanzaIllegalState() {
        when(nominaRepository.findByIdParaActualizar(nominaId)).thenReturn(Optional.of(nomina(EstadoNomina.PAGADA, null)));
        assertThrows(IllegalStateException.class, () -> service.aprobar(nominaId));
        verify(nominaRepository, never()).save(any());
    }

    @Test
    void pagar_nominaAprobada_marcaPagadaYLiquidaComision() {
        // given
        RrhhComision comision = comision(EstadoComision.APROBADA, "200.00");
        RrhhNomina n = nomina(EstadoNomina.APROBADA, comision);
        when(nominaRepository.findByIdParaActualizar(nominaId)).thenReturn(Optional.of(n));

        // when
        service.pagar(nominaId, new PagarNominaRequestDTO(MetodoPago.TRANSFERENCIA, "OP-123"));

        // then
        assertEquals(EstadoNomina.PAGADA, n.getEstado());
        assertEquals(MetodoPago.TRANSFERENCIA, n.getMetodoPago());
        assertEquals("OP-123", n.getReferenciaPago());
        assertSame(actual, n.getPagadoPor());
        assertNotNull(n.getFechaPago());
        assertEquals(EstadoComision.LIQUIDADA, comision.getEstado());
        verify(comisionRepository).save(comision);
    }

    @Test
    void pagar_nominaPendiente_lanzaIllegalState() {
        when(nominaRepository.findByIdParaActualizar(nominaId)).thenReturn(Optional.of(nomina(EstadoNomina.PENDIENTE, null)));
        assertThrows(IllegalStateException.class,
                () -> service.pagar(nominaId, new PagarNominaRequestDTO(MetodoPago.EFECTIVO, null)));
    }

    @Test
    void eliminar_nominaPendiente_laBorraYAudita() {
        // given
        RrhhNomina n = nomina(EstadoNomina.PENDIENTE, null);
        when(nominaRepository.findByIdParaActualizar(nominaId)).thenReturn(Optional.of(n));

        // when
        service.eliminar(nominaId);

        // then
        verify(nominaRepository).delete(n);
        verify(auditService).registrar(eq("rrhh_nominas"), eq(nominaId), eq(AccionAudit.DELETE), anyMap());
    }

    @Test
    void eliminar_nominaAprobada_lanzaIllegalState() {
        when(nominaRepository.findByIdParaActualizar(nominaId)).thenReturn(Optional.of(nomina(EstadoNomina.APROBADA, null)));
        assertThrows(IllegalStateException.class, () -> service.eliminar(nominaId));
        verify(nominaRepository, never()).delete(any());
    }

    // ===================== BOLETA =====================

    @Test
    void boleta_empleadoVeLaSuya() {
        // given: el usuario autenticado es el mismo empleado de la nómina
        actual = empleado;
        RrhhNomina n = nomina(EstadoNomina.PAGADA, null);
        when(nominaRepository.findDetalleById(nominaId)).thenReturn(Optional.of(n));
        when(legajoRepository.findByUser_Id(empleado.getId())).thenReturn(Optional.empty());

        // when
        BoletaPagoDTO b = service.boleta(nominaId);

        // then
        assertEquals(new BigDecimal("1479.00"), b.netoAPagar());
        assertEquals(new BigDecimal("1700.00"), b.totalIngresos());
        assertEquals(2, b.ingresos().size());
        assertEquals(1, b.descuentos().size());
    }

    @Test
    void boleta_deOtroEmpleado_lanzaAccessDenied() {
        // given: un multifuncional pide la boleta de otra persona
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL, EMAIL);
        when(nominaRepository.findDetalleById(nominaId)).thenReturn(Optional.of(nomina(EstadoNomina.PAGADA, null)));

        // when / then
        assertThrows(AccessDeniedException.class, () -> service.boleta(nominaId));
    }

    // ===================== CÁLCULO DE DÍAS =====================

    @Test
    void diasComputados_casosBorde() {
        YearMonth sep = YearMonth.of(2026, 9);
        assertEquals(30, NominaServiceImpl.diasComputados(LocalDate.of(2025, 5, 1), sep));
        assertEquals(30, NominaServiceImpl.diasComputados(LocalDate.of(2026, 9, 1), sep));
        assertEquals(1, NominaServiceImpl.diasComputados(LocalDate.of(2026, 9, 30), sep));
        assertEquals(0, NominaServiceImpl.diasComputados(LocalDate.of(2026, 10, 1), sep));
        assertEquals(1, NominaServiceImpl.diasComputados(LocalDate.of(2026, 10, 31), YearMonth.of(2026, 10)));
    }

    // ===================== HELPERS =====================

    private void prepararGeneracion(RrhhLegajoDigital legajo, Optional<RrhhComision> comision) {
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        when(comisionRepository.countByPeriodoAndEstadoAndSede_Id(PERIODO, EstadoComision.CALCULADA, sedeId)).thenReturn(0L);
        when(legajoRepository.findActivos(sedeId)).thenReturn(List.of(legajo));
        when(nominaRepository.existsByUser_IdAndPeriodo(empleado.getId(), PERIODO)).thenReturn(false);
        when(comisionRepository.findByUser_IdAndPeriodo(empleado.getId(), PERIODO)).thenReturn(comision);
    }

    private User usuario(String nombre, RolNombre rol, String email) {
        return User.builder().id(UUID.randomUUID()).nombre(nombre).apellido("Prueba").email(email)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private RrhhLegajoDigital legajo(String contrato, String sueldo, SistemaPension sistema, String tasa, LocalDate ingreso) {
        return RrhhLegajoDigital.builder().id(UUID.randomUUID()).user(empleado).tipoContrato(contrato)
                .sueldoBase(new BigDecimal(sueldo)).porcentajeComision(new BigDecimal("3.00"))
                .sistemaPension(sistema).tasaPension(tasa == null ? null : new BigDecimal(tasa))
                .fechaIngreso(ingreso).activo(true).build();
    }

    private RrhhComision comision(EstadoComision estado, String monto) {
        return RrhhComision.builder().id(UUID.randomUUID()).user(empleado).sede(sede).periodo(PERIODO)
                .totalVentasNetas(new BigDecimal("6666.67")).cantidadVentas(10L)
                .porcentajeComision(new BigDecimal("3.00")).montoComision(new BigDecimal(monto)).estado(estado).build();
    }

    private RrhhNomina nomina(EstadoNomina estado, RrhhComision comision) {
        return RrhhNomina.builder().id(nominaId).user(empleado).sede(sede).periodo(PERIODO)
                .tipoContrato("PLANILLA").diasComputados(30)
                .sueldoBaseCalculado(new BigDecimal("1500.00")).comision(comision)
                .ventasBaseComision(new BigDecimal("6666.67")).porcentajeComision(new BigDecimal("3.00"))
                .totalComisiones(new BigDecimal("200.00"))
                .sistemaPension(SistemaPension.ONP).tasaPension(new BigDecimal("13.00"))
                .descuentoPension(new BigDecimal("221.00")).otrosDescuentos(BigDecimal.ZERO)
                .descuentos(new BigDecimal("221.00")).totalNeto(new BigDecimal("1479.00"))
                .estado(estado).build();
    }

    // ===================== Completado en auditoría de cobertura =====================
    private NominaTotalesPorEstadoProjection totales(EstadoNomina estado, Long cantidad, String sueldos, String comisiones, String descuentos, String neto) {
        NominaTotalesPorEstadoProjection t = mock(NominaTotalesPorEstadoProjection.class);
        lenient().when(t.getEstado()).thenReturn(estado);
        lenient().when(t.getCantidad()).thenReturn(cantidad);
        lenient().when(t.getSueldos()).thenReturn(sueldos == null ? null : new BigDecimal(sueldos));
        lenient().when(t.getComisiones()).thenReturn(comisiones == null ? null : new BigDecimal(comisiones));
        lenient().when(t.getDescuentos()).thenReturn(descuentos == null ? null : new BigDecimal(descuentos));
        lenient().when(t.getNeto()).thenReturn(neto == null ? null : new BigDecimal(neto));
        return t;
    }

    @Test
    void dashboard_sumaPorEstadoYSeparaPagadoDePorPagar() {
        List<NominaTotalesPorEstadoProjection> filas = List.of(
                totales(EstadoNomina.PENDIENTE, 2L, "3000.00", "200.00", "400.00", "2800.00"),
                totales(EstadoNomina.APROBADA, 1L, "1500.00", "0.00", "195.00", "1305.00"),
                totales(EstadoNomina.PAGADA, 3L, "4500.00", "300.00", "600.00", "4200.00"));
        when(nominaRepository.totalesPorEstado(PERIODO, sedeId)).thenReturn(filas);

        NominaDashboardDTO d = service.dashboard(PERIODO, sedeId);

        assertEquals(PERIODO, d.periodo());
        assertEquals(2, d.cantidadPendientes());
        assertEquals(1, d.cantidadAprobadas());
        assertEquals(3, d.cantidadPagadas());
        assertEquals(new BigDecimal("9000.00"), d.totalSueldos());
        assertEquals(new BigDecimal("500.00"), d.totalComisiones());
        assertEquals(new BigDecimal("1195.00"), d.totalDescuentos());
        assertEquals(new BigDecimal("8305.00"), d.totalNeto());
        assertEquals(new BigDecimal("4200.00"), d.totalPagado());
        assertEquals(new BigDecimal("4105.00"), d.totalPorPagar());
        assertEquals(0, d.totalNeto().compareTo(d.totalPagado().add(d.totalPorPagar())));
    }

    @Test
    void dashboard_valoresNulosEnProyeccion_seTomanComoCero() {
        List<NominaTotalesPorEstadoProjection> filas = List.of(totales(EstadoNomina.PENDIENTE, null, null, null, null, null));
        when(nominaRepository.totalesPorEstado(PERIODO, null)).thenReturn(filas);

        NominaDashboardDTO d = service.dashboard(PERIODO, null);

        assertEquals(0, d.cantidadPendientes());
        assertEquals(0, d.totalNeto().signum());
        assertEquals(0, d.totalPorPagar().signum());
    }

    @Test
    void dashboard_sinPeriodo_usaElMesActual() {
        when(nominaRepository.totalesPorEstado(anyString(), any())).thenReturn(List.of());

        NominaDashboardDTO d = service.dashboard("  ", null);

        assertEquals(YearMonth.now().toString(), d.periodo());
        assertEquals(0, d.totalSueldos().signum());
    }

    @Test
    void dashboard_periodoInvalido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.dashboard("2026-13", null));
        verifyNoInteractions(nominaRepository);
    }

    @Test
    void dashboard_noAdmin_lanzaAccessDenied() {
        actual = empleado;

        assertThrows(AccessDeniedException.class, () -> service.dashboard(PERIODO, null));
    }

    @Test
    void buscar_periodoNormalizadoYFiltros() {
        Pageable p = PageRequest.of(0, 10);
        RrhhNomina n = nomina(EstadoNomina.PENDIENTE, null);
        when(nominaRepository.search("2026-09", sedeId, EstadoNomina.PENDIENTE, null, p)).thenReturn(new PageImpl<>(List.of(n)));

        assertEquals(1, service.buscar("2026-09", sedeId, EstadoNomina.PENDIENTE, null, p).getTotalElements());
        verify(mapper).toListadoDto(n);
    }

    @Test
    void buscar_periodoVacio_noFiltraPorPeriodo() {
        Pageable p = PageRequest.of(0, 10);
        when(nominaRepository.search(isNull(), any(), any(), any(), eq(p))).thenReturn(Page.empty());

        service.buscar("", null, null, null, p);

        verify(nominaRepository).search(null, null, null, null, p);
    }

    @Test
    void buscar_periodoMalFormado_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.buscar("09-2026", null, null, null, PageRequest.of(0, 10)));
    }

    @Test
    void buscar_noAdmin_lanzaAccessDenied() {
        actual = empleado;

        assertThrows(AccessDeniedException.class, () -> service.buscar(null, null, null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(nominaRepository);
    }

    @Test
    void obtener_existente_mapeaDetalle() {
        RrhhNomina n = nomina(EstadoNomina.APROBADA, null);
        when(nominaRepository.findDetalleById(nominaId)).thenReturn(Optional.of(n));

        service.obtener(nominaId);

        verify(mapper).toResponseDto(n);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(nominaRepository.findDetalleById(nominaId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(nominaId));
    }
}
