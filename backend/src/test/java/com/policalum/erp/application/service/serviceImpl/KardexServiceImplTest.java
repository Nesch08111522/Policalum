package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.TransferenciaRequestDTO;
import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.mapper.KardexConsultaMapper;
import com.policalum.erp.application.mapper.KardexMovimientoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import com.policalum.erp.infrastructure.repository.projection.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
class KardexServiceImplTest {

    @Mock KardexRepository kardexRepository;
    @Mock StockRepository stockRepository;
    @Mock ProductoRepository productoRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock UserRepository userRepository;
    @Mock KardexMovimientoMapper kardexMapper;
    @Spy KardexConsultaMapper consultaMapper = Mappers.getMapper(KardexConsultaMapper.class);
    @Mock AuditService auditService;
    @Mock ConfiguracionService configuracionService;
    @InjectMocks KardexServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;
    Producto plancha;
    Almacen principal;

    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.reportes()).thenReturn(ParametrosReportesDTO.DEFAULTS);
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL)
                .rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        plancha = Producto.builder().id(UUID.randomUUID()).nombre("Policarbonato 6mm").codigoSku("PC-6").unidad("UNID")
                .precioCompra(new BigDecimal("80.00")).build();
        principal = Almacen.builder().id(UUID.randomUUID()).nombre("Principal")
                .sede(Sede.builder().id(UUID.randomUUID()).nombre("Centro").build()).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));
        lenient().when(productoRepository.findById(plancha.getId())).thenReturn(Optional.of(plancha));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ===================== REGISTRO (concurrencia) =====================

    @Test
    void registrarMovimiento_bloqueaLaFilaDeStockYRechazaSalidaSinStock() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        Stock stock = Stock.builder().producto(plancha).almacen(principal).cantidad(new BigDecimal("3")).build();
        when(stockRepository.findParaActualizar(principal.getId(), plancha.getId())).thenReturn(Optional.of(stock));

        assertThrows(IllegalArgumentException.class, () -> service.registrarMovimiento(new KardexMovimientoRequestDTO(
                plancha, principal, null, TipoReferencia.VENTA, TipoMovimiento.SALIDA, new BigDecimal("5"), BigDecimal.ZERO, admin, null)));

        verify(stockRepository).findParaActualizar(principal.getId(), plancha.getId());
        verify(stockRepository, never()).findByAlmacen_IdAndProducto_Id(any(), any());
        verify(kardexRepository, never()).save(any());
        assertEquals(new BigDecimal("3"), stock.getCantidad());
    }

    // ===================== MOVIMIENTOS =====================

    @Test
    void buscarMovimientos_sinFechas_incluyeSinFechaYIgnoraOrdenDelCliente() {
        when(kardexRepository.buscar(any(), any(), any(), any(), any(), anyBoolean(), any())).thenReturn(Page.empty());

        service.buscarMovimientos(plancha.getId(), null, TipoReferencia.MERMA, null, null, PageRequest.of(1, 10, Sort.by("id")));

        verify(kardexRepository).buscar(plancha.getId(), null, TipoReferencia.MERMA,
                LocalDateTime.of(2000, 1, 1, 0, 0), LocalDateTime.of(2100, 1, 2, 0, 0), true, PageRequest.of(1, 10));
    }

    @Test
    void buscarMovimientos_conFechas_rangoSemiabiertoYCalculaStockAnterior() {
        KardexMovimiento salida = mov(TipoMovimiento.SALIDA, TipoReferencia.VENTA, "2", "8");
        when(kardexRepository.buscar(isNull(), isNull(), isNull(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(salida)));

        Page<KardexMovimientoDetalleDTO> r = service.buscarMovimientos(null, null, null,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 8), PageRequest.of(0, 20));

        verify(kardexRepository).buscar(null, null, null, LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 9, 0, 0), false, PageRequest.of(0, 20));
        KardexMovimientoDetalleDTO d = r.getContent().get(0);
        assertEquals(new BigDecimal("10"), d.stockAnterior());
        assertEquals("Centro", d.sedeNombre());
        assertEquals("Miguel Dueño", d.userNombre());
    }

    @Test
    void buscarMovimientos_desdePosteriorAHasta_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.buscarMovimientos(null, null, null,
                LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 1), PageRequest.of(0, 20)));
        verifyNoInteractions(kardexRepository);
    }

    @Test
    void obtenerMovimiento_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(kardexRepository.findDetalleById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.obtenerMovimiento(id));
    }

    // ===================== SALDOS =====================

    @Test
    void saldos_normalizaBusquedaYMarcaBajoMinimo() {
        when(stockRepository.buscarSaldos(isNull(), isNull(), isNull(), eq("%pc-6%"), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(saldo(principal, "4", "5"))));

        Page<SaldoKardexDTO> r = service.saldos(null, null, "  PC-6 ", true, PageRequest.of(0, 20));

        assertTrue(r.getContent().get(0).bajoMinimo());
        assertNull(KardexServiceImpl.patronBusqueda("   "));
    }

    @Test
    void saldosPorProducto_sumaTodosLosAlmacenes() {
        Almacen tienda = Almacen.builder().id(UUID.randomUUID()).nombre("Tienda").build();
        when(stockRepository.buscarSaldos(eq(plancha.getId()), isNull(), isNull(), isNull(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(saldo(principal, "10.5", "0"), saldo(tienda, "2", "5"))));

        SaldosProductoDTO r = service.saldosPorProducto(plancha.getId());

        assertEquals(new BigDecimal("12.5"), r.stockTotal());
        assertEquals(2, r.almacenes().size());
        assertFalse(r.almacenes().get(0).bajoMinimo());   // mínimo 0 = sin alerta
        assertTrue(r.almacenes().get(1).bajoMinimo());
    }

    @Test
    void saldosPorProductoOAlmacenInexistente_lanzaNotFound() {
        UUID otro = UUID.randomUUID();
        when(productoRepository.findById(otro)).thenReturn(Optional.empty());
        when(almacenRepository.existsById(otro)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.saldosPorProducto(otro));
        assertThrows(ResourceNotFoundException.class, () -> service.saldosPorAlmacen(otro, null, false, PageRequest.of(0, 20)));
        verifyNoInteractions(stockRepository);
    }

    // ===================== VALORIZACIÓN =====================

    @Test
    void valorizacion_cantidadPorCostoPromedioOrdenadaConPorcentajes() {
        when(stockRepository.valorizacion(null, null)).thenReturn(List.of(
                valorizacion("Tornillos", "100", "0.50", 2),       // 50.00
                valorizacion("Policarbonato 6mm", "10", "80.00", 1), // 800.00
                valorizacion("Muestra", "3", "0", 1)));              // sin costo

        ValorizacionKardexDTO r = service.valorizacion(null);

        assertEquals(new BigDecimal("850.00"), r.valorTotal());
        assertEquals(3, r.productos());
        assertEquals(1, r.productosSinCosto());
        assertEquals("Policarbonato 6mm", r.detalle().get(0).productoNombre());
        assertEquals(new BigDecimal("94.12"), r.detalle().get(0).porcentajeDelTotal());
        assertEquals(new BigDecimal("50.00"), r.detalle().get(1).valorTotal());
    }

    @Test
    void valorizacionAlmacen_inexistenteONoAdmin() {
        UUID otro = UUID.randomUUID();
        when(almacenRepository.findById(otro)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.valorizacionAlmacen(otro));

        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());
        assertThrows(AccessDeniedException.class, () -> service.valorizacion(null));
        assertThrows(AccessDeniedException.class, () -> service.mermas(null, null, null, null, PageRequest.of(0, 20)));
        assertThrows(AccessDeniedException.class, () -> service.exportarCsv(null, null, null, null, null));
        verifyNoInteractions(stockRepository, kardexRepository, auditService);
    }

    // ===================== MERMAS =====================

    @Test
    void mermas_resumenPorProductoYDetalleValorizado() {
        when(kardexRepository.mermasPorProducto(isNull(), eq(principal.getId()), any(), any(), eq(false))).thenReturn(List.of(
                merma("Policarbonato 6mm", 2, "1.5", "120.004"), merma("Tornillos", 1, "10", "5")));
        when(kardexRepository.buscarMermas(isNull(), eq(principal.getId()), any(), any(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(mov(TipoMovimiento.SALIDA, TipoReferencia.MERMA, "1.5", "0")), PageRequest.of(0, 1), 3));

        ReporteMermasDTO r = service.mermas(null, principal.getId(), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), PageRequest.of(0, 20));

        assertEquals(3, r.movimientos());
        assertEquals(new BigDecimal("125.00"), r.valorTotal());
        assertEquals(new BigDecimal("120.00"), r.porProducto().get(0).valor());
        assertEquals(new BigDecimal("120.00"), r.detalle().get(0).valor());   // 1.5 × 80.00
        assertEquals(3, r.totalElementos());
    }

    // ===================== HISTORIAL =====================

    @Test
    void historial_saldosTotalesYMovimientosConStockResultante() {
        when(stockRepository.buscarSaldos(eq(plancha.getId()), isNull(), isNull(), isNull(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(saldo(principal, "8", "0"))));
        when(kardexRepository.totalesPorTipo(eq(plancha.getId()), isNull(), any(), any(), eq(true))).thenReturn(List.of(
                totalTipo(TipoMovimiento.ENTRADA, "20"), totalTipo(TipoMovimiento.SALIDA, "12")));
        when(kardexRepository.buscar(eq(plancha.getId()), isNull(), isNull(), any(), any(), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(mov(TipoMovimiento.ENTRADA, TipoReferencia.COMPRA, "5", "8")), PageRequest.of(0, 20), 1));

        HistorialProductoDTO r = service.historialProducto(plancha.getId(), null, null, null, PageRequest.of(0, 20));

        assertEquals(new BigDecimal("8"), r.stockActual());
        assertEquals(new BigDecimal("20"), r.totalEntradas());
        assertEquals(new BigDecimal("12"), r.totalSalidas());
        assertEquals(new BigDecimal("8"), r.movimientos().get(0).stockResultante());
        assertEquals(new BigDecimal("3"), r.movimientos().get(0).stockAnterior());
        assertEquals("PC-6", r.codigoSku());
    }

    // ===================== EXPORTAR =====================

    @Test
    void exportar_csvConBomNumerosPlanosYAuditado() throws Exception {
        KardexMovimiento m = mov(TipoMovimiento.SALIDA, TipoReferencia.MERMA, "1.5000", "8.5000");
        m.setObservacion("=CMD|' /C calc'!A0");
        m.setCreatedAt(LocalDateTime.of(2026, 10, 8, 10, 15, 0));
        when(kardexRepository.buscarParaExportar(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(m), PageRequest.of(0, 1000), 1));

        ArchivoDescargaDTO archivo = service.exportarCsv(plancha.getId(), null, null, null, null);

        String[] lineas = new String(archivo.resource().getContentAsByteArray(), StandardCharsets.UTF_8).split("\r\n");
        assertTrue(lineas[0].startsWith("﻿fecha,tipo_movimiento"));
        assertTrue(lineas[1].startsWith("2026-10-08 10:15:00,SALIDA,MERMA,"));
        assertTrue(lineas[1].contains(",Policarbonato 6mm,PC-6,UNID,Principal,Centro,1.5,10,8.5,Miguel Dueño,"));
        assertTrue(lineas[1].endsWith(",'=CMD|' /C calc'!A0"), "la observación no debe quedar como fórmula");
        assertTrue(archivo.nombreOriginal().matches("kardex_\\d{8}_\\d{4}\\.csv"));
        verify(auditService).registrar(eq("kardex_movimientos"), eq(admin.getId()), eq(AccionAudit.ACCESS),
                argThat(d -> Long.valueOf(1).equals(d.get("filas"))));
    }

    @Test
    void exportar_superaElMaximo_lanzaIllegalArgumentSinAuditar() {
        when(kardexRepository.buscarParaExportar(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1000), ParametrosReportesDTO.DEFAULTS.maxFilasExportacion() + 1L));

        assertThrows(IllegalArgumentException.class, () -> service.exportarCsv(null, null, null, null, null));
        verifyNoInteractions(auditService);
    }

    // ===================== HELPERS =====================

    private KardexMovimiento mov(TipoMovimiento tipo, TipoReferencia ref, String cantidad, String resultante) {
        return KardexMovimiento.builder().id(UUID.randomUUID()).producto(plancha).almacen(principal).user(admin)
                .tipoMovimiento(tipo).tipoReferencia(ref).cantidadMovida(new BigDecimal(cantidad))
                .stockResultante(new BigDecimal(resultante)).build();
    }

    private SaldoProductoAlmacenProjection saldo(Almacen a, String cantidad, String minimo) {
        return new SaldoProductoAlmacenProjection() {
            public UUID getProductoId() { return plancha.getId(); }
            public String getProductoNombre() { return plancha.getNombre(); }
            public String getCodigoSku() { return plancha.getCodigoSku(); }
            public String getUnidad() { return plancha.getUnidad(); }
            public UUID getAlmacenId() { return a.getId(); }
            public String getAlmacenNombre() { return a.getNombre(); }
            public UUID getSedeId() { return null; }
            public String getSedeNombre() { return null; }
            public BigDecimal getCantidad() { return new BigDecimal(cantidad); }
            public BigDecimal getStockMinimo() { return new BigDecimal(minimo); }
            public LocalDateTime getUltimoMovimiento() { return null; }
        };
    }

    private ValorizacionProductoProjection valorizacion(String nombre, String cantidad, String costo, long almacenes) {
        return new ValorizacionProductoProjection() {
            public UUID getProductoId() { return UUID.randomUUID(); }
            public String getProductoNombre() { return nombre; }
            public String getCodigoSku() { return null; }
            public String getUnidad() { return "UNID"; }
            public BigDecimal getCostoUnitario() { return new BigDecimal(costo); }
            public BigDecimal getCantidad() { return new BigDecimal(cantidad); }
            public Long getAlmacenes() { return almacenes; }
        };
    }

    private MermaProjection merma(String nombre, long movimientos, String cantidad, String valor) {
        return new MermaProjection() {
            public UUID getProductoId() { return UUID.randomUUID(); }
            public String getProductoNombre() { return nombre; }
            public String getCodigoSku() { return null; }
            public String getUnidad() { return "UNID"; }
            public Long getMovimientos() { return movimientos; }
            public BigDecimal getCantidad() { return new BigDecimal(cantidad); }
            public BigDecimal getValor() { return new BigDecimal(valor); }
        };
    }

    private TotalPorTipoMovimientoProjection totalTipo(TipoMovimiento tipo, String cantidad) {
        return new TotalPorTipoMovimientoProjection() {
            public TipoMovimiento getTipo() { return tipo; }
            public BigDecimal getCantidad() { return new BigDecimal(cantidad); }
            public Long getMovimientos() { return 1L; }
        };
    }

    // ===================== REGISTRO: entradas, salidas y transferencias (auditoría de cobertura) =====================

    private Almacen almacen(String nombre) {
        Almacen a = Almacen.builder().id(UUID.randomUUID()).nombre(nombre).sede(principal.getSede()).build();
        lenient().when(almacenRepository.findById(a.getId())).thenReturn(Optional.of(a));
        return a;
    }

    private Stock stockEn(Almacen a, String cantidad) {
        Stock s = Stock.builder().producto(plancha).almacen(a).cantidad(new BigDecimal(cantidad)).build();
        lenient().when(stockRepository.findParaActualizar(a.getId(), plancha.getId())).thenReturn(Optional.of(s));
        return s;
    }

    private KardexMovimiento movimientoGuardado() {
        ArgumentCaptor<KardexMovimiento> c = ArgumentCaptor.forClass(KardexMovimiento.class);
        verify(kardexRepository).save(c.capture());
        return c.getValue();
    }

    @Test
    void registrarMovimiento_entrada_sumaStockYGuardaStockResultante() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        Stock stock = stockEn(principal, "10");

        service.registrarMovimiento(new KardexMovimientoRequestDTO(plancha, principal, null, TipoReferencia.COMPRA,
                TipoMovimiento.ENTRADA, new BigDecimal("4.5"), BigDecimal.ZERO, admin, "OC-1"));

        assertEquals(new BigDecimal("14.5"), stock.getCantidad());
        verify(stockRepository).save(stock);
        KardexMovimiento m = movimientoGuardado();
        assertEquals(new BigDecimal("14.5"), m.getStockResultante());
        assertEquals(TipoReferencia.COMPRA, m.getTipoReferencia());
        assertSame(admin, m.getUser());
    }

    @Test
    void registrarMovimiento_salidaExactaAlStock_dejaCero() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        Stock stock = stockEn(principal, "5");

        service.registrarMovimiento(new KardexMovimientoRequestDTO(plancha, principal, null, TipoReferencia.VENTA,
                TipoMovimiento.SALIDA, new BigDecimal("5"), BigDecimal.ZERO, admin, null));

        assertEquals(0, stock.getCantidad().signum());
        assertEquals(0, movimientoGuardado().getStockResultante().signum());
    }

    @Test
    void registrarMovimiento_sinFilaDeStock_creaStockInicialEnCero() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        when(stockRepository.findParaActualizar(principal.getId(), plancha.getId())).thenReturn(Optional.empty());

        service.registrarMovimiento(new KardexMovimientoRequestDTO(plancha, principal, null, TipoReferencia.COMPRA,
                TipoMovimiento.ENTRADA, new BigDecimal("3"), BigDecimal.ZERO, admin, null));

        ArgumentCaptor<Stock> c = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(c.capture());
        assertEquals(new BigDecimal("3"), c.getValue().getCantidad());
        assertSame(plancha, c.getValue().getProducto());
        assertSame(principal, c.getValue().getAlmacen());
    }

    @Test
    void registrarMovimiento_salidaSinFilaDeStock_lanzaStockInsuficiente() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        when(stockRepository.findParaActualizar(principal.getId(), plancha.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.registrarMovimiento(new KardexMovimientoRequestDTO(
                plancha, principal, null, TipoReferencia.VENTA, TipoMovimiento.SALIDA, BigDecimal.ONE, BigDecimal.ZERO, admin, null)));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void registrarMovimiento_sinUsuarioEnDto_usaElAutenticadoYTipoReferenciaPorDefectoAjuste() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        stockEn(principal, "1");

        service.registrarMovimiento(new KardexMovimientoRequestDTO(plancha, principal, null, null,
                TipoMovimiento.ENTRADA, BigDecimal.ONE, BigDecimal.ZERO, null, null));

        KardexMovimiento m = movimientoGuardado();
        assertSame(admin, m.getUser());
        assertEquals(TipoReferencia.AJUSTE, m.getTipoReferencia());
    }

    @Test
    void registrarMovimiento_sinUsuarioNiAutenticacion_lanzaIllegalArgument() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.of(principal));
        stockEn(principal, "1");
        SecurityContextHolder.clearContext();

        assertThrows(IllegalArgumentException.class, () -> service.registrarMovimiento(new KardexMovimientoRequestDTO(
                plancha, principal, null, null, TipoMovimiento.ENTRADA, BigDecimal.ONE, BigDecimal.ZERO, null, null)));
        verify(kardexRepository, never()).save(any());
    }

    @Test
    void registrarMovimiento_almacenInexistente_lanzaNotFound() {
        when(almacenRepository.findById(principal.getId())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrarMovimiento(new KardexMovimientoRequestDTO(
                plancha, principal, null, null, TipoMovimiento.ENTRADA, BigDecimal.ONE, BigDecimal.ZERO, admin, null)));
        verifyNoInteractions(stockRepository);
    }

    @Test
    void registrarTransferencia_salidaDeOrigenYEntradaEnDestinoConLockEnAmbos() {
        Almacen origen = almacen("Principal");
        Almacen destino = almacen("Sucursal Norte");
        Stock sOrigen = stockEn(origen, "10");
        Stock sDestino = stockEn(destino, "2");
        UUID ref = UUID.randomUUID();
        when(kardexMapper.toResponseDto(any())).thenAnswer(i -> mock(KardexMovimientoResponseDTO.class));

        List<KardexMovimientoResponseDTO> r = service.registrarTransferencia(
                new TransferenciaRequestDTO(plancha.getId(), origen.getId(), destino.getId(), new BigDecimal("4"), "reposición", null), ref);

        assertEquals(2, r.size());
        assertEquals(new BigDecimal("6"), sOrigen.getCantidad());
        assertEquals(new BigDecimal("6"), sDestino.getCantidad());
        verify(stockRepository).findParaActualizar(origen.getId(), plancha.getId());
        verify(stockRepository).findParaActualizar(destino.getId(), plancha.getId());

        ArgumentCaptor<KardexMovimiento> c = ArgumentCaptor.forClass(KardexMovimiento.class);
        verify(kardexRepository, times(2)).save(c.capture());
        KardexMovimiento salida = c.getAllValues().get(0), entrada = c.getAllValues().get(1);
        assertEquals(TipoMovimiento.SALIDA, salida.getTipoMovimiento());
        assertSame(origen, salida.getAlmacen());
        assertEquals("Transferencia hacia Sucursal Norte - reposición", salida.getObservacion());
        assertEquals(TipoMovimiento.ENTRADA, entrada.getTipoMovimiento());
        assertSame(destino, entrada.getAlmacen());
        assertEquals("Transferencia desde Principal - reposición", entrada.getObservacion());
        assertEquals(TipoReferencia.TRANSFERENCIA, entrada.getTipoReferencia());
        assertEquals(ref, salida.getReferenciaId());
        assertEquals(ref, entrada.getReferenciaId());
    }

    @Test
    void registrarTransferencia_sinMotivoNiReferencia_observacionSinSufijo() {
        Almacen origen = almacen("A");
        Almacen destino = almacen("B");
        stockEn(origen, "1");
        stockEn(destino, "0");

        when(kardexMapper.toResponseDto(any())).thenAnswer(i -> mock(KardexMovimientoResponseDTO.class));

        service.registrarTransferencia(new TransferenciaRequestDTO(plancha.getId(), origen.getId(), destino.getId(), BigDecimal.ONE, null, null));

        ArgumentCaptor<KardexMovimiento> c = ArgumentCaptor.forClass(KardexMovimiento.class);
        verify(kardexRepository, times(2)).save(c.capture());
        assertEquals("Transferencia hacia B", c.getAllValues().get(0).getObservacion());
        assertNull(c.getAllValues().get(0).getReferenciaId());
    }

    @Test
    void registrarTransferencia_stockInsuficienteEnOrigen_noRegistraEntrada() {
        Almacen origen = almacen("A");
        Almacen destino = almacen("B");
        stockEn(origen, "1");
        Stock sDestino = stockEn(destino, "0");

        assertThrows(IllegalArgumentException.class, () -> service.registrarTransferencia(
                new TransferenciaRequestDTO(plancha.getId(), origen.getId(), destino.getId(), new BigDecimal("2"), null, null)));
        verify(kardexRepository, never()).save(any());
        assertEquals(0, sDestino.getCantidad().signum());
    }

    @Test
    void registrarTransferencia_mismoAlmacen_lanzaIllegalArgumentSinConsultar() {
        UUID mismo = principal.getId();

        assertThrows(IllegalArgumentException.class, () -> service.registrarTransferencia(
                new TransferenciaRequestDTO(plancha.getId(), mismo, mismo, BigDecimal.ONE, null, null)));
        verifyNoInteractions(stockRepository, kardexRepository);
    }

    @Test
    void registrarTransferencia_destinoInexistente_lanzaNotFound() {
        Almacen origen = almacen("A");
        UUID destino = UUID.randomUUID();
        when(almacenRepository.findById(destino)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.registrarTransferencia(
                new TransferenciaRequestDTO(plancha.getId(), origen.getId(), destino, BigDecimal.ONE, null, null)));
        verifyNoInteractions(kardexRepository);
    }

    @Test
    void searchMovimientos_delegaYMapea() {
        KardexMovimiento m = KardexMovimiento.builder().id(UUID.randomUUID()).build();
        Pageable p = PageRequest.of(0, 5);
        LocalDateTime ini = LocalDateTime.now().minusDays(1), fin = LocalDateTime.now();
        when(kardexRepository.searchMovimientos(plancha.getId(), null, ini, fin, p)).thenReturn(new PageImpl<>(List.of(m)));

        assertEquals(1, service.searchMovimientos(plancha.getId(), null, ini, fin, p).getTotalElements());
        verify(kardexMapper).toResponseDto(m);
    }
}
