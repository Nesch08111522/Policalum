package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.AjusteStockRequestDTO;
import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaStockRequestDTO;
import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.mapper.KardexConsultaMapper;
import com.policalum.erp.application.mapper.StockOperacionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.infrastructure.repository.projection.TotalPorTipoMovimientoProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StockOperacionServiceImplTest {

    @Mock KardexService kardexService;
    @Mock StockRepository stockRepository;
    @Mock ProductoRepository productoRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock AjusteStockRepository ajusteRepository;
    @Mock TransferenciaStockRepository transferenciaRepository;
    @Mock KardexRepository kardexRepository;
    @Mock UserRepository userRepository;
    @Mock CorrelativoService correlativoService;
    @Mock StockOperacionMapper mapper;
    @Spy KardexConsultaMapper consultaMapper = Mappers.getMapper(KardexConsultaMapper.class);
    @Mock AuditService auditService;
    @Mock ConfiguracionService configuracionService;
    @InjectMocks StockOperacionServiceImpl service;

    private static final String EMAIL = "user@policalum.com";
    User actual;
    Sede centro, norte;
    Almacen principal, tienda, almacenNorte;
    Producto plancha;   // costo S/ 80.00

    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.inventario()).thenReturn(ParametrosInventarioDTO.DEFAULTS);
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").build();
        principal = almacen("Principal", centro);
        tienda = almacen("Tienda", centro);
        almacenNorte = almacen("Norte", norte);
        plancha = Producto.builder().id(UUID.randomUUID()).nombre("Policarbonato 6mm").unidad("UNID").precioCompra(new BigDecimal("80.00")).build();
        actual = usuario(RolNombre.ROLE_ADMIN, centro);

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
        lenient().when(productoRepository.findById(plancha.getId())).thenReturn(Optional.of(plancha));
        for (Almacen a : List.of(principal, tienda, almacenNorte)) {
            lenient().when(almacenRepository.findById(a.getId())).thenReturn(Optional.of(a));
        }
        lenient().when(correlativoService.siguiente(anyString())).thenReturn(7L);
        lenient().when(ajusteRepository.save(any())).thenAnswer(i -> {
            AjusteStock a = i.getArgument(0);
            if (a.getId() == null) a.setId(UUID.randomUUID());
            return a;
        });
        lenient().when(transferenciaRepository.save(any())).thenAnswer(i -> {
            TransferenciaStock t = i.getArgument(0);
            if (t.getId() == null) t.setId(UUID.randomUUID());
            return t;
        });
        lenient().when(kardexRepository.getReferenceById(any())).thenAnswer(i -> KardexMovimiento.builder().id(i.getArgument(0)).build());
        lenient().when(mapper.toAjusteDto(any())).thenAnswer(i -> ajusteDto(i.getArgument(0)));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== AJUSTES =====================

    @Test
    void ajuste_positivo_registraEntradaEnKardexConReferenciaYAudita() {
        stockActual(principal, "10");
        UUID movId = UUID.randomUUID();
        when(kardexService.registrarMovimiento(any())).thenReturn(movimiento(movId, "13"));
        ArgumentCaptor<KardexMovimientoRequestDTO> captor = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);

        AjusteStockResponseDTO r = service.registrarAjuste(ajuste(principal, "3", "Conteo físico: sobrante", null));

        verify(kardexService).registrarMovimiento(captor.capture());
        KardexMovimientoRequestDTO k = captor.getValue();
        assertEquals(TipoReferencia.AJUSTE, k.tipoReferencia());
        assertEquals(TipoMovimiento.ENTRADA, k.tipoMovimiento());
        assertEquals(new BigDecimal("3"), k.cantidadMovida());
        assertEquals(r.id(), k.referenciaId());
        assertSame(actual, k.user());
        assertEquals("AJ-" + LocalDate.now().getYear() + "-000007", r.codigo());
        assertEquals(new BigDecimal("10"), r.stockAnterior());
        assertEquals(new BigDecimal("13"), r.stockResultante());
        assertEquals(new BigDecimal("240.00"), r.valor());
        assertEquals(movId, r.kardexMovimientoId());
        verify(auditService).registrar(eq("stock_ajustes"), eq(r.id()), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void ajuste_negativoQueDejariaStockNegativo_lanzaIllegalArgumentSinMoverKardex() {
        stockActual(principal, "2");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAjuste(ajuste(principal, "-3", "Merma por rotura", null)));
        assertTrue(ex.getMessage().contains("negativo"));
        verifyNoInteractions(kardexService);
        verify(ajusteRepository, never()).save(any());
    }

    @Test
    void ajuste_negativoHastaCero_seAceptaComoSalida() {
        stockActual(principal, "2");
        when(kardexService.registrarMovimiento(any())).thenReturn(movimiento(UUID.randomUUID(), "0"));

        AjusteStockResponseDTO r = service.registrarAjuste(ajuste(principal, "-2", "Merma por rotura", null));

        assertEquals(TipoMovimiento.SALIDA, r.tipoMovimiento());
        verify(kardexService).registrarMovimiento(argThat(k -> k.tipoMovimiento() == TipoMovimiento.SALIDA
                && k.cantidadMovida().compareTo(new BigDecimal("2")) == 0));
    }

    @Test
    void ajuste_cantidadCero_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarAjuste(ajuste(principal, "0", "Conteo", null)));
        verifyNoInteractions(stockRepository, kardexService);
    }

    @Test
    void ajuste_mayorA500_exigeJustificacionDe30Caracteres() {
        stockActual(principal, "0");
        // 7 × 80 = 560 > 500
        assertThrows(IllegalArgumentException.class, () -> service.registrarAjuste(ajuste(principal, "7", "Sobrante", null)));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAjuste(ajuste(principal, "7", "Sobrante", "  corta  ")));
        assertTrue(ex.getMessage().contains("560.00"));
        verifyNoInteractions(kardexService);

        when(kardexService.registrarMovimiento(any())).thenReturn(movimiento(UUID.randomUUID(), "7"));
        AjusteStockResponseDTO ok = service.registrarAjuste(ajuste(principal, "7", "Sobrante",
                "Llegó mercadería de la compra OC-2026-0012 sin registrar en el sistema"));
        assertNotNull(ok.justificacion());
    }

    @Test
    void ajuste_usaUmbralConfigurado() {
        // Módulo 14: umbral S/ 100 y 10 caracteres → 2 × 80 = 160 ya exige justificación
        when(configuracionService.inventario()).thenReturn(new ParametrosInventarioDTO(new BigDecimal("100.00"), 10));
        stockActual(principal, "0");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.registrarAjuste(ajuste(principal, "2", "Sobrante", "corta")));
        assertTrue(ex.getMessage().contains("100.00"));
        assertTrue(ex.getMessage().contains("10 caracteres"));
        verifyNoInteractions(kardexService);
    }

    @Test
    void ajuste_exactamente500_noExigeJustificacion() {
        Producto barato = Producto.builder().id(UUID.randomUUID()).nombre("Perfil").precioCompra(new BigDecimal("100.00")).build();
        when(productoRepository.findById(barato.getId())).thenReturn(Optional.of(barato));
        when(stockRepository.findParaActualizar(principal.getId(), barato.getId())).thenReturn(Optional.empty());
        when(kardexService.registrarMovimiento(any())).thenReturn(movimiento(UUID.randomUUID(), "5"));

        AjusteStockResponseDTO r = service.registrarAjuste(new AjusteStockRequestDTO(barato.getId(), principal.getId(),
                new BigDecimal("5"), "Inventario inicial", null));

        assertEquals(new BigDecimal("500.00"), r.valor());
        assertEquals(BigDecimal.ZERO, r.stockAnterior());
    }

    @Test
    void ajuste_multifuncional_soloEnSuSedeYSinVerCostos() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        assertThrows(AccessDeniedException.class, () -> service.registrarAjuste(ajuste(almacenNorte, "1", "Sobrante", null)));
        verifyNoInteractions(stockRepository, kardexService);

        stockActual(principal, "1");
        when(kardexService.registrarMovimiento(any())).thenReturn(movimiento(UUID.randomUUID(), "2"));
        AjusteStockResponseDTO r = service.registrarAjuste(ajuste(principal, "1", "Sobrante", null));
        assertNull(r.valor());
        assertNull(r.costoUnitario());
        assertEquals(new BigDecimal("2"), r.stockResultante());
    }

    @Test
    void ajuste_almacenInactivo_lanzaIllegalState() {
        principal.setActivo(false);
        assertThrows(IllegalStateException.class, () -> service.registrarAjuste(ajuste(principal, "1", "Sobrante", null)));
    }

    // ===================== TRANSFERENCIAS =====================

    @Test
    void transferir_bloqueaEnOrdenFijoYAgrupaSalidaYEntrada() {
        stockActual(principal, "10");
        when(stockRepository.findParaActualizar(almacenNorte.getId(), plancha.getId())).thenReturn(Optional.empty());
        UUID salida = UUID.randomUUID(), entrada = UUID.randomUUID();
        when(kardexService.registrarTransferencia(any(), any())).thenReturn(List.of(movimiento(salida, "6"), movimiento(entrada, "4")));
        when(transferenciaRepository.findDetalleById(any())).thenAnswer(i -> Optional.of(TransferenciaStock.builder().id(i.getArgument(0)).build()));
        ArgumentCaptor<TransferenciaStock> captor = ArgumentCaptor.forClass(TransferenciaStock.class);
        ArgumentCaptor<TransferenciaRequestDTO> req = ArgumentCaptor.forClass(TransferenciaRequestDTO.class);

        service.transferir(new TransferenciaStockRequestDTO(plancha.getId(), principal.getId(), almacenNorte.getId(),
                new BigDecimal("4"), "Reposición sede Norte", "GR-001"));

        List<UUID> orden = StockOperacionServiceImpl.ordenados(principal.getId(), almacenNorte.getId());
        InOrder enOrden = inOrder(stockRepository);
        enOrden.verify(stockRepository).findParaActualizar(orden.get(0), plancha.getId());
        enOrden.verify(stockRepository).findParaActualizar(orden.get(1), plancha.getId());
        verify(transferenciaRepository, times(2)).save(captor.capture());
        TransferenciaStock t = captor.getValue();
        verify(kardexService).registrarTransferencia(req.capture(), eq(t.getId()));
        assertTrue(req.getValue().motivo().startsWith("TRF-" + LocalDate.now().getYear() + "-000007"));
        assertTrue(t.getEntreSedes());
        assertEquals(salida, t.getMovimientoSalida().getId());
        assertEquals(entrada, t.getMovimientoEntrada().getId());
        verify(auditService).registrar(eq("stock_transferencias"), eq(t.getId()), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void transferir_mismoAlmacen_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.transferir(new TransferenciaStockRequestDTO(
                plancha.getId(), principal.getId(), principal.getId(), BigDecimal.ONE, null, null)));
        verifyNoInteractions(kardexService, stockRepository);
    }

    @Test
    void transferir_stockInsuficienteEnOrigen_lanzaIllegalArgumentSinMoverKardex() {
        stockActual(principal, "3");
        lenient().when(stockRepository.findParaActualizar(tienda.getId(), plancha.getId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.transferir(
                new TransferenciaStockRequestDTO(plancha.getId(), principal.getId(), tienda.getId(), new BigDecimal("5"), null, null)));
        assertTrue(ex.getMessage().contains("Stock insuficiente en Principal"));
        verifyNoInteractions(kardexService);
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void transferir_multifuncionalEntreSedes_lanzaAccessDenied() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        assertThrows(AccessDeniedException.class, () -> service.transferir(new TransferenciaStockRequestDTO(
                plancha.getId(), principal.getId(), almacenNorte.getId(), BigDecimal.ONE, null, null)));
        verifyNoInteractions(kardexService, stockRepository);
    }

    @Test
    void obtenerTransferencia_inexistenteOAjenaParaMultifuncional() {
        UUID id = UUID.randomUUID();
        when(transferenciaRepository.findDetalleById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerTransferencia(id));

        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, norte);
        UUID otra = UUID.randomUUID();
        when(transferenciaRepository.findDetalleById(otra)).thenReturn(Optional.of(TransferenciaStock.builder().id(otra)
                .almacenOrigen(principal).almacenDestino(tienda).build()));
        assertThrows(AccessDeniedException.class, () -> service.obtenerTransferencia(otra));
    }

    // ===================== CONSULTAS =====================

    @Test
    void consultas_multifuncionalQuedaEnSuSede() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        when(ajusteRepository.buscar(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());
        when(stockRepository.consolidado(any(), any(), any())).thenReturn(Page.empty());

        service.buscarAjustes(null, null, null, null, null, null, null, PageRequest.of(0, 20));
        service.consolidado(null, "  PC-6 ", PageRequest.of(0, 20));

        verify(ajusteRepository).buscar(isNull(), isNull(), eq(centro.getId()), isNull(), isNull(),
                eq(LocalDateTime.of(2000, 1, 1, 0, 0)), eq(LocalDateTime.of(2100, 1, 2, 0, 0)), eq(PageRequest.of(0, 20)));
        verify(stockRepository).consolidado(centro.getId(), "%pc-6%", PageRequest.of(0, 20));
        assertThrows(AccessDeniedException.class, () -> service.bajoStock(norte.getId(), null, PageRequest.of(0, 20)));
    }

    @Test
    void dashboard_cuentaMovimientosDelDiaYValorSoloParaAdmin() {
        when(kardexRepository.conteoPorTipo(any(), any(), any())).thenReturn(List.of(tipo(TipoMovimiento.ENTRADA, 4), tipo(TipoMovimiento.SALIDA, 6)));
        when(ajusteRepository.resumen(any(), any(), any())).thenReturn(resumen("150.5", 2));
        when(stockRepository.valorizadoPorSede(null)).thenReturn(List.of(monto("1000.00"), monto("250.25")));
        when(stockRepository.buscarSaldos(any(), any(), any(), any(), eq(true), eq(PageRequest.of(0, 5)))).thenReturn(Page.empty());
        when(stockRepository.contarProductosConStock(null)).thenReturn(30L);
        when(stockRepository.contarBajoMinimo(null)).thenReturn(4L);
        when(stockRepository.contarAgotados(null)).thenReturn(1L);
        when(transferenciaRepository.contar(any(), any(), any())).thenReturn(3L);

        StockDashboardDTO d = service.dashboard(null);

        assertEquals(10, d.movimientosHoy());
        assertEquals(4, d.entradasHoy());
        assertEquals(new BigDecimal("1250.25"), d.valorInventario());
        assertEquals(new BigDecimal("150.50"), d.valorAjustesHoy());
        assertEquals(2, d.ajustesHoy());
        assertEquals(3, d.transferenciasHoy());
        assertEquals(LocalDate.now(), d.fecha());

        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        StockDashboardDTO m = service.dashboard(null);
        assertNull(m.valorInventario());
        assertNull(m.valorAjustesHoy());
        assertEquals(centro.getId(), m.sedeId());
    }

    // ===================== HELPERS =====================

    private void stockActual(Almacen a, String cantidad) {
        when(stockRepository.findParaActualizar(a.getId(), plancha.getId()))
                .thenReturn(Optional.of(Stock.builder().producto(plancha).almacen(a).cantidad(new BigDecimal(cantidad)).build()));
    }

    private AjusteStockRequestDTO ajuste(Almacen a, String cantidad, String motivo, String justificacion) {
        return new AjusteStockRequestDTO(plancha.getId(), a.getId(), new BigDecimal(cantidad), motivo, justificacion);
    }

    private KardexMovimientoResponseDTO movimiento(UUID id, String resultante) {
        return new KardexMovimientoResponseDTO(id, null, null, null, null, null, null, new BigDecimal(resultante), null, null);
    }

    private Almacen almacen(String nombre, Sede sede) {
        return Almacen.builder().id(UUID.randomUUID()).nombre(nombre).sede(sede).activo(true).build();
    }

    private User usuario(RolNombre rol, Sede sede) {
        return User.builder().id(UUID.randomUUID()).nombre("Ana").apellido("Ruiz").email(EMAIL).sede(sede)
                .rol(Rol.builder().nombre(rol).build()).build();
    }

    private AjusteStockResponseDTO ajusteDto(AjusteStock a) {
        return new AjusteStockResponseDTO(a.getId(), a.getCodigo(), a.getProducto().getId(), null, null, null, a.getAlmacen().getId(),
                null, null, null, a.getTipoMovimiento(), a.getCantidad(), a.getStockAnterior(), a.getStockResultante(),
                a.getCostoUnitario(), a.getValor(), a.getMotivo(), a.getJustificacion(),
                a.getKardexMovimiento() != null ? a.getKardexMovimiento().getId() : null, null, null, null);
    }

    private TotalPorTipoMovimientoProjection tipo(TipoMovimiento t, long n) {
        return new TotalPorTipoMovimientoProjection() {
            public TipoMovimiento getTipo() { return t; }
            public BigDecimal getCantidad() { return BigDecimal.TEN; }
            public Long getMovimientos() { return n; }
        };
    }

    private MontoCantidadProjection resumen(String total, long n) {
        return new MontoCantidadProjection() {
            public BigDecimal getTotal() { return new BigDecimal(total); }
            public Long getCantidad() { return n; }
        };
    }

    private MontoPorSedeProjection monto(String total) {
        return new MontoPorSedeProjection() {
            public UUID getSedeId() { return centro.getId(); }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void buscarTransferencias_admin_sinFechas_usaRangoAbiertoYQuitaElOrden() {
        when(transferenciaRepository.buscar(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(Page.empty());

        service.buscarTransferencias(plancha.getId(), null, null, true, null, null, PageRequest.of(2, 15, Sort.by("fecha")));

        verify(transferenciaRepository).buscar(plancha.getId(), null, null, true,
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 2, 0, 0), PageRequest.of(2, 15));
    }

    @Test
    void buscarTransferencias_conFechas_hastaInclusivo() {
        when(transferenciaRepository.buscar(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(Page.empty());
        LocalDate d = LocalDate.of(2026, 9, 1), h = LocalDate.of(2026, 9, 30);

        service.buscarTransferencias(null, null, centro.getId(), false, d, h, Pageable.unpaged());

        verify(transferenciaRepository).buscar(null, null, centro.getId(), false,
                d.atStartOfDay(), LocalDate.of(2026, 10, 1).atStartOfDay(), Pageable.unpaged());
    }

    @Test
    void buscarTransferencias_noAdminSinSede_seRestringeASuPropiaSede() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, norte);
        when(transferenciaRepository.buscar(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(Page.empty());

        service.buscarTransferencias(null, null, null, false, null, null, PageRequest.of(0, 10));

        verify(transferenciaRepository).buscar(isNull(), isNull(), eq(norte.getId()), eq(false), any(), any(), any());
    }

    @Test
    void buscarTransferencias_noAdminOtraSede_lanzaAccessDenied() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, norte);

        assertThrows(AccessDeniedException.class, () -> service.buscarTransferencias(null, null, centro.getId(), false, null, null, PageRequest.of(0, 10)));
        verifyNoInteractions(transferenciaRepository);
    }

    @Test
    void buscarTransferencias_desdePosteriorAHasta_lanzaIllegalArgument() {
        LocalDate hoy = LocalDate.now();

        assertThrows(IllegalArgumentException.class, () -> service.buscarTransferencias(null, null, null, false, hoy, hoy.minusDays(1), PageRequest.of(0, 10)));
    }

    @Test
    void buscarTransferencias_mapeaCadaResultado() {
        TransferenciaStock t = TransferenciaStock.builder().id(UUID.randomUUID()).build();
        when(transferenciaRepository.buscar(any(), any(), any(), anyBoolean(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(t)));

        assertEquals(1, service.buscarTransferencias(null, null, null, false, null, null, PageRequest.of(0, 10)).getTotalElements());
        verify(mapper).toTransferenciaDto(t);
    }
}
