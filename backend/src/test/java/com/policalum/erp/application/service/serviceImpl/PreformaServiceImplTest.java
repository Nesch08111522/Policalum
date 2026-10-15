package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.ParametrosVentasDTO;
import com.policalum.erp.application.dto.response.PreformaDashboardDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.mapper.DetallePreformaMapper;
import com.policalum.erp.application.mapper.PreformaVentaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.VentaService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.PreformasPorEstadoProjection;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PreformaServiceImplTest {

    @Mock PreformaVentaRepository preformaRepository;
    @Mock DetallePreformaRepository detalleRepository;
    @Mock SedeRepository sedeRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock CatalogoFraccionRepository fraccionRepository;
    @Mock ServicioRepository servicioRepository;
    @Mock TurnoCajaRepository turnoRepository;
    @Mock VentaRepository ventaRepository;
    @Mock UserRepository userRepository;
    @Mock VentaService ventaService;
    @Mock CorrelativoService correlativoService;
    @Mock PreformaVentaMapper mapper;
    @Mock DetallePreformaMapper detalleMapper;
    @Mock AuditService auditService;
    @Mock ConfiguracionService configuracionService;
    @InjectMocks PreformaServiceImpl service;

    private static final String EMAIL = "user@policalum.com";
    UUID preformaId = UUID.randomUUID();
    UUID almacenId = UUID.randomUUID();
    Sede sede;
    User actual;
    User vendedor;
    Producto plancha;      // S/ 100.00 entera
    Producto fraccionable; // solo por fracción
    CatalogoFraccion media; // S/ 55.00
    Servicio corte;        // S/ 20.00

    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.ventas()).thenReturn(ParametrosVentasDTO.DEFAULTS);
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Trujillo Centro").activa(true).build();
        vendedor = usuario("Ana", RolNombre.ROLE_MULTIFUNCIONAL);
        actual = vendedor;
        plancha = Producto.builder().id(UUID.randomUUID()).nombre("Policarbonato 6mm").activo(true).esFraccionable(false)
                .precioVentaBase(new BigDecimal("100.00")).build();
        fraccionable = Producto.builder().id(UUID.randomUUID()).nombre("Policarbonato 8mm").activo(true).esFraccionable(true)
                .precioVentaBase(new BigDecimal("150.00")).build();
        media = CatalogoFraccion.builder().id(UUID.randomUUID()).producto(fraccionable).activo(true)
                .nombreFraccion("Media").precioVenta(new BigDecimal("55.00")).build();
        corte = Servicio.builder().id(UUID.randomUUID()).nombre("Corte").activo(true).precio(new BigDecimal("20.00")).build();

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(sedeRepository.findById(sede.getId())).thenReturn(Optional.of(sede));
        lenient().when(productoRepository.findById(plancha.getId())).thenReturn(Optional.of(plancha));
        lenient().when(productoRepository.findById(fraccionable.getId())).thenReturn(Optional.of(fraccionable));
        lenient().when(fraccionRepository.findWithProductoById(media.getId())).thenReturn(Optional.of(media));
        lenient().when(servicioRepository.findById(corte.getId())).thenReturn(Optional.of(corte));
        lenient().when(correlativoService.siguiente(anyString())).thenReturn(1L);
        lenient().when(preformaRepository.save(any())).thenAnswer(i -> {
            PreformaVenta p = i.getArgument(0);
            if (p.getId() == null) p.setId(preformaId);
            return p;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR =====================

    @Test
    void crear_calculaTotalesCodigoYVigenciaPorDefecto() {
        // given: 2 planchas (200.00) + 1 corte (20.00) = 220.00
        ArgumentCaptor<PreformaVenta> captor = ArgumentCaptor.forClass(PreformaVenta.class);
        LocalDateTime antes = LocalDateTime.now();

        // when
        service.crear(request(null, linea(plancha, null, "2"), lineaServicio("1")));

        // then
        verify(preformaRepository).save(captor.capture());
        PreformaVenta p = captor.getValue();
        assertEquals("PRE-" + Year.now().getValue() + "-000001", p.getCodigo());
        assertEquals(EstadoPreforma.ABIERTA, p.getEstado());
        assertEquals(new BigDecimal("220.00"), p.getTotalEstimado());
        assertEquals(new BigDecimal("186.44"), p.getSubtotalNetoEstimado());
        assertEquals(new BigDecimal("33.56"), p.getTotalIgvEstimado());
        assertSame(vendedor, p.getUser());
        assertFalse(p.getFechaExpiracion().isBefore(antes.plusMinutes(120)));
        assertTrue(p.getFechaExpiracion().isBefore(LocalDateTime.now().plusMinutes(121)));
        verify(detalleRepository).saveAll(argThat(l -> ((List<?>) l).size() == 2));
        verify(auditService).registrar(eq("preformas_venta"), eq(preformaId), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_conFraccion_usaPrecioDeLaFraccion() {
        ArgumentCaptor<PreformaVenta> captor = ArgumentCaptor.forClass(PreformaVenta.class);

        service.crear(request(30, linea(fraccionable, media, "3")));

        verify(preformaRepository).save(captor.capture());
        assertEquals(new BigDecimal("165.00"), captor.getValue().getTotalEstimado());
        assertTrue(captor.getValue().getFechaExpiracion().isBefore(LocalDateTime.now().plusMinutes(31)));
    }

    @Test
    void crear_vigenciaFueraDeRango_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(request(2, linea(plancha, null, "1"))));
        verify(preformaRepository, never()).save(any());
    }

    @Test
    void crear_lineaConProductoYServicio_lanzaIllegalArgument() {
        DetallePreformaRequestDTO ambos = new DetallePreformaRequestDTO(plancha.getId(), null, corte.getId(), BigDecimal.ONE);
        assertThrows(IllegalArgumentException.class, () -> service.crear(request(null, ambos)));
    }

    @Test
    void crear_productoFraccionableSinFraccion_lanzaIllegalArgument() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.crear(request(null, linea(fraccionable, null, "1"))));
        assertTrue(ex.getMessage().contains("fracción"));
    }

    @Test
    void crear_fraccionDeOtroProducto_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(request(null, linea(plancha, media, "1"))));
    }

    // ===================== CONSULTAR / EXPIRAR =====================

    @Test
    void obtener_abiertaVencida_seDevuelveExpirada() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().minusMinutes(1));
        when(preformaRepository.findDetalleById(preformaId)).thenReturn(Optional.of(p));
        when(detalleRepository.findByPreforma_Id(preformaId)).thenReturn(List.of());

        service.obtener(preformaId);

        assertEquals(EstadoPreforma.EXPIRADA, p.getEstado());
        verify(preformaRepository).save(p);
    }

    @Test
    void obtener_vigente_noCambiaEstado() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        when(preformaRepository.findDetalleById(preformaId)).thenReturn(Optional.of(p));
        when(detalleRepository.findByPreforma_Id(preformaId)).thenReturn(List.of());

        service.obtener(preformaId);

        assertEquals(EstadoPreforma.ABIERTA, p.getEstado());
        verify(preformaRepository, never()).save(any());
    }

    @Test
    void abiertas_excluyeYMarcaLasVencidas() {
        PreformaVenta vigente = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(10));
        PreformaVenta vencida = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().minusMinutes(10));
        when(preformaRepository.findByUser_IdAndEstadoOrderByFechaExpiracionAsc(vendedor.getId(), EstadoPreforma.ABIERTA))
                .thenReturn(List.of(vencida, vigente));

        assertEquals(1, service.abiertasDelUsuario().size());
        assertEquals(EstadoPreforma.EXPIRADA, vencida.getEstado());
        assertEquals(EstadoPreforma.ABIERTA, vigente.getEstado());
    }

    @Test
    void vencidas_multifuncionalVeSoloLasSuyasTrasExpirar() {
        Pageable pageable = Pageable.unpaged();
        when(preformaRepository.buscarPorEstado(EstadoPreforma.EXPIRADA, null, vendedor.getId(), pageable)).thenReturn(Page.empty());

        service.vencidas(null, pageable);

        InOrder orden = inOrder(preformaRepository);
        orden.verify(preformaRepository).expirarVencidas(any());
        orden.verify(preformaRepository).buscarPorEstado(EstadoPreforma.EXPIRADA, null, vendedor.getId(), pageable);
    }

    // ===================== EDITAR / ELIMINAR =====================

    @Test
    void actualizar_recalculaYReemplazaLineas() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        when(preformaRepository.findByIdParaActualizar(preformaId)).thenReturn(Optional.of(p));

        service.actualizar(preformaId, request(60, linea(plancha, null, "5")));

        assertEquals(new BigDecimal("500.00"), p.getTotalEstimado());
        assertTrue(p.getFechaExpiracion().isAfter(LocalDateTime.now().plusMinutes(59)));
        InOrder orden = inOrder(detalleRepository);
        orden.verify(detalleRepository).eliminarPorPreforma(preformaId);
        orden.verify(detalleRepository).saveAll(any());
        verify(auditService).registrar(eq("preformas_venta"), eq(preformaId), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void actualizar_deOtroVendedor_lanzaAccessDenied() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL);
        when(preformaRepository.findByIdParaActualizar(preformaId))
                .thenReturn(Optional.of(preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30))));
        assertThrows(AccessDeniedException.class, () -> service.actualizar(preformaId, request(null, linea(plancha, null, "1"))));
        verify(detalleRepository, never()).eliminarPorPreforma(any());
    }

    @Test
    void actualizar_vencida_lanzaIllegalState() {
        when(preformaRepository.findByIdParaActualizar(preformaId))
                .thenReturn(Optional.of(preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().minusMinutes(1))));
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.actualizar(preformaId, request(null, linea(plancha, null, "1"))));
        assertTrue(ex.getMessage().contains("expiró"));
    }

    @Test
    void eliminar_abierta_borraLineasYPreforma() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        when(preformaRepository.findByIdParaActualizar(preformaId)).thenReturn(Optional.of(p));

        service.eliminar(preformaId);

        verify(detalleRepository).eliminarPorPreforma(preformaId);
        verify(preformaRepository).delete(p);
        verify(auditService).registrar(eq("preformas_venta"), eq(preformaId), eq(AccionAudit.DELETE), anyMap());
    }

    @Test
    void eliminar_confirmada_lanzaIllegalState() {
        when(preformaRepository.findByIdParaActualizar(preformaId))
                .thenReturn(Optional.of(preforma(EstadoPreforma.CONFIRMADA, vendedor, LocalDateTime.now().plusMinutes(30))));
        assertThrows(IllegalStateException.class, () -> service.eliminar(preformaId));
        verify(preformaRepository, never()).delete(any());
    }

    // ===================== CONFIRMAR =====================

    @Test
    void confirmar_cajeroDistintoAlVendedor_creaVentaEnSuTurno() {
        // given: Ana creó la preforma; Carla (cajera con turno) la cobra
        User cajera = usuario("Carla", RolNombre.ROLE_MULTIFUNCIONAL);
        actual = cajera;
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        p.setTotalEstimado(new BigDecimal("200.00"));
        TurnoCaja turno = TurnoCaja.builder().id(UUID.randomUUID()).user(cajera).sede(sede).build();
        UUID ventaId = UUID.randomUUID();
        prepararConfirmacion(p, turno, cajera, List.of(detalle(p, plancha, "2")));
        when(ventaService.registrar(any())).thenReturn(new VentaProcesadaResponseDTO(ventaId, "B001-9",
                new BigDecimal("169.49"), new BigDecimal("30.51"), new BigDecimal("200.00"), "COMPLETADA"));
        Venta venta = Venta.builder().id(ventaId).build();
        when(ventaRepository.findById(ventaId)).thenReturn(Optional.of(venta));
        ArgumentCaptor<RegistrarVentaRequestDTO> captor = ArgumentCaptor.forClass(RegistrarVentaRequestDTO.class);

        // when
        service.confirmar(preformaId, confirmar("200.00"));

        // then
        verify(ventaService).registrar(captor.capture());
        RegistrarVentaRequestDTO r = captor.getValue();
        assertEquals(turno.getId(), r.turnoCajaId());
        assertEquals(preformaId, r.preformaId());
        assertEquals(CanalVenta.LOCAL, r.canalVenta());
        assertEquals(1, r.lineas().size());
        assertEquals(plancha.getId(), r.lineas().get(0).productoId());
        assertEquals(EstadoPreforma.CONFIRMADA, p.getEstado());
        assertSame(venta, p.getVenta());
        assertSame(turno, p.getTurnoCaja());
        assertSame(cajera, p.getConfirmadoPor());
        assertNotNull(p.getFechaConfirmacion());
    }

    @Test
    void confirmar_sinTurnoAbierto_lanzaIllegalStateSinCrearVenta() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        when(preformaRepository.findByIdParaActualizar(preformaId)).thenReturn(Optional.of(p));
        when(turnoRepository.findByUser_IdAndSede_IdAndEstado(vendedor.getId(), sede.getId(), EstadoTurno.ABIERTO)).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.confirmar(preformaId, confirmar("200.00")));
        assertTrue(ex.getMessage().contains("turno de caja ABIERTO"));
        verifyNoInteractions(ventaService);
    }

    @Test
    void confirmar_preciosCambiaron_lanzaIllegalStateConAmbosTotales() {
        // given: se armó a 90.00 la plancha (180.00) y hoy cuesta 100.00 (200.00)
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        p.setTotalEstimado(new BigDecimal("180.00"));
        prepararConfirmacion(p, TurnoCaja.builder().id(UUID.randomUUID()).build(), vendedor, List.of(detalle(p, plancha, "2")));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.confirmar(preformaId, confirmar("180.00")));
        assertTrue(ex.getMessage().contains("S/ 180.00") && ex.getMessage().contains("S/ 200.00"));
        verifyNoInteractions(ventaService);
        assertEquals(EstadoPreforma.ABIERTA, p.getEstado());
    }

    @Test
    void confirmar_pagosNoCuadran_lanzaIllegalArgument() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        p.setTotalEstimado(new BigDecimal("200.00"));
        prepararConfirmacion(p, TurnoCaja.builder().id(UUID.randomUUID()).build(), vendedor, List.of(detalle(p, plancha, "2")));

        assertThrows(IllegalArgumentException.class, () -> service.confirmar(preformaId, confirmar("150.00")));
        verifyNoInteractions(ventaService);
    }

    @Test
    void confirmar_yaConfirmada_lanzaIllegalState() {
        when(preformaRepository.findByIdParaActualizar(preformaId))
                .thenReturn(Optional.of(preforma(EstadoPreforma.CONFIRMADA, vendedor, LocalDateTime.now().plusMinutes(30))));
        assertThrows(IllegalStateException.class, () -> service.confirmar(preformaId, confirmar("200.00")));
        verifyNoInteractions(ventaService, turnoRepository);
    }

    @Test
    void confirmar_vencida_lanzaIllegalState() {
        when(preformaRepository.findByIdParaActualizar(preformaId))
                .thenReturn(Optional.of(preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().minusSeconds(5))));
        assertThrows(IllegalStateException.class, () -> service.confirmar(preformaId, confirmar("200.00")));
        verifyNoInteractions(ventaService);
    }

    // ===================== CANCELAR =====================

    @Test
    void cancelar_creadorConMotivo_noTocaVentaNiCaja() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        when(preformaRepository.findByIdParaActualizar(preformaId)).thenReturn(Optional.of(p));
        when(detalleRepository.findByPreforma_Id(preformaId)).thenReturn(List.of());

        service.cancelar(preformaId, new CancelarPreformaRequestDTO(" Cliente desistió "));

        assertEquals(EstadoPreforma.CANCELADA, p.getEstado());
        assertEquals("Cliente desistió", p.getMotivoCancelacion());
        assertSame(vendedor, p.getCanceladoPor());
        verifyNoInteractions(ventaService, turnoRepository);
    }

    @Test
    void cancelar_sinCuerpo_seAcepta() {
        PreformaVenta p = preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30));
        when(preformaRepository.findByIdParaActualizar(preformaId)).thenReturn(Optional.of(p));
        when(detalleRepository.findByPreforma_Id(preformaId)).thenReturn(List.of());

        service.cancelar(preformaId, null);

        assertEquals(EstadoPreforma.CANCELADA, p.getEstado());
        assertNull(p.getMotivoCancelacion());
    }

    @Test
    void cancelar_otroMultifuncional_lanzaAccessDenied() {
        actual = usuario("Luis", RolNombre.ROLE_MULTIFUNCIONAL);
        when(preformaRepository.findByIdParaActualizar(preformaId))
                .thenReturn(Optional.of(preforma(EstadoPreforma.ABIERTA, vendedor, LocalDateTime.now().plusMinutes(30))));
        assertThrows(AccessDeniedException.class, () -> service.cancelar(preformaId, null));
    }

    // ===================== DASHBOARD / VIGENCIA =====================

    @Test
    void dashboard_tasaDeConversionSobrePreformasCerradas() {
        // given: 3 confirmadas (S/ 600), 1 cancelada (S/ 100), 1 expirada (S/ 50) → 3/5 = 60.00 %
        actual = usuario("Admin", RolNombre.ROLE_ADMIN);
        when(preformaRepository.totalesPorEstado(any(), any(), any())).thenReturn(List.of(
                porEstado(EstadoPreforma.CONFIRMADA, 3, "600.00"), porEstado(EstadoPreforma.CANCELADA, 1, "100.00"),
                porEstado(EstadoPreforma.EXPIRADA, 1, "50.00")));
        when(preformaRepository.sumarAbiertasVigentes(any(), any())).thenReturn(new BigDecimal("75.00"));
        when(preformaRepository.contarAbiertasVigentes(any(), any())).thenReturn(2L);

        PreformaDashboardDTO d = service.dashboard(null, null, null);

        assertEquals(new BigDecimal("60.00"), d.tasaConversionPorcentaje());
        assertEquals(new BigDecimal("600.00"), d.montoConfirmado());
        assertEquals(new BigDecimal("150.00"), d.montoPerdido());
        assertEquals(0L, d.preformasPorEstado().get("ABIERTA"));
        assertEquals(2, d.abiertasAhora());
        verify(preformaRepository).expirarVencidas(any());
    }

    @Test
    void resolverVigencia_limites() {
        ParametrosVentasDTO d = ParametrosVentasDTO.DEFAULTS;
        assertEquals(120, PreformaServiceImpl.resolverVigencia(null, d));
        assertEquals(5, PreformaServiceImpl.resolverVigencia(5, d));
        assertEquals(4320, PreformaServiceImpl.resolverVigencia(4320, d));
        assertThrows(IllegalArgumentException.class, () -> PreformaServiceImpl.resolverVigencia(4321, d));
        assertThrows(IllegalArgumentException.class, () -> PreformaServiceImpl.resolverVigencia(4, d));
        // Rango configurado distinto (Módulo 14): default 60, mínimo 30, máximo 90
        ParametrosVentasDTO custom = new ParametrosVentasDTO(15, 60, 30, 90);
        assertEquals(60, PreformaServiceImpl.resolverVigencia(null, custom));
        assertThrows(IllegalArgumentException.class, () -> PreformaServiceImpl.resolverVigencia(120, custom));
    }

    // ===================== HELPERS =====================

    private void prepararConfirmacion(PreformaVenta p, TurnoCaja turno, User quienConfirma, List<DetallePreforma> detalles) {
        when(preformaRepository.findByIdParaActualizar(preformaId)).thenReturn(Optional.of(p));
        when(turnoRepository.findByUser_IdAndSede_IdAndEstado(quienConfirma.getId(), sede.getId(), EstadoTurno.ABIERTO))
                .thenReturn(Optional.of(turno));
        when(detalleRepository.findByPreforma_Id(preformaId)).thenReturn(detalles);
    }

    private User usuario(String nombre, RolNombre rol) {
        return User.builder().id(UUID.randomUUID()).nombre(nombre).apellido("Prueba").email(EMAIL)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private PreformaVentaRequestDTO request(Integer minutos, DetallePreformaRequestDTO... lineas) {
        return new PreformaVentaRequestDTO(sede.getId(), null, minutos, null, List.of(lineas));
    }

    private DetallePreformaRequestDTO linea(Producto p, CatalogoFraccion f, String cantidad) {
        return new DetallePreformaRequestDTO(p.getId(), f == null ? null : f.getId(), null, new BigDecimal(cantidad));
    }

    private DetallePreformaRequestDTO lineaServicio(String cantidad) {
        return new DetallePreformaRequestDTO(null, null, corte.getId(), new BigDecimal(cantidad));
    }

    private ConfirmarPreformaRequestDTO confirmar(String monto) {
        return new ConfirmarPreformaRequestDTO(almacenId, TipoComprobante.BOLETA, null,
                List.of(new PagoVentaCommandDTO(MetodoPago.EFECTIVO, new BigDecimal(monto), null)));
    }

    private PreformaVenta preforma(EstadoPreforma estado, User creador, LocalDateTime expira) {
        return PreformaVenta.builder().id(preformaId).codigo("PRE-2026-000001").sede(sede).user(creador)
                .estado(estado).fechaExpiracion(expira).totalEstimado(new BigDecimal("200.00"))
                .subtotalNetoEstimado(new BigDecimal("169.49")).totalIgvEstimado(new BigDecimal("30.51")).build();
    }

    private DetallePreforma detalle(PreformaVenta p, Producto producto, String cantidad) {
        return DetallePreforma.builder().id(UUID.randomUUID()).preforma(p).producto(producto)
                .cantidad(new BigDecimal(cantidad)).precioUnitarioConIgv(producto.getPrecioVentaBase()).build();
    }

    private PreformasPorEstadoProjection porEstado(EstadoPreforma e, long n, String monto) {
        return new PreformasPorEstadoProjection() {
            public EstadoPreforma getEstado() { return e; }
            public Long getCantidad() { return n; }
            public BigDecimal getMonto() { return new BigDecimal(monto); }
        };
    }

    // ===================== EXPIRACIÓN AUTOMÁTICA (auditoría de cobertura) =====================

    @Test
    void expirarVencidas_delegaConLaHoraActualYDevuelveCantidad() {
        LocalDateTime antes = LocalDateTime.now();
        when(preformaRepository.expirarVencidas(any())).thenReturn(3);

        int n = service.expirarVencidas();

        assertEquals(3, n);
        ArgumentCaptor<LocalDateTime> c = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(preformaRepository).expirarVencidas(c.capture());
        assertFalse(c.getValue().isBefore(antes));
        assertFalse(c.getValue().isAfter(LocalDateTime.now()));
    }

    @Test
    void expirarVencidas_ningunaVencida_devuelveCeroSinAuditar() {
        when(preformaRepository.expirarVencidas(any())).thenReturn(0);

        assertEquals(0, service.expirarVencidas());
        verifyNoInteractions(auditService);
    }

    @Test
    void expirarVencidas_noRequiereUsuarioAutenticado() {
        SecurityContextHolder.clearContext(); // corre desde el scheduler, sin sesión
        when(preformaRepository.expirarVencidas(any())).thenReturn(1);

        assertEquals(1, service.expirarVencidas());
        verifyNoInteractions(userRepository);
    }
}
