package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.FiltroVentasReporteDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.ArqueoTurnoDTO;
import com.policalum.erp.application.dto.response.ArqueosReporteDTO;
import com.policalum.erp.application.dto.response.ClientesFrecuentesDTO;
import com.policalum.erp.application.dto.response.ParametrosReportesDTO;
import com.policalum.erp.application.dto.response.PeriodoVentasDTO;
import com.policalum.erp.application.dto.response.ProductosEstrellaDTO;
import com.policalum.erp.application.dto.response.ProveedoresRankingDTO;
import com.policalum.erp.application.dto.response.RentabilidadItemDTO;
import com.policalum.erp.application.dto.response.RentabilidadReporteDTO;
import com.policalum.erp.application.dto.response.VentaReporteDTO;
import com.policalum.erp.application.dto.response.VentasResumenDTO;
import com.policalum.erp.application.mapper.ReporteMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.AgrupacionReporte;
import com.policalum.erp.domain.enums.CriterioRankingCliente;
import com.policalum.erp.domain.enums.CriterioRankingProducto;
import com.policalum.erp.domain.enums.CriterioRankingProveedor;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.Moneda;
import com.policalum.erp.domain.enums.ResultadoArqueo;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Categoria;
import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.infrastructure.repository.CompraRepository;
import com.policalum.erp.infrastructure.repository.DetalleDevolucionRepository;
import com.policalum.erp.infrastructure.repository.DetalleVentaRepository;
import com.policalum.erp.infrastructure.repository.DevolucionRepository;
import com.policalum.erp.infrastructure.repository.PagoVentaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.TurnoCajaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import com.policalum.erp.infrastructure.repository.projection.ComprasPorProveedorProjection;
import com.policalum.erp.infrastructure.repository.projection.DevueltoPorProductoProjection;
import com.policalum.erp.infrastructure.repository.projection.EfectivoPorTurnoProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadPorIdProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorMetodoProjection;
import com.policalum.erp.infrastructure.repository.projection.PagoPorVentaProjection;
import com.policalum.erp.infrastructure.repository.projection.RecepcionCompraProjection;
import com.policalum.erp.infrastructure.repository.projection.RentabilidadProjection;
import com.policalum.erp.infrastructure.repository.projection.VentasPorClienteProjection;
import com.policalum.erp.infrastructure.repository.projection.VentasPorDiaProjection;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ReporteServiceImplTest {

    private static final String EMAIL = "admin@policalum.com";
    private static final LocalDate D = LocalDate.of(2026, 9, 1);
    private static final LocalDate H = LocalDate.of(2026, 9, 30);

    @Mock VentaRepository ventaRepository;
    @Mock PagoVentaRepository pagoRepository;
    @Mock DetalleVentaRepository detalleVentaRepository;
    @Mock DetalleDevolucionRepository detalleDevolucionRepository;
    @Mock DevolucionRepository devolucionRepository;
    @Mock ProductoRepository productoRepository;
    @Mock CompraRepository compraRepository;
    @Mock TurnoCajaRepository turnoRepository;
    @Mock UserRepository userRepository;
    @Mock ConfiguracionService configuracionService;
    @Mock AuditService auditService;
    @Spy ReporteMapper mapper = Mappers.getMapper(ReporteMapper.class);
    @InjectMocks ReporteServiceImpl service;

    User actual;
    Sede centro;
    Sede norte;

    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.reportes()).thenReturn(ParametrosReportesDTO.DEFAULTS);
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").activa(true).build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").activa(true).build();
        actual = usuario(RolNombre.ROLE_ADMIN, centro);

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(actual));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    // ================================================================ rango y top

    @Test
    void rango_sinFechasEsMesEnCurso_yDesdePosteriorAHastaEs400() {
        ReporteServiceImpl.Rango r = ReporteServiceImpl.rango(null, null);
        assertEquals(LocalDate.now().withDayOfMonth(1), r.desde());
        assertEquals(LocalDate.now(), r.hasta());
        assertEquals(LocalDate.now().plusDays(1).atStartOfDay(), r.fin(), "rango semiabierto [desde, hasta+1)");

        ReporteServiceImpl.Rango soloHasta = ReporteServiceImpl.rango(null, LocalDate.of(2026, 3, 15));
        assertEquals(LocalDate.of(2026, 3, 1), soloHasta.desde());

        assertThrows(IllegalArgumentException.class, () -> ReporteServiceImpl.rango(H, D));
        assertThrows(IllegalArgumentException.class, () -> ReporteServiceImpl.top(0));
        assertThrows(IllegalArgumentException.class, () -> ReporteServiceImpl.top(101));
        assertEquals(10, ReporteServiceImpl.top(null));
    }

    // ================================================================ ventas

    @Test
    void periodos_semanaIso_recortaAlRangoYRellenaSemanasSinVentas() {
        // 2026-09-01 es martes: la primera semana ISO (W36) empieza el lunes 31/08 pero se recorta al 01/09
        List<VentasPorDiaProjection> dias = List.of(
                dia(LocalDate.of(2026, 9, 2), 2, "100.00", "18.00", "118.00"),
                dia(LocalDate.of(2026, 9, 4), 1, "50.00", "9.00", "59.00"),
                dia(LocalDate.of(2026, 9, 21), 1, "10.00", "1.80", "11.80"));

        List<PeriodoVentasDTO> p = ReporteServiceImpl.periodos(dias, D, H, AgrupacionReporte.SEMANA);

        assertEquals(5, p.size());                        // W36..W40
        assertEquals("2026-W36", p.get(0).etiqueta());
        assertEquals(D, p.get(0).desde());
        assertEquals(LocalDate.of(2026, 9, 6), p.get(0).hasta());
        assertEquals(3, p.get(0).cantidad());
        assertEquals(new BigDecimal("177.00"), p.get(0).total());
        assertEquals(new BigDecimal("59.00"), p.get(0).ticketPromedio());
        assertEquals(0, p.get(1).cantidad());             // semana sin ventas va en cero
        assertNull(p.get(1).ticketPromedio());
        assertEquals(H, p.get(4).hasta());                // W40 se recorta al 30/09
    }

    @Test
    void periodos_porDiaDemasiadoLargo_lanza400() {
        assertThrows(IllegalArgumentException.class, () -> ReporteServiceImpl.periodos(List.of(),
                LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1), AgrupacionReporte.DIA));
        assertEquals(73, ReporteServiceImpl.periodos(List.of(), LocalDate.of(2020, 1, 1), LocalDate.of(2026, 1, 1),
                AgrupacionReporte.MES).size());
    }

    @Test
    void resumenVentas_sinFiltrosExtra_totalesTicketMetodosYDevolucionesComoUtilidadNeta() {
        when(ventaRepository.ventasPorDia(eq(EstadoVenta.COMPLETADA), any(), any(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(dia(LocalDate.of(2026, 9, 2), 3, "300.00", "54.00", "354.00")));
        when(pagoRepository.pagosPorMetodo(eq(EstadoVenta.COMPLETADA), any(), any(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(metodo(MetodoPago.YAPE, 1, "54.00"), metodo(MetodoPago.EFECTIVO, 2, "300.00")));
        when(devolucionRepository.totalDevueltoPorPeriodo(isNull(), eq(D.atStartOfDay()), eq(H.plusDays(1).atStartOfDay())))
                .thenReturn(proyeccion(MontoCantidadProjection.class, "total", new BigDecimal("59.00"), "cantidad", 1L));

        VentasResumenDTO r = service.resumenVentas(filtro(null, null), AgrupacionReporte.MES);

        assertEquals(3, r.cantidad());
        assertEquals(new BigDecimal("354.00"), r.total());
        assertEquals(new BigDecimal("118.00"), r.ticketPromedio());
        assertEquals(new BigDecimal("59.00"), r.devoluciones());
        assertEquals(new BigDecimal("295.00"), r.totalMenosDevoluciones());
        assertEquals(MetodoPago.EFECTIVO, r.porMetodoPago().get(0).metodo(), "ordenado por método");
        assertEquals(new BigDecimal("84.75"), r.porMetodoPago().get(0).porcentaje());
        assertEquals(1, r.periodos().size());
        assertTrue(r.advertencias().isEmpty());
    }

    @Test
    void resumenVentas_conFiltroDeVendedor_noRestaDevolucionesYLoAdvierte() {
        UUID vendedor = UUID.randomUUID();
        when(ventaRepository.ventasPorDia(any(), any(), any(), any(), eq(vendedor), any(), any(), any())).thenReturn(List.of());
        when(pagoRepository.pagosPorMetodo(any(), any(), any(), any(), eq(vendedor), any(), any(), any())).thenReturn(List.of());

        VentasResumenDTO r = service.resumenVentas(new FiltroVentasReporteDTO(D, H, null, vendedor, null, null, null, null), null);

        assertNull(r.devoluciones());
        assertNull(r.ticketPromedio());
        assertEquals(AgrupacionReporte.DIA, r.agrupacion());
        assertEquals(30, r.periodos().size());
        assertFalse(r.advertencias().isEmpty());
        verify(devolucionRepository, never()).totalDevueltoPorPeriodo(any(), any(), any());
    }

    @Test
    void reportes_multifuncional_soloSuSede() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        when(ventaRepository.ventasPorDia(any(), any(), any(), eq(centro.getId()), any(), any(), any(), any())).thenReturn(List.of());
        when(pagoRepository.pagosPorMetodo(any(), any(), any(), eq(centro.getId()), any(), any(), any(), any())).thenReturn(List.of());

        VentasResumenDTO r = service.resumenVentas(filtro(null, null), AgrupacionReporte.MES);
        assertEquals(centro.getId(), r.sedeId(), "sin sede pedida se fuerza la propia");

        assertThrows(AccessDeniedException.class, () -> service.resumenVentas(filtro(norte.getId(), null), null));
        assertThrows(AccessDeniedException.class, () -> service.rentabilidad(D, H, norte.getId(), null));
    }

    @Test
    void ventas_detalleIncluyeMetodosDePagoDeUnaSolaConsulta() {
        Venta v1 = venta("B001-1", "354.00"), v2 = venta("B001-2", "59.00");
        when(ventaRepository.reporteVentas(eq(EstadoVenta.COMPLETADA), any(), any(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(v1, v2), PageRequest.of(0, 20), 2));
        when(pagoRepository.pagosPorVentas(List.of(v1.getId(), v2.getId()))).thenReturn(List.of(
                pago(v1.getId(), MetodoPago.YAPE, "54.00"), pago(v1.getId(), MetodoPago.EFECTIVO, "300"),
                pago(v2.getId(), MetodoPago.TARJETA, "59.00")));

        Page<VentaReporteDTO> r = service.ventas(filtro(null, null), PageRequest.of(0, 20));

        assertEquals("EFECTIVO 300.00 + YAPE 54.00", r.getContent().get(0).metodosPago());
        assertEquals("Ana Pérez", r.getContent().get(0).vendedor());
        assertEquals("Constructora Sol", r.getContent().get(0).cliente());
        assertEquals("TARJETA 59.00", r.getContent().get(1).metodosPago());
        verify(pagoRepository, times(1)).pagosPorVentas(any());
    }

    @Test
    void exportarVentas_soloAdmin_respetaTopeYAudita() throws IOException {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        assertThrows(AccessDeniedException.class, () -> service.exportarVentas(filtro(null, null)));
        verifyNoInteractions(ventaRepository, auditService);

        actual = usuario(RolNombre.ROLE_ADMIN, centro);
        when(configuracionService.reportes()).thenReturn(new ParametrosReportesDTO(1_000));
        when(ventaRepository.reporteVentas(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1000), 1_001));
        assertThrows(IllegalArgumentException.class, () -> service.exportarVentas(filtro(null, null)));

        Venta v = venta("B001-9", "118.00");
        v.getCliente().setNombre("=HYPERLINK(\"x\")");
        when(ventaRepository.reporteVentas(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(v), PageRequest.of(0, 1000), 1));
        when(pagoRepository.pagosPorVentas(any())).thenReturn(List.of());

        ArchivoDescargaDTO a = service.exportarVentas(filtro(null, null));

        String csv = new String(a.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("﻿fecha,serie_correlativo"));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"x\"\")\""), "fórmula neutralizada por CsvUtil");
        assertTrue(a.nombreOriginal().startsWith("reporte_ventas_"));
        verify(auditService).registrar(eq("reportes"), eq(actual.getId()), eq(AccionAudit.ACCESS), anyMap());
    }

    // ================================================================ rentabilidad / estrella

    @Test
    void rentabilidad_restaDevolucionesEstimaCostoYSeparaServicios() {
        UUID plancha = UUID.randomUUID(), perfil = UUID.randomUUID(), corte = UUID.randomUUID(), devueltoSolo = UUID.randomUUID();
        when(detalleVentaRepository.rentabilidad(any(), any(), isNull(), isNull())).thenReturn(List.of(
                // 10 planchas por 1000 neto, costo actual 60 → costo 540 tras devolver 1
                renta(plancha, null, "Plancha 6mm", "60.00", "10", "1000.00", 7),
                renta(perfil, null, "Perfil U", null, "4", "80.00", 2),          // sin costo registrado
                renta(null, corte, "Corte a medida", null, "3", "90.00", 3)));     // servicio
        when(detalleDevolucionRepository.devueltoPorProducto(any(), any(), isNull(), isNull())).thenReturn(List.of(
                devuelto(plancha, "1", "100.00"), devuelto(devueltoSolo, "0.5", "40.00")));
        Producto p = Producto.builder().id(devueltoSolo).nombre("Plancha 8mm").precioCompra(new BigDecimal("70.00"))
                .categoria(Categoria.builder().nombre("Policarbonato").build()).build();
        when(productoRepository.findAllById(any())).thenReturn(List.of(p));

        RentabilidadReporteDTO r = service.rentabilidad(D, H, null, null);

        RentabilidadItemDTO pl = item(r, plancha);
        assertEquals(new BigDecimal("9.00"), pl.cantidadNeta());
        assertEquals(new BigDecimal("900.00"), pl.ingresoNetoReal());
        assertEquals(new BigDecimal("540.00"), pl.costoEstimado());
        assertEquals(new BigDecimal("360.00"), pl.margen());
        assertEquals(new BigDecimal("40.00"), pl.margenPorcentaje());

        RentabilidadItemDTO soloDev = item(r, devueltoSolo);   // devuelto sin ventas en el periodo: resta igual
        assertEquals(0, soloDev.ventas());
        assertEquals(new BigDecimal("-40.00"), soloDev.ingresoNetoReal());
        assertEquals(new BigDecimal("-35.00"), soloDev.costoEstimado());
        assertEquals("Policarbonato", soloDev.categoria());

        RentabilidadItemDTO servicio = item(r, corte);
        assertEquals(RentabilidadItemDTO.SERVICIO, servicio.tipo());
        assertNull(servicio.costoEstimado());
        assertNull(item(r, perfil).margen());

        assertEquals(new BigDecimal("1030.00"), r.ingresoNetoReal());   // 900 + 80 + 90 − 40
        assertEquals(new BigDecimal("505.00"), r.costoEstimado());      // 540 − 35
        assertEquals(new BigDecimal("355.00"), r.margen());
        assertEquals(new BigDecimal("41.28"), r.margenPorcentaje());    // 355 / 860
        assertEquals(plancha, r.items().get(0).id(), "orden por ingreso real");
        assertTrue(r.costosVisibles());
        assertEquals(3, r.advertencias().size());
    }

    @Test
    void rentabilidad_multifuncional_ocultaCostosYMargen() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        UUID plancha = UUID.randomUUID();
        when(detalleVentaRepository.rentabilidad(any(), any(), eq(centro.getId()), isNull()))
                .thenReturn(List.of(renta(plancha, null, "Plancha 6mm", "60.00", "2", "200.00", 1)));
        when(detalleDevolucionRepository.devueltoPorProducto(any(), any(), eq(centro.getId()), isNull())).thenReturn(List.of());

        RentabilidadReporteDTO r = service.rentabilidad(D, H, null, null);

        assertFalse(r.costosVisibles());
        assertNull(r.costoEstimado());
        assertNull(r.margen());
        assertNull(r.items().get(0).costoUnitario());
        assertNull(r.items().get(0).margenPorcentaje());
        assertEquals(new BigDecimal("200.00"), r.items().get(0).ingresoNetoReal());
        assertTrue(r.advertencias().stream().noneMatch(a -> a.contains("costo")));
    }

    @Test
    void productosEstrella_rankingSinServicios_yMargenSoloAdmin() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), s = UUID.randomUUID();
        when(detalleVentaRepository.rentabilidad(any(), any(), any(), any())).thenReturn(List.of(
                renta(a, null, "Plancha A", "90.00", "10", "1000.00", 5),   // margen 100
                renta(b, null, "Plancha B", "10.00", "5", "500.00", 5),     // margen 450
                renta(null, s, "Instalación", null, "50", "5000.00", 50)));
        when(detalleDevolucionRepository.devueltoPorProducto(any(), any(), any(), any())).thenReturn(List.of());

        ProductosEstrellaDTO porIngreso = service.productosEstrella(D, H, null, null, null, 5);
        assertEquals(List.of(a, b), porIngreso.items().stream().map(RentabilidadItemDTO::id).toList(), "servicios excluidos");

        ProductosEstrellaDTO porMargen = service.productosEstrella(D, H, null, null, CriterioRankingProducto.MARGEN, 1);
        assertEquals(b, porMargen.items().get(0).id());
        assertEquals(1, porMargen.items().size());

        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        assertThrows(AccessDeniedException.class,
                () -> service.productosEstrella(D, H, null, null, CriterioRankingProducto.MARGEN, 5));
        ProductosEstrellaDTO mf = service.productosEstrella(D, H, null, null, CriterioRankingProducto.CANTIDAD, 5);
        assertEquals(a, mf.items().get(0).id());
        assertNull(mf.items().get(0).costoEstimado());
    }

    // ================================================================ clientes

    @Test
    void clientesFrecuentes_frecuenciaDevueltoYPublicoGeneralAparte() {
        UUID ana = UUID.randomUUID(), luis = UUID.randomUUID(), rosa = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 10, 0);
        when(ventaRepository.ventasPorCliente(any(), any(), isNull())).thenReturn(List.of(
                porCliente(ana, "Ana", 5, "1000.00", base, base.plusDays(20)),     // cada 5 días
                porCliente(luis, "Luis", 3, "3000.00", base, base.plusDays(4)),    // cada 2 días
                porCliente(rosa, "Rosa", 1, "5000.00", base, base),                // 1 compra
                porCliente(null, null, 40, "4000.00", base, base.plusDays(29))));  // público general
        when(devolucionRepository.devueltoPorCliente(any(), any(), isNull())).thenReturn(List.of(
                proyeccion(MontoCantidadPorIdProjection.class, "id", rosa, "total", new BigDecimal("4500.00"), "cantidad", 1L)));

        ClientesFrecuentesDTO porCantidad = service.clientesFrecuentes(D, H, null, null, null);
        assertEquals(3, porCantidad.clientesConCompras());
        assertEquals(40, porCantidad.ventasSinCliente());
        assertEquals(new BigDecimal("4000.00"), porCantidad.montoSinCliente());
        assertEquals(ana, porCantidad.items().get(0).clienteId());
        assertEquals(1, porCantidad.items().get(0).posicion());
        assertEquals(new BigDecimal("5.0"), porCantidad.items().get(0).diasEntreCompras());

        ClientesFrecuentesDTO porMonto = service.clientesFrecuentes(D, H, null, CriterioRankingCliente.MONTO, 10);
        assertEquals(luis, porMonto.items().get(0).clienteId(), "Rosa baja por la devolución: neto 500");
        assertEquals(new BigDecimal("500.00"), porMonto.items().get(2).montoNeto());

        ClientesFrecuentesDTO porFrecuencia = service.clientesFrecuentes(D, H, null, CriterioRankingCliente.FRECUENCIA, 10);
        assertEquals(List.of(luis, ana), porFrecuencia.items().stream().map(c -> c.clienteId()).toList(), "con 1 compra no hay frecuencia");
    }

    // ================================================================ proveedores

    @Test
    void rankingProveedores_diasDeEntregaYMontosOcultosParaMultifuncional() {
        UUID rapido = UUID.randomUUID(), lento = UUID.randomUUID();
        when(compraRepository.comprasPorProveedor(eq(Moneda.PEN), eq(D), eq(H.plusDays(1)), any())).thenReturn(List.of(
                compras(rapido, "Plásticos SAC", 2, 2, "2360.00"),
                compras(lento, "Aluminios EIRL", 4, 3, "9000.00")));
        when(compraRepository.recepcionesCompra(eq(D), eq(H.plusDays(1)), any())).thenReturn(List.of(
                recepcion(rapido, D, D.plusDays(1)), recepcion(rapido, D.plusDays(10), D.plusDays(12)),
                recepcion(lento, D, D.plusDays(9)), recepcion(lento, D.plusDays(2), D.plusDays(5))));
        when(compraRepository.contarEnOtraMoneda(any(), eq(Moneda.PEN), eq(D), eq(H), any())).thenReturn(1L);

        ProveedoresRankingDTO porEntrega = service.rankingProveedores(D, H, null, CriterioRankingProveedor.ENTREGA, 10);
        assertEquals(rapido, porEntrega.items().get(0).proveedorId());
        assertEquals(new BigDecimal("1.5"), porEntrega.items().get(0).diasEntregaPromedio());
        assertEquals(9, porEntrega.items().get(1).diasEntregaMaximo());
        assertEquals(new BigDecimal("3000.00"), porEntrega.items().get(1).ticketPromedio());
        assertEquals(1, porEntrega.comprasOtraMoneda());

        ProveedoresRankingDTO porVolumen = service.rankingProveedores(D, H, null, CriterioRankingProveedor.VOLUMEN, 10);
        assertEquals(lento, porVolumen.items().get(0).proveedorId());

        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        assertThrows(AccessDeniedException.class, () -> service.rankingProveedores(D, H, null, CriterioRankingProveedor.VOLUMEN, 10));
        ProveedoresRankingDTO mf = service.rankingProveedores(D, H, null, null, 10);
        assertFalse(mf.montosVisibles());
        assertNull(mf.items().get(0).montoRecibido());
        assertEquals(lento, mf.items().get(0).proveedorId(), "FRECUENCIA por defecto: más compras recibidas");
    }

    // ================================================================ arqueos

    @Test
    void arqueos_mismaFormulaQueElCierre_resumenYPaginacion() {
        TurnoCaja faltante = turno(EstadoTurno.CERRADO, "100.00", "525.00", "50.00", "20.00");
        TurnoCaja cuadrado = turno(EstadoTurno.CERRADO, "200.00", "200.00", null, null);
        TurnoCaja sobrante = turno(EstadoTurno.CERRADO, "0.00", "110.00", null, null);
        TurnoCaja abierto = turno(EstadoTurno.ABIERTO, "50.00", null, null, null);
        when(turnoRepository.reporteArqueos(any(), any(), isNull(), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(faltante, cuadrado, sobrante, abierto), PageRequest.of(0, 50_000), 4));
        when(pagoRepository.efectivoPorTurnos(any())).thenReturn(List.of(
                efectivo(faltante.getId(), "500.00", "30.00"), efectivo(sobrante.getId(), "100.00", "0")));

        ArqueosReporteDTO r = service.arqueos(D, H, null, null, null, PageRequest.of(0, 2));

        ArqueoTurnoDTO f = r.turnos().get(0);
        assertEquals(new BigDecimal("530.00"), f.esperado());   // 100 + 500 − 50 − 20
        assertEquals(new BigDecimal("-5.00"), f.diferencia());
        assertEquals(ResultadoArqueo.FALTANTE, f.resultado());
        assertEquals(new BigDecimal("30.00"), f.efectivoVentasAnuladas());
        assertEquals(ResultadoArqueo.CUADRADO, r.turnos().get(1).resultado());
        assertEquals(2, r.turnos().size());
        assertEquals(4, r.totalElementos());
        assertEquals(2, r.totalPaginas());

        assertEquals(4, r.resumen().turnos());
        assertEquals(1, r.resumen().abiertos());
        assertEquals(1, r.resumen().conFaltante());
        assertEquals(1, r.resumen().conSobrante());
        assertEquals(new BigDecimal("5.00"), r.resumen().totalFaltante());
        assertEquals(new BigDecimal("10.00"), r.resumen().totalSobrante());
        assertEquals(new BigDecimal("5.00"), r.resumen().diferenciaNeta());

        ArqueosReporteDTO pagina2 = service.arqueos(D, H, null, null, null, PageRequest.of(1, 2));
        assertEquals(ResultadoArqueo.ABIERTO, pagina2.turnos().get(1).resultado());
        assertNull(pagina2.turnos().get(1).diferencia());
    }

    @Test
    void arqueos_multifuncional_soloSusTurnos() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);
        assertThrows(AccessDeniedException.class, () -> service.arqueos(D, H, null, UUID.randomUUID(), null, Pageable.unpaged()));

        when(turnoRepository.reporteArqueos(any(), any(), eq(centro.getId()), eq(actual.getId()), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50_000), 0));
        ArqueosReporteDTO r = service.arqueos(D, H, null, null, null, Pageable.unpaged());
        assertEquals(actual.getId(), r.usuarioId());
        assertEquals(0, r.resumen().turnos());
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void arqueos_masTurnosQueElTope_lanza400() {
        when(configuracionService.reportes()).thenReturn(new ParametrosReportesDTO(1_000));
        when(turnoRepository.reporteArqueos(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1_000), 1_500));
        assertThrows(IllegalArgumentException.class, () -> service.exportarArqueos(D, H, null, null, null));
    }

    // ================================================================ helpers

    private User usuario(RolNombre rol, Sede sede) {
        return User.builder().id(UUID.randomUUID()).nombre("Ana").apellido("Pérez").email(EMAIL).activo(true).sede(sede)
                .rol(Rol.builder().nombre(rol).build()).build();
    }

    private static FiltroVentasReporteDTO filtro(UUID sedeId, EstadoVenta estado) {
        return new FiltroVentasReporteDTO(D, H, sedeId, null, null, null, null, estado);
    }

    private Venta venta(String serie, String total) {
        return Venta.builder().id(UUID.randomUUID()).serieCorrelativo(serie).sede(centro).user(actual)
                .cliente(Cliente.builder().nombre("Constructora Sol").numeroDocumento("20123456789").build())
                .estado(EstadoVenta.COMPLETADA).total(new BigDecimal(total)).createdAt(LocalDateTime.of(2026, 9, 2, 10, 0)).build();
    }

    private TurnoCaja turno(EstadoTurno estado, String inicial, String declarado, String retiros, String reembolsos) {
        return TurnoCaja.builder().id(UUID.randomUUID()).sede(centro).user(actual).estado(estado)
                .montoInicial(new BigDecimal(inicial)).montoFinalDeclarado(declarado != null ? new BigDecimal(declarado) : null)
                .totalRetirosSocios(retiros != null ? new BigDecimal(retiros) : null)
                .totalReembolsos(reembolsos != null ? new BigDecimal(reembolsos) : null)
                .fechaApertura(LocalDateTime.of(2026, 9, 5, 8, 0)).build();
    }

    private static RentabilidadItemDTO item(RentabilidadReporteDTO r, UUID id) {
        return r.items().stream().filter(i -> id.equals(i.id())).findFirst().orElseThrow();
    }

    private static VentasPorDiaProjection dia(LocalDate f, long cantidad, String neto, String igv, String total) {
        return proyeccion(VentasPorDiaProjection.class, "fecha", f, "cantidad", cantidad, "neto", new BigDecimal(neto),
                "igv", new BigDecimal(igv), "total", new BigDecimal(total));
    }

    private static MontoPorMetodoProjection metodo(MetodoPago m, long cantidad, String total) {
        return proyeccion(MontoPorMetodoProjection.class, "metodo", m, "cantidad", cantidad, "total", new BigDecimal(total));
    }

    private static PagoPorVentaProjection pago(UUID ventaId, MetodoPago m, String total) {
        return proyeccion(PagoPorVentaProjection.class, "ventaId", ventaId, "metodo", m, "total", new BigDecimal(total));
    }

    private static RentabilidadProjection renta(UUID productoId, UUID servicioId, String nombre, String costo, String cantidad,
                                                String ingreso, long ventas) {
        return proyeccion(RentabilidadProjection.class, "productoId", productoId, "servicioId", servicioId, "nombre", nombre,
                "costoUnitario", costo != null ? new BigDecimal(costo) : null, "cantidad", new BigDecimal(cantidad),
                "ingresoNeto", new BigDecimal(ingreso), "ventas", ventas);
    }

    private static DevueltoPorProductoProjection devuelto(UUID productoId, String cantidad, String monto) {
        return proyeccion(DevueltoPorProductoProjection.class, "productoId", productoId, "cantidad", new BigDecimal(cantidad),
                "montoNeto", new BigDecimal(monto));
    }

    private static VentasPorClienteProjection porCliente(UUID id, String nombre, long cantidad, String total,
                                                         LocalDateTime primera, LocalDateTime ultima) {
        return proyeccion(VentasPorClienteProjection.class, "clienteId", id, "nombre", nombre, "cantidad", cantidad,
                "total", new BigDecimal(total), "primeraCompra", primera, "ultimaCompra", ultima);
    }

    private static ComprasPorProveedorProjection compras(UUID id, String nombre, long recibidas, long recibidasPen, String monto) {
        return proyeccion(ComprasPorProveedorProjection.class, "proveedorId", id, "nombre", nombre, "cantidad", recibidas + 1,
                "recibidas", recibidas, "pendientes", 1L, "anuladas", 0L, "recibidasMonedaBase", recibidasPen,
                "monto", new BigDecimal(monto), "ultimaCompra", H);
    }

    private static RecepcionCompraProjection recepcion(UUID proveedorId, LocalDate emision, LocalDate recepcion) {
        return proyeccion(RecepcionCompraProjection.class, "proveedorId", proveedorId, "fechaEmision", emision,
                "fechaRecepcion", recepcion.atTime(15, 30));
    }

    private static EfectivoPorTurnoProjection efectivo(UUID turnoId, String total, String anuladas) {
        return proyeccion(EfectivoPorTurnoProjection.class, "turnoId", turnoId, "total", new BigDecimal(total),
                "anuladas", new BigDecimal(anuladas));
    }

    /** Implementa una proyección de Spring Data con un mapa propiedad → valor (getX → "x"). */
    @SuppressWarnings("unchecked")
    private static <T> T proyeccion(Class<T> tipo, Object... clavesYValores) {
        Map<String, Object> valores = new HashMap<>();
        for (int i = 0; i < clavesYValores.length; i += 2) valores.put((String) clavesYValores[i], clavesYValores[i + 1]);
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[]{tipo}, (proxy, m, args) -> {
            if (m.getName().equals("toString")) return tipo.getSimpleName() + valores;
            if (m.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (m.getName().equals("equals")) return proxy == args[0];
            String p = m.getName().substring(3);
            return valores.get(Character.toLowerCase(p.charAt(0)) + p.substring(1));
        });
    }

    // ===================== Completado en auditoría de cobertura =====================
    private static String contenido(ArchivoDescargaDTO a) throws IOException {
        return new String(a.resource().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> datosAuditados() {
        ArgumentCaptor<Map<String, Object>> c = ArgumentCaptor.forClass(Map.class);
        verify(auditService).registrar(eq("reportes"), eq(actual.getId()), eq(AccionAudit.ACCESS), c.capture());
        return c.getValue();
    }

    @Test
    void exportarRentabilidad_admin_csvConCabeceraFilasYAuditoria() throws IOException {
        UUID plancha = UUID.randomUUID(), categoria = UUID.randomUUID();
        when(detalleVentaRepository.rentabilidad(any(), any(), isNull(), eq(categoria))).thenReturn(List.of(
                renta(plancha, null, "Plancha 6mm", "60.00", "10", "1000.00", 7)));
        when(detalleDevolucionRepository.devueltoPorProducto(any(), any(), isNull(), eq(categoria))).thenReturn(List.of());

        ArchivoDescargaDTO a = service.exportarRentabilidad(D, H, null, categoria);

        String csv = contenido(a);
        assertTrue(csv.startsWith("﻿tipo,nombre,sku"));
        assertTrue(csv.contains("Plancha 6mm"));
        assertEquals(2, csv.strip().split("\r?\n").length, "cabecera + 1 producto");
        assertTrue(a.nombreOriginal().startsWith("reporte_rentabilidad_") && a.nombreOriginal().endsWith(".csv"));
        Map<String, Object> d = datosAuditados();
        assertEquals("RENTABILIDAD", d.get("reporte"));
        assertEquals(1L, d.get("filas"));
        assertEquals(categoria.toString(), d.get("categoriaId"));
        assertEquals("2026-09-01", d.get("desde"));
    }

    @Test
    void exportarRentabilidad_noAdmin_lanzaAccessDeniedSinConsultar() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);

        assertThrows(AccessDeniedException.class, () -> service.exportarRentabilidad(D, H, null, null));
        verifyNoInteractions(detalleVentaRepository, auditService);
    }

    @Test
    void exportarRentabilidad_superaTope_lanzaIllegalArgumentSinAuditar() {
        when(configuracionService.reportes()).thenReturn(new ParametrosReportesDTO(1));
        when(detalleVentaRepository.rentabilidad(any(), any(), any(), any())).thenReturn(List.of(
                renta(UUID.randomUUID(), null, "A", "1.00", "1", "10.00", 1),
                renta(UUID.randomUUID(), null, "B", "1.00", "1", "10.00", 1)));
        when(detalleDevolucionRepository.devueltoPorProducto(any(), any(), any(), any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.exportarRentabilidad(D, H, null, null));
        verifyNoInteractions(auditService);
    }

    @Test
    void exportarRentabilidad_rangoInvertido_lanzaIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.exportarRentabilidad(H, D, null, null));
        verifyNoInteractions(detalleVentaRepository);
    }

    @Test
    void exportarClientes_criterioPorDefectoCantidadYRankingCompleto() throws IOException {
        UUID ana = UUID.randomUUID(), luis = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 10, 0);
        when(ventaRepository.ventasPorCliente(any(), any(), isNull())).thenReturn(List.of(
                porCliente(ana, "Ana", 5, "1000.00", base, base.plusDays(20)),
                porCliente(luis, "Luis", 3, "3000.00", base, base.plusDays(4)),
                porCliente(null, null, 40, "4000.00", base, base.plusDays(29))));
        when(devolucionRepository.devueltoPorCliente(any(), any(), isNull())).thenReturn(List.of());

        ArchivoDescargaDTO a = service.exportarClientes(D, H, null, null);

        String csv = contenido(a);
        assertTrue(csv.startsWith("﻿posicion,nombre"));
        String[] lineas = csv.strip().split("\r?\n");
        assertEquals(3, lineas.length, "cabecera + 2 clientes; el público general no se exporta");
        assertTrue(lineas[1].contains("Ana"), "CANTIDAD por defecto: Ana (5 compras) primero");
        Map<String, Object> d = datosAuditados();
        assertEquals("CLIENTES", d.get("reporte"));
        assertEquals("CANTIDAD", d.get("criterio"));
        assertEquals(2L, d.get("filas"));
    }

    @Test
    void exportarClientes_porMonto_ordenaPorMontoNeto() throws IOException {
        UUID ana = UUID.randomUUID(), luis = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 10, 0);
        when(ventaRepository.ventasPorCliente(any(), any(), isNull())).thenReturn(List.of(
                porCliente(ana, "Ana", 5, "1000.00", base, base.plusDays(20)),
                porCliente(luis, "Luis", 3, "3000.00", base, base.plusDays(4))));
        when(devolucionRepository.devueltoPorCliente(any(), any(), isNull())).thenReturn(List.of());

        String csv = contenido(service.exportarClientes(D, H, null, CriterioRankingCliente.MONTO));

        assertTrue(csv.strip().split("\r?\n")[1].contains("Luis"));
        assertEquals("MONTO", datosAuditados().get("criterio"));
    }

    @Test
    void exportarClientes_noAdmin_lanzaAccessDenied() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);

        assertThrows(AccessDeniedException.class, () -> service.exportarClientes(D, H, null, null));
        verifyNoInteractions(ventaRepository, auditService);
    }

    @Test
    void exportarClientes_superaTope_lanzaIllegalArgument() {
        when(configuracionService.reportes()).thenReturn(new ParametrosReportesDTO(1));
        LocalDateTime base = LocalDateTime.of(2026, 9, 1, 10, 0);
        when(ventaRepository.ventasPorCliente(any(), any(), any())).thenReturn(List.of(
                porCliente(UUID.randomUUID(), "A", 1, "1.00", base, base),
                porCliente(UUID.randomUUID(), "B", 1, "1.00", base, base)));
        when(devolucionRepository.devueltoPorCliente(any(), any(), any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.exportarClientes(D, H, null, null));
        verifyNoInteractions(auditService);
    }

    @Test
    void exportarProveedores_criterioPorDefectoFrecuencia() throws IOException {
        UUID rapido = UUID.randomUUID(), lento = UUID.randomUUID();
        when(compraRepository.comprasPorProveedor(eq(Moneda.PEN), eq(D), eq(H.plusDays(1)), any())).thenReturn(List.of(
                compras(rapido, "Plásticos SAC", 2, 2, "2360.00"),
                compras(lento, "Aluminios EIRL", 4, 3, "9000.00")));
        when(compraRepository.recepcionesCompra(eq(D), eq(H.plusDays(1)), any())).thenReturn(List.of());

        ArchivoDescargaDTO a = service.exportarProveedores(D, H, null, null);

        String csv = contenido(a);
        assertTrue(csv.startsWith("﻿posicion,nombre,numero_documento"));
        String[] lineas = csv.strip().split("\r?\n");
        assertEquals(3, lineas.length);
        assertTrue(lineas[1].contains("Aluminios EIRL"), "FRECUENCIA: más compras recibidas primero");
        assertTrue(a.nombreOriginal().startsWith("reporte_proveedores_"));
        Map<String, Object> d = datosAuditados();
        assertEquals("PROVEEDORES", d.get("reporte"));
        assertEquals("FRECUENCIA", d.get("criterio"));
    }

    @Test
    void exportarProveedores_sinCompras_soloCabecera() throws IOException {
        when(compraRepository.comprasPorProveedor(any(), any(), any(), any())).thenReturn(List.of());
        when(compraRepository.recepcionesCompra(any(), any(), any())).thenReturn(List.of());

        String csv = contenido(service.exportarProveedores(D, H, centro.getId(), CriterioRankingProveedor.VOLUMEN));

        assertEquals(1, csv.strip().split("\r?\n").length);
        assertEquals(0L, datosAuditados().get("filas"));
    }

    @Test
    void exportarProveedores_noAdmin_lanzaAccessDenied() {
        actual = usuario(RolNombre.ROLE_MULTIFUNCIONAL, centro);

        assertThrows(AccessDeniedException.class, () -> service.exportarProveedores(D, H, null, null));
        verifyNoInteractions(compraRepository, auditService);
    }

    @Test
    void exportarProveedores_superaTope_lanzaIllegalArgument() {
        when(configuracionService.reportes()).thenReturn(new ParametrosReportesDTO(1));
        when(compraRepository.comprasPorProveedor(any(), any(), any(), any())).thenReturn(List.of(
                compras(UUID.randomUUID(), "A", 1, 1, "1.00"), compras(UUID.randomUUID(), "B", 1, 1, "1.00")));
        when(compraRepository.recepcionesCompra(any(), any(), any())).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> service.exportarProveedores(D, H, null, null));
        verifyNoInteractions(auditService);
    }
}
