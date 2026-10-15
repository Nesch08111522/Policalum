package com.policalum.erp.application.service.serviceImpl;

import org.springframework.data.domain.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.DevolucionDashboardDTO;
import com.policalum.erp.application.mapper.DetalleDevolucionMapper;
import com.policalum.erp.application.mapper.DevolucionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.TotalesFinancierosProjection;
import java.math.BigDecimal;
import java.time.Year;
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
class DevolucionServiceImplTest {

    @Mock DevolucionRepository devolucionRepository;
    @Mock DetalleDevolucionRepository detalleRepository;
    @Mock ReembolsoRepository reembolsoRepository;
    @Mock VentaRepository ventaRepository;
    @Mock DetalleVentaRepository detalleVentaRepository;
    @Mock KardexRepository kardexRepository;
    @Mock KardexService kardexService;
    @Mock CorrelativoService correlativoService;
    @Mock UserRepository userRepository;
    @Mock DevolucionMapper mapper;
    @Mock DetalleDevolucionMapper detalleMapper;
    @Mock AuditService auditService;
    @InjectMocks DevolucionServiceImpl service;

    private static final String EMAIL = "user@policalum.com";
    UUID ventaId = UUID.randomUUID();
    UUID devId = UUID.randomUUID();
    Sede sede;
    User actual;
    Venta venta;
    Producto plancha;
    Almacen almacen;
    CatalogoFraccion media;
    DetalleVenta lineaEntera;   // 3 planchas a S/ 118.00
    DetalleVenta lineaMedia;    // 4 medias planchas a S/ 60.00
    DetalleVenta lineaServicio; // corte

    @BeforeEach
    void setUp() {
        sede = Sede.builder().id(UUID.randomUUID()).nombre("Trujillo Centro").build();
        actual = usuario(RolNombre.ROLE_ADMIN);
        venta = Venta.builder().id(ventaId).sede(sede).estado(EstadoVenta.COMPLETADA).serieCorrelativo("B001-1").build();
        plancha = Producto.builder().id(UUID.randomUUID()).nombre("Policarbonato 6mm").build();
        almacen = Almacen.builder().id(UUID.randomUUID()).nombre("Principal").build();
        media = CatalogoFraccion.builder().id(UUID.randomUUID()).nombreFraccion("Media Plancha").factorDescuentoKardex(new BigDecimal("0.5")).build();
        lineaEntera = DetalleVenta.builder().id(UUID.randomUUID()).venta(venta).producto(plancha)
                .cantidad(new BigDecimal("3")).precioUnitarioConIgv(new BigDecimal("118.00")).build();
        lineaMedia = DetalleVenta.builder().id(UUID.randomUUID()).venta(venta).producto(plancha).fraccion(media)
                .cantidad(new BigDecimal("4")).precioUnitarioConIgv(new BigDecimal("60.00")).build();
        lineaServicio = DetalleVenta.builder().id(UUID.randomUUID()).venta(venta).servicio(Servicio.builder().id(UUID.randomUUID()).build())
                .cantidad(BigDecimal.ONE).precioUnitarioConIgv(new BigDecimal("20.00")).build();

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(devolucionRepository.save(any())).thenAnswer(i -> {
            Devolucion d = i.getArgument(0);
            if (d.getId() == null) d.setId(devId);
            return d;
        });
        lenient().when(reembolsoRepository.findFirstByDevolucion_Id(any())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== CREAR =====================

    @Test
    void crear_parcialConFraccion_calculaMontoYCantidadStock() {
        // given: devuelve 1 plancha entera (118) y 2 medias (2 × 60 = 120) → total 238.00
        prepararVenta();
        when(detalleRepository.sumarCantidadDevuelta(any(), any())).thenReturn(BigDecimal.ZERO);
        when(correlativoService.siguiente("DEV-" + Year.now().getValue())).thenReturn(7L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DetalleDevolucion>> detalles = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Devolucion> dev = ArgumentCaptor.forClass(Devolucion.class);

        // when
        service.crear(new DevolucionRequestDTO(ventaId, " Medidas incorrectas ", List.of(
                linea(lineaEntera, "1", EstadoProducto.BUENO), linea(lineaMedia, "2", EstadoProducto.DAÑADO))));

        // then
        verify(devolucionRepository).save(dev.capture());
        verify(detalleRepository).saveAll(detalles.capture());
        Devolucion d = dev.getValue();
        assertEquals("DEV-" + Year.now().getValue() + "-000007", d.getCodigo());
        assertEquals(EstadoDevolucion.PENDIENTE, d.getEstado());
        assertEquals(new BigDecimal("238.00"), d.getMontoTotal());
        assertEquals("Medidas incorrectas", d.getMotivo());
        assertSame(actual, d.getSolicitadoPor());
        DetalleDevolucion medias = detalles.getValue().get(1);
        assertEquals(0, new BigDecimal("1.0").compareTo(medias.getCantidadStock())); // 2 × 0.5
        assertEquals(new BigDecimal("120.00"), medias.getSubtotal());
        assertSame(d, medias.getDevolucion());
        verify(auditService).registrar(eq("devoluciones"), eq(devId), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_ventaAnulada_lanzaIllegalState() {
        venta.setEstado(EstadoVenta.ANULADA);
        when(ventaRepository.findByIdParaActualizar(ventaId)).thenReturn(Optional.of(venta));
        assertThrows(IllegalStateException.class, () -> service.crear(
                new DevolucionRequestDTO(ventaId, "x", List.of(linea(lineaEntera, "1", EstadoProducto.BUENO)))));
        verifyNoInteractions(correlativoService);
    }

    @Test
    void crear_ventaNoExiste_lanzaNotFound() {
        when(ventaRepository.findByIdParaActualizar(ventaId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.crear(
                new DevolucionRequestDTO(ventaId, "x", List.of(linea(lineaEntera, "1", EstadoProducto.BUENO)))));
    }

    @Test
    void crear_lineaDeOtraVenta_lanzaIllegalArgument() {
        prepararVenta();
        DetalleVenta ajena = DetalleVenta.builder().id(UUID.randomUUID()).producto(plancha)
                .cantidad(BigDecimal.ONE).precioUnitarioConIgv(BigDecimal.TEN).build();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(
                new DevolucionRequestDTO(ventaId, "x", List.of(linea(ajena, "1", EstadoProducto.BUENO)))));
        assertTrue(ex.getMessage().contains("no pertenece"));
        verify(devolucionRepository, never()).save(any());
    }

    @Test
    void crear_lineaDeServicio_lanzaIllegalArgument() {
        prepararVenta();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(
                new DevolucionRequestDTO(ventaId, "x", List.of(linea(lineaServicio, "1", EstadoProducto.BUENO)))));
        assertTrue(ex.getMessage().contains("servicios"));
    }

    @Test
    void crear_masDeLoVendidoConsiderandoDevolucionesPrevias_lanzaIllegalArgument() {
        // given: vendió 3, ya hay 2 en devoluciones vigentes → quedan 1; pide 2
        prepararVenta();
        when(detalleRepository.sumarCantidadDevuelta(lineaEntera.getId(), DevolucionServiceImpl.VIGENTES)).thenReturn(new BigDecimal("2"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(
                new DevolucionRequestDTO(ventaId, "x", List.of(linea(lineaEntera, "2", EstadoProducto.BUENO)))));
        assertTrue(ex.getMessage().contains("solo quedan 1"));
    }

    @Test
    void crear_lineaRepetida_lanzaIllegalArgument() {
        prepararVenta();
        when(detalleRepository.sumarCantidadDevuelta(any(), any())).thenReturn(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> service.crear(new DevolucionRequestDTO(ventaId, "x",
                List.of(linea(lineaEntera, "1", EstadoProducto.BUENO), linea(lineaEntera, "1", EstadoProducto.BUENO)))));
    }

    // ===================== APROBAR =====================

    @Test
    void aprobar_bueno_soloReingresaAlAlmacenOriginal() {
        // given
        Devolucion d = devolucion(EstadoDevolucion.PENDIENTE);
        DetalleDevolucion det = detalle(d, lineaEntera, "1", "1", EstadoProducto.BUENO);
        prepararAprobacion(d, List.of(det));
        ArgumentCaptor<KardexMovimientoRequestDTO> mov = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);

        // when
        service.aprobar(devId, new AprobarDevolucionRequestDTO("ok"));

        // then
        verify(kardexService).registrarMovimiento(mov.capture());
        KardexMovimientoRequestDTO m = mov.getValue();
        assertEquals(TipoMovimiento.ENTRADA, m.tipoMovimiento());
        assertEquals(TipoReferencia.DEVOLUCION, m.tipoReferencia());
        assertSame(almacen, m.almacen());
        assertEquals(devId, m.referenciaId());
        assertSame(almacen, det.getAlmacen());
        assertEquals(EstadoDevolucion.APROBADA, d.getEstado());
        assertSame(actual, d.getAprobadoPor());
        assertNotNull(d.getFechaResolucion());
        assertEquals("ok", d.getObservacionAdmin());
    }

    @Test
    void aprobar_danado_entradaYSalidaPorMermaNetoCero() {
        // given: 2 medias DAÑADAS = 1.0 plancha de stock
        Devolucion d = devolucion(EstadoDevolucion.PENDIENTE);
        prepararAprobacion(d, List.of(detalle(d, lineaMedia, "2", "1.0", EstadoProducto.DAÑADO)));
        ArgumentCaptor<KardexMovimientoRequestDTO> mov = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);

        // when
        service.aprobar(devId, null);

        // then
        verify(kardexService, times(2)).registrarMovimiento(mov.capture());
        KardexMovimientoRequestDTO entrada = mov.getAllValues().get(0), salida = mov.getAllValues().get(1);
        assertEquals(TipoMovimiento.ENTRADA, entrada.tipoMovimiento());
        assertEquals(TipoMovimiento.SALIDA, salida.tipoMovimiento());
        assertEquals(TipoReferencia.MERMA, salida.tipoReferencia());
        assertEquals(0, entrada.cantidadMovida().compareTo(salida.cantidadMovida()));
        assertNull(d.getObservacionAdmin());
    }

    @Test
    void aprobar_sinSalidaDeKardexDeLaVenta_lanzaIllegalStateYNoMueveStock() {
        Devolucion d = devolucion(EstadoDevolucion.PENDIENTE);
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(d));
        when(ventaRepository.findByIdParaActualizar(ventaId)).thenReturn(Optional.of(venta));
        when(kardexRepository.findByReferenciaIdAndTipoReferencia(ventaId, TipoReferencia.VENTA)).thenReturn(List.of());
        when(detalleRepository.findByDevolucion_Id(devId)).thenReturn(List.of(detalle(d, lineaEntera, "1", "1", EstadoProducto.BUENO)));

        assertThrows(IllegalStateException.class, () -> service.aprobar(devId, null));
        verifyNoInteractions(kardexService);
        assertEquals(EstadoDevolucion.PENDIENTE, d.getEstado());
    }

    @Test
    void aprobar_yaAprobada_lanzaIllegalState() {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(devolucion(EstadoDevolucion.APROBADA)));
        assertThrows(IllegalStateException.class, () -> service.aprobar(devId, null));
        verifyNoInteractions(kardexService);
    }

    @Test
    void aprobar_ventaAnuladaEntreTanto_lanzaIllegalState() {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(devolucion(EstadoDevolucion.PENDIENTE)));
        venta.setEstado(EstadoVenta.ANULADA);
        when(ventaRepository.findByIdParaActualizar(ventaId)).thenReturn(Optional.of(venta));
        assertThrows(IllegalStateException.class, () -> service.aprobar(devId, null));
    }

    @Test
    void aprobar_multifuncional_lanzaAccessDenied() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL);
        assertThrows(AccessDeniedException.class, () -> service.aprobar(devId, null));
        verifyNoInteractions(devolucionRepository, kardexService);
    }

    // ===================== RECHAZAR =====================

    @Test
    void rechazar_guardaMotivoYNoMueveStock() {
        Devolucion d = devolucion(EstadoDevolucion.PENDIENTE);
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(d));
        when(detalleRepository.findByDevolucion_Id(devId)).thenReturn(List.of());

        service.rechazar(devId, new RechazarDevolucionRequestDTO(" Fuera de plazo "));

        assertEquals(EstadoDevolucion.RECHAZADA, d.getEstado());
        assertEquals("Fuera de plazo", d.getObservacionAdmin());
        verifyNoInteractions(kardexService);
    }

    @Test
    void rechazar_sinMotivo_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.rechazar(devId, new RechazarDevolucionRequestDTO("  ")));
        verifyNoInteractions(devolucionRepository);
    }

    @Test
    void rechazar_yaRechazada_lanzaIllegalState() {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(devolucion(EstadoDevolucion.RECHAZADA)));
        assertThrows(IllegalStateException.class, () -> service.rechazar(devId, new RechazarDevolucionRequestDTO("x")));
    }

    // ===================== CONSULTAS =====================

    @Test
    void porVenta_ventaInexistente_lanzaNotFound() {
        when(ventaRepository.existsById(ventaId)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.porVenta(ventaId));
    }

    @Test
    void dashboard_tasaDeDevolucionSobreVentas() {
        // given: S/ 50 aprobados sobre S/ 1000 vendidos → 5.00 %
        when(devolucionRepository.totalesPorEstado(any(), any(), any())).thenReturn(List.of());
        when(devolucionRepository.sumarMontoResuelto(eq(EstadoDevolucion.APROBADA), any(), any(), any())).thenReturn(new BigDecimal("50.00"));
        when(ventaRepository.totalesPorPeriodo(eq(EstadoVenta.COMPLETADA), any(), any(), any())).thenReturn(totales("1000.00"));
        when(reembolsoRepository.contarPendientes(any())).thenReturn(1L);
        when(reembolsoRepository.sumarPendientes(any())).thenReturn(new BigDecimal("50.00"));
        when(reembolsoRepository.sumarProcesados(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        when(detalleRepository.sumarUnidadesAprobadas(any(), any(), any(), any())).thenReturn(BigDecimal.ONE, new BigDecimal("0.5"));

        DevolucionDashboardDTO d = service.dashboard(null, null, null);

        assertEquals(new BigDecimal("5.00"), d.tasaDevolucionPorcentaje());
        assertEquals(0L, d.devolucionesPorEstado().get("PENDIENTE"));
        assertEquals(BigDecimal.ONE, d.unidadesReingresadas());
        assertEquals(new BigDecimal("0.5"), d.unidadesMerma());
    }

    // ===================== HELPERS =====================

    private void prepararVenta() {
        when(ventaRepository.findByIdParaActualizar(ventaId)).thenReturn(Optional.of(venta));
        when(detalleVentaRepository.findByVenta_Id(ventaId)).thenReturn(List.of(lineaEntera, lineaMedia, lineaServicio));
    }

    private void prepararAprobacion(Devolucion d, List<DetalleDevolucion> detalles) {
        when(devolucionRepository.findByIdParaActualizar(devId)).thenReturn(Optional.of(d));
        when(ventaRepository.findByIdParaActualizar(ventaId)).thenReturn(Optional.of(venta));
        KardexMovimiento salidaVenta = KardexMovimiento.builder().producto(plancha).almacen(almacen)
                .tipoMovimiento(TipoMovimiento.SALIDA).tipoReferencia(TipoReferencia.VENTA).build();
        when(kardexRepository.findByReferenciaIdAndTipoReferencia(ventaId, TipoReferencia.VENTA)).thenReturn(List.of(salidaVenta));
        when(detalleRepository.findByDevolucion_Id(devId)).thenReturn(detalles);
    }

    private User usuario(RolNombre rol) {
        return User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Prueba").email(EMAIL)
                .activo(true).sede(sede).rol(Rol.builder().nombre(rol).build()).build();
    }

    private DetalleDevolucionRequestDTO linea(DetalleVenta dv, String cantidad, EstadoProducto estado) {
        return new DetalleDevolucionRequestDTO(dv.getId(), new BigDecimal(cantidad), estado, null);
    }

    private Devolucion devolucion(EstadoDevolucion estado) {
        return Devolucion.builder().id(devId).codigo("DEV-2026-000001").venta(venta).sede(sede)
                .estado(estado).montoTotal(new BigDecimal("118.00")).motivo("x").build();
    }

    private DetalleDevolucion detalle(Devolucion d, DetalleVenta dv, String cant, String stock, EstadoProducto estado) {
        return DetalleDevolucion.builder().id(UUID.randomUUID()).devolucion(d).detalleVenta(dv).producto(plancha)
                .fraccion(dv.getFraccion()).cantidadDevuelta(new BigDecimal(cant)).cantidadStock(new BigDecimal(stock))
                .precioUnitarioRef(dv.getPrecioUnitarioConIgv()).estadoProducto(estado).build();
    }

    private TotalesFinancierosProjection totales(String total) {
        return new TotalesFinancierosProjection() {
            public BigDecimal getNeto() { return BigDecimal.ZERO; }
            public BigDecimal getIgv() { return BigDecimal.ZERO; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
            public Long getCantidad() { return 1L; }
        };
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtener_incluyeDetallesYReembolsoAsociado() {
        Devolucion d = devolucion(EstadoDevolucion.APROBADA);
        DetalleDevolucion det = DetalleDevolucion.builder().id(UUID.randomUUID()).devolucion(d).build();
        Reembolso r = Reembolso.builder().id(UUID.randomUUID()).build();
        when(devolucionRepository.findDetalleById(devId)).thenReturn(Optional.of(d));
        when(detalleRepository.findByDevolucion_Id(devId)).thenReturn(List.of(det));
        when(reembolsoRepository.findFirstByDevolucion_Id(devId)).thenReturn(Optional.of(r));

        service.obtener(devId);

        verify(detalleMapper).toResponseDto(det);
        verify(mapper).toResponseDto(eq(d), argThat(l -> l.size() == 1), eq(r));
    }

    @Test
    void obtener_sinReembolso_pasaNulo() {
        Devolucion d = devolucion(EstadoDevolucion.PENDIENTE);
        when(devolucionRepository.findDetalleById(devId)).thenReturn(Optional.of(d));
        when(detalleRepository.findByDevolucion_Id(devId)).thenReturn(List.of());
        when(reembolsoRepository.findFirstByDevolucion_Id(devId)).thenReturn(Optional.empty());

        service.obtener(devId);

        verify(mapper).toResponseDto(eq(d), eq(List.of()), isNull());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        when(devolucionRepository.findDetalleById(devId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(devId));
        verifyNoInteractions(detalleRepository);
    }

    @Test
    void buscar_sinFechas_usaRangoAbierto() {
        Pageable p = PageRequest.of(0, 10);
        when(devolucionRepository.search(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        service.buscar(sede.getId(), EstadoDevolucion.PENDIENTE, null, null, null, p);

        verify(devolucionRepository).search(sede.getId(), EstadoDevolucion.PENDIENTE, null,
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 1, 0, 0), p);
    }

    @Test
    void buscar_conFechas_hastaInclusivoYMapea() {
        Pageable p = PageRequest.of(0, 10);
        Devolucion d = devolucion(EstadoDevolucion.APROBADA);
        LocalDate desde = LocalDate.of(2026, 9, 1), hasta = LocalDate.of(2026, 9, 30);
        when(devolucionRepository.search(null, null, venta.getId(), desde.atStartOfDay(), LocalDate.of(2026, 10, 1).atStartOfDay(), p))
                .thenReturn(new PageImpl<>(List.of(d)));

        assertEquals(1, service.buscar(null, null, venta.getId(), desde, hasta, p).getTotalElements());
        verify(mapper).toListadoDto(d);
    }

    @Test
    void buscar_desdePosteriorAHasta_lanzaIllegalArgument() {
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null, hoy, hoy.minusDays(1), PageRequest.of(0, 10)));
        verifyNoInteractions(devolucionRepository);
    }
}
