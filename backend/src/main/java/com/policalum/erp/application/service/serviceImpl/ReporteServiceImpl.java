package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.FiltroVentasReporteDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.ArqueoTurnoDTO;
import com.policalum.erp.application.dto.response.ArqueosReporteDTO;
import com.policalum.erp.application.dto.response.ClienteFrecuenteDTO;
import com.policalum.erp.application.dto.response.ClientesFrecuentesDTO;
import com.policalum.erp.application.dto.response.MetodoPagoMontoDTO;
import com.policalum.erp.application.dto.response.PeriodoVentasDTO;
import com.policalum.erp.application.dto.response.ProductosEstrellaDTO;
import com.policalum.erp.application.dto.response.ProveedorRankingDTO;
import com.policalum.erp.application.dto.response.ProveedoresRankingDTO;
import com.policalum.erp.application.dto.response.RentabilidadItemDTO;
import com.policalum.erp.application.dto.response.RentabilidadReporteDTO;
import com.policalum.erp.application.dto.response.ResumenArqueosDTO;
import com.policalum.erp.application.dto.response.VentaReporteDTO;
import com.policalum.erp.application.dto.response.VentasResumenDTO;
import com.policalum.erp.application.mapper.ReporteMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.ReporteService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.AgrupacionReporte;
import com.policalum.erp.domain.enums.CriterioRankingCliente;
import com.policalum.erp.domain.enums.CriterioRankingProducto;
import com.policalum.erp.domain.enums.CriterioRankingProveedor;
import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.Moneda;
import com.policalum.erp.domain.enums.ResultadoArqueo;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Producto;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    static final int TOP_DEFAULT = 10;
    static final int TOP_MAX = 100;
    /** Tope de periodos de la serie de ventas (p. ej. más de ~2,7 años por día). */
    static final int MAX_PERIODOS = 1_000;
    static final int LOTE = 1_000;
    private static final String TABLA_AUDITORIA = "reportes";
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final VentaRepository ventaRepository;
    private final PagoVentaRepository pagoRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final DetalleDevolucionRepository detalleDevolucionRepository;
    private final DevolucionRepository devolucionRepository;
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final TurnoCajaRepository turnoRepository;
    private final UserRepository userRepository;
    private final ConfiguracionService configuracionService;
    private final AuditService auditService;
    private final ReporteMapper mapper;

    // =================================================================== VENTAS

    @Override
    public VentasResumenDTO resumenVentas(FiltroVentasReporteDTO f, AgrupacionReporte agrupacion) {
        Acceso a = acceso(f.sedeId());
        Rango r = rango(f.desde(), f.hasta());
        AgrupacionReporte g = agrupacion != null ? agrupacion : AgrupacionReporte.DIA;
        EstadoVenta estado = estado(f);

        List<VentasPorDiaProjection> dias = ventaRepository.ventasPorDia(estado, r.inicio(), r.fin(), a.sedeId(),
                f.vendedorId(), f.clienteId(), f.canal(), f.tipoComprobante());
        List<PeriodoVentasDTO> periodos = periodos(dias, r.desde(), r.hasta(), g);

        long cantidad = 0;
        BigDecimal neto = BigDecimal.ZERO, igv = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (PeriodoVentasDTO p : periodos) {
            cantidad += p.cantidad();
            neto = neto.add(p.neto());
            igv = igv.add(p.igv());
            total = total.add(p.total());
        }

        List<MontoPorMetodoProjection> cobros = pagoRepository.pagosPorMetodo(estado, r.inicio(), r.fin(), a.sedeId(),
                f.vendedorId(), f.clienteId(), f.canal(), f.tipoComprobante());
        BigDecimal totalCobrado = cobros.stream().map(c -> nvl(c.getTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MetodoPagoMontoDTO> porMetodo = cobros.stream()
                .sorted(Comparator.comparing(MontoPorMetodoProjection::getMetodo))
                .map(c -> new MetodoPagoMontoDTO(c.getMetodo(), n(c.getCantidad()), dinero(c.getTotal()),
                        porcentaje(nvl(c.getTotal()), totalCobrado)))
                .toList();

        List<String> advertencias = new ArrayList<>();
        BigDecimal devoluciones = null, totalMenosDevoluciones = null;
        Long cantidadDevoluciones = null;
        if (estado == EstadoVenta.COMPLETADA && !f.filtraMasQueSede()) {
            // Mismo criterio que la Utilidad Neta (M6): devoluciones aprobadas resueltas en el periodo, con IGV.
            MontoCantidadProjection dev = devolucionRepository.totalDevueltoPorPeriodo(a.sedeId(), r.inicio(), r.fin());
            devoluciones = dinero(dev != null ? dev.getTotal() : null);
            cantidadDevoluciones = dev != null ? n(dev.getCantidad()) : 0L;
            totalMenosDevoluciones = dinero(total.subtract(devoluciones));
        } else if (estado == EstadoVenta.COMPLETADA) {
            advertencias.add("Las devoluciones no se pueden filtrar por vendedor, cliente, canal ni tipo de comprobante: no se restan");
        }

        return new VentasResumenDTO(r.desde(), r.hasta(), g, a.sedeId(), estado, cantidad, dinero(neto), dinero(igv),
                dinero(total), promedio(total, cantidad), devoluciones, cantidadDevoluciones, totalMenosDevoluciones,
                periodos, porMetodo, advertencias);
    }

    @Override
    public Page<VentaReporteDTO> ventas(FiltroVentasReporteDTO f, Pageable pageable) {
        Acceso a = acceso(f.sedeId());
        Rango r = rango(f.desde(), f.hasta());
        Page<Venta> pagina = ventaRepository.reporteVentas(estado(f), r.inicio(), r.fin(), a.sedeId(), f.vendedorId(),
                f.clienteId(), f.canal(), f.tipoComprobante(), sinOrden(pageable));
        Map<UUID, String> pagos = metodosPago(pagina.getContent());
        return pagina.map(v -> mapper.toResponseDto(v, pagos.get(v.getId())));
    }

    @Override
    public ArchivoDescargaDTO exportarVentas(FiltroVentasReporteDTO f) {
        User admin = exigirAdmin();
        Rango r = rango(f.desde(), f.hasta());
        EstadoVenta estado = estado(f);
        int max = maxFilas();
        Page<Venta> pagina = ventaRepository.reporteVentas(estado, r.inicio(), r.fin(), f.sedeId(), f.vendedorId(),
                f.clienteId(), f.canal(), f.tipoComprobante(), PageRequest.of(0, LOTE));
        exigirTope(pagina.getTotalElements(), max, "ventas");

        StringBuilder csv = ReporteCsv.nuevo(ReporteCsv.COLUMNAS_VENTAS);
        long filas = 0;
        while (true) {
            Map<UUID, String> pagos = metodosPago(pagina.getContent());
            for (Venta v : pagina.getContent()) {
                ReporteCsv.venta(csv, mapper.toResponseDto(v, pagos.get(v.getId())));
                filas++;
            }
            if (!pagina.hasNext() || filas >= max) break;
            pagina = ventaRepository.reporteVentas(estado, r.inicio(), r.fin(), f.sedeId(), f.vendedorId(), f.clienteId(),
                    f.canal(), f.tipoComprobante(), pagina.nextPageable());
        }
        Map<String, Object> datos = datos("VENTAS", r, f.sedeId(), filas);
        datos.put("estado", estado.name());
        datos.put("vendedorId", texto(f.vendedorId()));
        datos.put("clienteId", texto(f.clienteId()));
        datos.put("canal", texto(f.canal()));
        datos.put("tipoComprobante", texto(f.tipoComprobante()));
        return archivo(admin, "ventas", csv, datos);
    }

    /** Divide los días con ventas en periodos completos del rango (los periodos sin ventas van en cero). */
    static List<PeriodoVentasDTO> periodos(List<VentasPorDiaProjection> dias, LocalDate desde, LocalDate hasta,
                                           AgrupacionReporte g) {
        TreeMap<LocalDate, Acumulado> porInicio = new TreeMap<>();
        for (LocalDate inicio = inicioPeriodo(desde, g); !inicio.isAfter(hasta); inicio = siguientePeriodo(inicio, g)) {
            if (porInicio.size() >= MAX_PERIODOS) {
                throw new IllegalArgumentException("El rango genera más de " + MAX_PERIODOS + " periodos; use una agrupación "
                        + (g == AgrupacionReporte.DIA ? "SEMANA o MES" : "MES") + " o acote las fechas");
            }
            porInicio.put(inicio, new Acumulado());
        }
        for (VentasPorDiaProjection d : dias) {
            Acumulado acc = porInicio.get(inicioPeriodo(d.getFecha(), g));
            if (acc == null) continue;   // no ocurre: la consulta ya filtra por el rango
            acc.cantidad += n(d.getCantidad());
            acc.neto = acc.neto.add(nvl(d.getNeto()));
            acc.igv = acc.igv.add(nvl(d.getIgv()));
            acc.total = acc.total.add(nvl(d.getTotal()));
        }
        List<PeriodoVentasDTO> resultado = new ArrayList<>(porInicio.size());
        porInicio.forEach((inicio, acc) -> {
            LocalDate fin = siguientePeriodo(inicio, g).minusDays(1);
            resultado.add(new PeriodoVentasDTO(etiqueta(inicio, g), max(inicio, desde), min(fin, hasta), acc.cantidad,
                    dinero(acc.neto), dinero(acc.igv), dinero(acc.total), promedio(acc.total, acc.cantidad)));
        });
        return resultado;
    }

    static LocalDate inicioPeriodo(LocalDate fecha, AgrupacionReporte g) {
        return switch (g) {
            case DIA -> fecha;
            case SEMANA -> fecha.with(DayOfWeek.MONDAY);
            case MES -> fecha.withDayOfMonth(1);
        };
    }

    private static LocalDate siguientePeriodo(LocalDate inicio, AgrupacionReporte g) {
        return switch (g) {
            case DIA -> inicio.plusDays(1);
            case SEMANA -> inicio.plusWeeks(1);
            case MES -> inicio.plusMonths(1);
        };
    }

    /** 2026-10-08, 2026-W41 (semana ISO) o 2026-10. */
    static String etiqueta(LocalDate inicio, AgrupacionReporte g) {
        return switch (g) {
            case DIA -> inicio.toString();
            case SEMANA -> String.format("%d-W%02d", inicio.get(IsoFields.WEEK_BASED_YEAR), inicio.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            case MES -> YearMonth.from(inicio).toString();
        };
    }

    /** "EFECTIVO 50.00 + YAPE 30.00" por venta, con una sola consulta por lote. */
    private Map<UUID, String> metodosPago(List<Venta> ventas) {
        if (ventas.isEmpty()) return Map.of();
        List<UUID> ids = ventas.stream().map(Venta::getId).toList();
        Map<UUID, Map<MetodoPago, BigDecimal>> porVenta = new HashMap<>();
        for (PagoPorVentaProjection p : pagoRepository.pagosPorVentas(ids)) {
            porVenta.computeIfAbsent(p.getVentaId(), k -> new EnumMap<>(MetodoPago.class)).merge(p.getMetodo(), nvl(p.getTotal()), BigDecimal::add);
        }
        Map<UUID, String> texto = new HashMap<>();
        porVenta.forEach((id, m) -> texto.put(id, m.entrySet().stream()
                .map(e -> e.getKey().name() + " " + dinero(e.getValue()).toPlainString())
                .collect(Collectors.joining(" + "))));
        return texto;
    }

    private static EstadoVenta estado(FiltroVentasReporteDTO f) {
        return f.estado() != null ? f.estado() : EstadoVenta.COMPLETADA;
    }

    // =================================================================== RENTABILIDAD / ESTRELLA

    @Override
    public RentabilidadReporteDTO rentabilidad(LocalDate desde, LocalDate hasta, UUID sedeId, UUID categoriaId) {
        Acceso a = acceso(sedeId);
        RentabilidadReporteDTO completo = construirRentabilidad(rango(desde, hasta), a.sedeId(), categoriaId);
        return a.admin() ? completo : sinCostos(completo);
    }

    @Override
    public ArchivoDescargaDTO exportarRentabilidad(LocalDate desde, LocalDate hasta, UUID sedeId, UUID categoriaId) {
        User admin = exigirAdmin();
        Rango r = rango(desde, hasta);
        RentabilidadReporteDTO rep = construirRentabilidad(r, sedeId, categoriaId);
        exigirTope(rep.items().size(), maxFilas(), "productos");
        StringBuilder csv = ReporteCsv.nuevo(ReporteCsv.COLUMNAS_RENTABILIDAD);
        ReporteCsv.rentabilidad(csv, rep.items());
        Map<String, Object> datos = datos("RENTABILIDAD", r, sedeId, rep.items().size());
        datos.put("categoriaId", texto(categoriaId));
        return archivo(admin, "rentabilidad", csv, datos);
    }

    @Override
    public ProductosEstrellaDTO productosEstrella(LocalDate desde, LocalDate hasta, UUID sedeId, UUID categoriaId,
                                                  CriterioRankingProducto criterio, Integer top) {
        Acceso a = acceso(sedeId);
        CriterioRankingProducto c = criterio != null ? criterio : CriterioRankingProducto.INGRESO;
        if (c == CriterioRankingProducto.MARGEN && !a.admin()) {
            throw new AccessDeniedException("El ranking por margen involucra costos: solo un administrador puede verlo");
        }
        int n = top(top);
        Rango r = rango(desde, hasta);
        RentabilidadReporteDTO base = construirRentabilidad(r, a.sedeId(), categoriaId);

        Comparator<RentabilidadItemDTO> orden = switch (c) {
            case CANTIDAD -> Comparator.comparing(RentabilidadItemDTO::cantidadNeta);
            case INGRESO -> Comparator.comparing(RentabilidadItemDTO::ingresoNetoReal);
            case MARGEN -> Comparator.comparing(RentabilidadItemDTO::margen);
        };
        List<RentabilidadItemDTO> items = base.items().stream()
                .filter(i -> RentabilidadItemDTO.PRODUCTO.equals(i.tipo()))
                .filter(i -> switch (c) {
                    case CANTIDAD -> i.cantidadNeta().signum() > 0;
                    case INGRESO -> i.ingresoNetoReal().signum() > 0;
                    case MARGEN -> i.margen() != null;
                })
                .sorted(orden.reversed().thenComparing(RentabilidadItemDTO::nombre, Comparator.nullsLast(String::compareTo)))
                .limit(n)
                .map(i -> a.admin() ? i : i.sinCostos())
                .toList();
        return new ProductosEstrellaDTO(r.desde(), r.hasta(), a.sedeId(), c, n, items);
    }

    /** Rentabilidad con costos (la vista sin costos se arma después con {@link #sinCostos}). */
    RentabilidadReporteDTO construirRentabilidad(Rango r, UUID sedeId, UUID categoriaId) {
        List<RentabilidadProjection> vendidos = detalleVentaRepository.rentabilidad(r.inicio(), r.fin(), sedeId, categoriaId);
        Map<UUID, DevueltoPorProductoProjection> devueltos = new LinkedHashMap<>();
        for (DevueltoPorProductoProjection d : detalleDevolucionRepository.devueltoPorProducto(r.inicio(), r.fin(), sedeId, categoriaId)) {
            devueltos.put(d.getProductoId(), d);
        }

        List<Parcial> parciales = new ArrayList<>();
        for (RentabilidadProjection v : vendidos) {
            DevueltoPorProductoProjection d = v.getProductoId() != null ? devueltos.remove(v.getProductoId()) : null;
            parciales.add(new Parcial(v.getProductoId() != null ? RentabilidadItemDTO.PRODUCTO : RentabilidadItemDTO.SERVICIO,
                    v.getProductoId() != null ? v.getProductoId() : v.getServicioId(), v.getNombre(), v.getSku(), v.getCategoria(),
                    v.getUnidad(), n(v.getVentas()), cantidad(v.getCantidad()), cantidad(d != null ? d.getCantidad() : null),
                    dinero(v.getIngresoNeto()), dinero(d != null ? d.getMontoNeto() : null),
                    v.getProductoId() != null ? v.getCostoUnitario() : null));
        }
        // Devoluciones del periodo de productos que no se vendieron en el periodo: también restan (como en la Utilidad Neta)
        if (!devueltos.isEmpty()) {
            for (Producto p : productoRepository.findAllById(devueltos.keySet())) {
                DevueltoPorProductoProjection d = devueltos.get(p.getId());
                parciales.add(new Parcial(RentabilidadItemDTO.PRODUCTO, p.getId(), p.getNombre(), p.getCodigoSku(),
                        p.getCategoria() != null ? p.getCategoria().getNombre() : null, p.getUnidad(), 0, BigDecimal.ZERO.setScale(2),
                        cantidad(d.getCantidad()), BigDecimal.ZERO.setScale(2), dinero(d.getMontoNeto()), p.getPrecioCompra()));
            }
        }

        BigDecimal ingresoTotal = parciales.stream().map(Parcial::ingresoReal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal costoTotal = BigDecimal.ZERO, margenTotal = BigDecimal.ZERO, ingresoConCosto = BigDecimal.ZERO;
        long sinCosto = 0, servicios = 0;
        List<RentabilidadItemDTO> items = new ArrayList<>(parciales.size());
        for (Parcial p : parciales) {
            BigDecimal ingresoReal = p.ingresoReal();
            BigDecimal cantidadNeta = p.cantidadVendida().subtract(p.cantidadDevuelta());
            BigDecimal costoUnitario = p.costoUnitario() != null ? p.costoUnitario().setScale(2, RoundingMode.HALF_UP) : null;
            BigDecimal costo = null, margen = null, margenPct = null;
            if (costoUnitario != null) {
                costo = dinero(p.costoUnitario().multiply(cantidadNeta));
                margen = ingresoReal.subtract(costo);
                margenPct = porcentaje(margen, ingresoReal);
                costoTotal = costoTotal.add(costo);
                margenTotal = margenTotal.add(margen);
                ingresoConCosto = ingresoConCosto.add(ingresoReal);
            } else if (RentabilidadItemDTO.SERVICIO.equals(p.tipo())) {
                servicios++;
            } else {
                sinCosto++;
            }
            items.add(new RentabilidadItemDTO(p.tipo(), p.id(), p.nombre(), p.sku(), p.categoria(), p.unidad(), p.ventas(),
                    p.cantidadVendida(), p.cantidadDevuelta(), cantidadNeta, p.ingresoNeto(), p.devolucionesNeto(), ingresoReal,
                    porcentaje(ingresoReal, ingresoTotal), costoUnitario, costo, margen, margenPct));
        }
        items.sort(Comparator.comparing(RentabilidadItemDTO::ingresoNetoReal).reversed()
                .thenComparing(RentabilidadItemDTO::nombre, Comparator.nullsLast(String::compareTo)));

        List<String> advertencias = new ArrayList<>();
        advertencias.add("Costo estimado con el costo promedio ponderado ACTUAL de cada producto: las ventas no guardan el costo del momento");
        if (sinCosto > 0) advertencias.add(sinCosto + " producto(s) sin costo de compra registrado: se excluyen del margen");
        if (servicios > 0) advertencias.add(servicios + " servicio(s) sin costo registrado: se excluyen del margen");

        return new RentabilidadReporteDTO(r.desde(), r.hasta(), sedeId, categoriaId, dinero(ingresoTotal), dinero(costoTotal),
                dinero(margenTotal), porcentaje(margenTotal, ingresoConCosto), true, items, advertencias);
    }

    private static RentabilidadReporteDTO sinCostos(RentabilidadReporteDTO r) {
        List<String> advertencias = r.advertencias().stream().filter(a -> !a.contains("costo")).toList();
        return new RentabilidadReporteDTO(r.desde(), r.hasta(), r.sedeId(), r.categoriaId(), r.ingresoNetoReal(), null, null,
                null, false, r.items().stream().map(RentabilidadItemDTO::sinCostos).toList(), advertencias);
    }

    /** Valores de un producto/servicio antes de calcular costo y margen. */
    private record Parcial(String tipo, UUID id, String nombre, String sku, String categoria, String unidad, long ventas,
                           BigDecimal cantidadVendida, BigDecimal cantidadDevuelta, BigDecimal ingresoNeto,
                           BigDecimal devolucionesNeto, BigDecimal costoUnitario) {
        BigDecimal ingresoReal() {
            return ingresoNeto.subtract(devolucionesNeto);
        }
    }

    // =================================================================== CLIENTES

    @Override
    public ClientesFrecuentesDTO clientesFrecuentes(LocalDate desde, LocalDate hasta, UUID sedeId,
                                                    CriterioRankingCliente criterio, Integer top) {
        Acceso a = acceso(sedeId);
        int n = top(top);
        Rango r = rango(desde, hasta);
        CriterioRankingCliente c = criterio != null ? criterio : CriterioRankingCliente.CANTIDAD;
        Clientes todos = construirClientes(r, a.sedeId(), c);
        return new ClientesFrecuentesDTO(r.desde(), r.hasta(), a.sedeId(), c, n, todos.conCompras(), todos.ventasSinCliente(),
                todos.montoSinCliente(), todos.items().stream().limit(n).toList());
    }

    @Override
    public ArchivoDescargaDTO exportarClientes(LocalDate desde, LocalDate hasta, UUID sedeId, CriterioRankingCliente criterio) {
        User admin = exigirAdmin();
        Rango r = rango(desde, hasta);
        CriterioRankingCliente c = criterio != null ? criterio : CriterioRankingCliente.CANTIDAD;
        Clientes todos = construirClientes(r, sedeId, c);
        exigirTope(todos.items().size(), maxFilas(), "clientes");
        StringBuilder csv = ReporteCsv.nuevo(ReporteCsv.COLUMNAS_CLIENTES);
        ReporteCsv.clientes(csv, todos.items());
        Map<String, Object> datos = datos("CLIENTES", r, sedeId, todos.items().size());
        datos.put("criterio", c.name());
        return archivo(admin, "clientes", csv, datos);
    }

    /** Todos los clientes con compras en el rango, ordenados por el criterio (FRECUENCIA: solo con 2+ compras). */
    Clientes construirClientes(Rango r, UUID sedeId, CriterioRankingCliente c) {
        Map<UUID, MontoCantidadPorIdProjection> devuelto = new HashMap<>();
        for (MontoCantidadPorIdProjection d : devolucionRepository.devueltoPorCliente(r.inicio(), r.fin(), sedeId)) {
            devuelto.put(d.getId(), d);   // HashMap admite la clave null (ventas sin cliente)
        }
        LocalDate hoy = LocalDate.now();
        long ventasSinCliente = 0;
        BigDecimal montoSinCliente = BigDecimal.ZERO;
        List<ClienteFrecuenteDTO> items = new ArrayList<>();
        for (VentasPorClienteProjection v : ventaRepository.ventasPorCliente(r.inicio(), r.fin(), sedeId)) {
            if (v.getClienteId() == null) {
                ventasSinCliente = n(v.getCantidad());
                montoSinCliente = dinero(v.getTotal());
                continue;
            }
            long compras = n(v.getCantidad());
            BigDecimal total = dinero(v.getTotal());
            MontoCantidadPorIdProjection d = devuelto.get(v.getClienteId());
            BigDecimal dev = dinero(d != null ? d.getTotal() : null);
            BigDecimal diasEntre = null;
            if (compras > 1 && v.getPrimeraCompra() != null && v.getUltimaCompra() != null) {
                long minutos = Duration.between(v.getPrimeraCompra(), v.getUltimaCompra()).toMinutes();
                diasEntre = BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(1440L * (compras - 1)), 1, RoundingMode.HALF_UP);
            }
            long diasDesde = v.getUltimaCompra() != null ? Math.max(0, ChronoUnit.DAYS.between(v.getUltimaCompra().toLocalDate(), hoy)) : 0;
            items.add(new ClienteFrecuenteDTO(0, v.getClienteId(), v.getNombre(), v.getTipoDocumento(), v.getNumeroDocumento(),
                    compras, total, dev, dinero(total.subtract(dev)), promedio(total, compras), v.getPrimeraCompra(),
                    v.getUltimaCompra(), diasEntre, diasDesde));
        }
        long conCompras = items.size();

        Comparator<ClienteFrecuenteDTO> porNombre = Comparator.comparing(ClienteFrecuenteDTO::nombre, Comparator.nullsLast(String::compareTo));
        Comparator<ClienteFrecuenteDTO> orden = switch (c) {
            case CANTIDAD -> Comparator.comparingLong(ClienteFrecuenteDTO::compras).reversed()
                    .thenComparing(Comparator.comparing(ClienteFrecuenteDTO::montoNeto).reversed());
            case MONTO -> Comparator.comparing(ClienteFrecuenteDTO::montoNeto).reversed()
                    .thenComparing(Comparator.comparingLong(ClienteFrecuenteDTO::compras).reversed());
            case FRECUENCIA -> Comparator.comparing(ClienteFrecuenteDTO::diasEntreCompras)
                    .thenComparing(Comparator.comparingLong(ClienteFrecuenteDTO::compras).reversed());
        };
        List<ClienteFrecuenteDTO> ordenados = items.stream()
                .filter(i -> c != CriterioRankingCliente.FRECUENCIA || i.diasEntreCompras() != null)
                .sorted(orden.thenComparing(porNombre))
                .toList();
        List<ClienteFrecuenteDTO> conPosicion = new ArrayList<>(ordenados.size());
        for (int i = 0; i < ordenados.size(); i++) {
            ClienteFrecuenteDTO x = ordenados.get(i);
            conPosicion.add(new ClienteFrecuenteDTO(i + 1, x.clienteId(), x.nombre(), x.tipoDocumento(), x.numeroDocumento(),
                    x.compras(), x.montoTotal(), x.devuelto(), x.montoNeto(), x.ticketPromedio(), x.primeraCompra(),
                    x.ultimaCompra(), x.diasEntreCompras(), x.diasDesdeUltimaCompra()));
        }
        return new Clientes(conPosicion, conCompras, ventasSinCliente, montoSinCliente);
    }

    record Clientes(List<ClienteFrecuenteDTO> items, long conCompras, long ventasSinCliente, BigDecimal montoSinCliente) { }

    // =================================================================== PROVEEDORES

    @Override
    public ProveedoresRankingDTO rankingProveedores(LocalDate desde, LocalDate hasta, UUID sedeId,
                                                    CriterioRankingProveedor criterio, Integer top) {
        Acceso a = acceso(sedeId);
        CriterioRankingProveedor c = criterio != null ? criterio : CriterioRankingProveedor.FRECUENCIA;
        if (c == CriterioRankingProveedor.VOLUMEN && !a.admin()) {
            throw new AccessDeniedException("El ranking por volumen involucra montos de compra: solo un administrador puede verlo");
        }
        int n = top(top);
        Rango r = rango(desde, hasta);
        List<ProveedorRankingDTO> todos = construirProveedores(r, a.sedeId(), c);
        long otraMoneda = compraRepository.contarEnOtraMoneda(EstadoCompra.RECIBIDA, Moneda.PEN, r.desde(), r.hasta(), a.sedeId());
        List<String> advertencias = new ArrayList<>();
        advertencias.add("Días de entrega = de la emisión de la orden a su entrada en Kardex (las compras no registran fecha prometida)");
        if (otraMoneda > 0 && a.admin()) {
            advertencias.add(otraMoneda + " compra(s) recibida(s) en otra moneda: no suman al monto (que está en PEN)");
        }
        List<ProveedorRankingDTO> items = todos.stream().limit(n).map(p -> a.admin() ? p : p.sinMontos()).toList();
        return new ProveedoresRankingDTO(r.desde(), r.hasta(), a.sedeId(), c, n, otraMoneda, a.admin(), items, advertencias);
    }

    @Override
    public ArchivoDescargaDTO exportarProveedores(LocalDate desde, LocalDate hasta, UUID sedeId, CriterioRankingProveedor criterio) {
        User admin = exigirAdmin();
        Rango r = rango(desde, hasta);
        CriterioRankingProveedor c = criterio != null ? criterio : CriterioRankingProveedor.FRECUENCIA;
        List<ProveedorRankingDTO> todos = construirProveedores(r, sedeId, c);
        exigirTope(todos.size(), maxFilas(), "proveedores");
        StringBuilder csv = ReporteCsv.nuevo(ReporteCsv.COLUMNAS_PROVEEDORES);
        ReporteCsv.proveedores(csv, todos);
        Map<String, Object> datos = datos("PROVEEDORES", r, sedeId, todos.size());
        datos.put("criterio", c.name());
        return archivo(admin, "proveedores", csv, datos);
    }

    /** Todos los proveedores con compras emitidas en el rango, ordenados (ENTREGA: solo con recepciones). */
    List<ProveedorRankingDTO> construirProveedores(Rango r, UUID sedeId, CriterioRankingProveedor c) {
        LocalDate finExclusivo = r.hasta().plusDays(1);
        Map<UUID, List<Long>> diasPorProveedor = new HashMap<>();
        for (RecepcionCompraProjection rc : compraRepository.recepcionesCompra(r.desde(), finExclusivo, sedeId)) {
            if (rc.getFechaEmision() == null || rc.getFechaRecepcion() == null) continue;
            long dias = Math.max(0, ChronoUnit.DAYS.between(rc.getFechaEmision(), rc.getFechaRecepcion().toLocalDate()));
            diasPorProveedor.computeIfAbsent(rc.getProveedorId(), k -> new ArrayList<>()).add(dias);
        }

        List<ProveedorRankingDTO> items = new ArrayList<>();
        for (ComprasPorProveedorProjection p : compraRepository.comprasPorProveedor(Moneda.PEN, r.desde(), finExclusivo, sedeId)) {
            List<Long> dias = diasPorProveedor.getOrDefault(p.getProveedorId(), List.of());
            BigDecimal promedioDias = dias.isEmpty() ? null : BigDecimal.valueOf(dias.stream().mapToLong(Long::longValue).sum())
                    .divide(BigDecimal.valueOf(dias.size()), 1, RoundingMode.HALF_UP);
            Integer maximo = dias.isEmpty() ? null : (int) dias.stream().mapToLong(Long::longValue).max().orElse(0);
            BigDecimal monto = dinero(p.getMonto());
            items.add(new ProveedorRankingDTO(0, p.getProveedorId(), p.getNombre(), p.getNumeroDocumento(), n(p.getCantidad()),
                    n(p.getRecibidas()), n(p.getPendientes()), n(p.getAnuladas()), monto,
                    promedio(monto, n(p.getRecibidasMonedaBase())), p.getUltimaCompra(), promedioDias, maximo));
        }

        Comparator<ProveedorRankingDTO> orden = switch (c) {
            case VOLUMEN -> Comparator.comparing(ProveedorRankingDTO::montoRecibido).reversed()
                    .thenComparing(Comparator.comparingLong(ProveedorRankingDTO::recibidas).reversed());
            case FRECUENCIA -> Comparator.comparingLong(ProveedorRankingDTO::recibidas).reversed()
                    .thenComparing(Comparator.comparingLong(ProveedorRankingDTO::compras).reversed());
            case ENTREGA -> Comparator.comparing(ProveedorRankingDTO::diasEntregaPromedio)
                    .thenComparing(Comparator.comparingLong(ProveedorRankingDTO::recibidas).reversed());
        };
        List<ProveedorRankingDTO> ordenados = items.stream()
                .filter(p -> c != CriterioRankingProveedor.ENTREGA || p.diasEntregaPromedio() != null)
                .sorted(orden.thenComparing(ProveedorRankingDTO::nombre, Comparator.nullsLast(String::compareTo)))
                .toList();
        List<ProveedorRankingDTO> conPosicion = new ArrayList<>(ordenados.size());
        for (int i = 0; i < ordenados.size(); i++) {
            ProveedorRankingDTO x = ordenados.get(i);
            conPosicion.add(new ProveedorRankingDTO(i + 1, x.proveedorId(), x.nombre(), x.numeroDocumento(), x.compras(),
                    x.recibidas(), x.pendientes(), x.anuladas(), x.montoRecibido(), x.ticketPromedio(), x.ultimaCompra(),
                    x.diasEntregaPromedio(), x.diasEntregaMaximo()));
        }
        return conPosicion;
    }

    // =================================================================== ARQUEOS

    @Override
    public ArqueosReporteDTO arqueos(LocalDate desde, LocalDate hasta, UUID sedeId, UUID usuarioId, EstadoTurno estado,
                                     Pageable pageable) {
        Acceso a = acceso(sedeId);
        UUID usuario = usuarioId;
        if (!a.admin()) {
            if (usuarioId != null && !usuarioId.equals(a.usuario().getId())) {
                throw new AccessDeniedException("Solo puede consultar el arqueo de sus propios turnos");
            }
            usuario = a.usuario().getId();
        }
        Rango r = rango(desde, hasta);
        List<ArqueoTurnoDTO> todos = construirArqueos(r, a.sedeId(), usuario, estado);

        int tamano = pageable.isPaged() ? pageable.getPageSize() : Math.max(1, todos.size());
        int pagina = pageable.isPaged() ? pageable.getPageNumber() : 0;
        int desdeIdx = (int) Math.min((long) pagina * tamano, todos.size());
        int hastaIdx = Math.min(desdeIdx + tamano, todos.size());
        int totalPaginas = (int) Math.ceil(todos.size() / (double) tamano);
        return new ArqueosReporteDTO(r.desde(), r.hasta(), a.sedeId(), usuario, resumen(todos),
                List.copyOf(todos.subList(desdeIdx, hastaIdx)), pagina, tamano, todos.size(), totalPaginas);
    }

    @Override
    public ArchivoDescargaDTO exportarArqueos(LocalDate desde, LocalDate hasta, UUID sedeId, UUID usuarioId, EstadoTurno estado) {
        User admin = exigirAdmin();
        Rango r = rango(desde, hasta);
        List<ArqueoTurnoDTO> todos = construirArqueos(r, sedeId, usuarioId, estado);
        StringBuilder csv = ReporteCsv.nuevo(ReporteCsv.COLUMNAS_ARQUEOS);
        ReporteCsv.arqueos(csv, todos);
        Map<String, Object> datos = datos("ARQUEOS", r, sedeId, todos.size());
        datos.put("usuarioId", texto(usuarioId));
        datos.put("estado", texto(estado));
        return archivo(admin, "arqueos", csv, datos);
    }

    /** Arqueo de todos los turnos abiertos en el rango (tope: máximo de filas de exportación). */
    List<ArqueoTurnoDTO> construirArqueos(Rango r, UUID sedeId, UUID usuarioId, EstadoTurno estado) {
        int max = maxFilas();
        Page<TurnoCaja> turnos = turnoRepository.reporteArqueos(r.inicio(), r.fin(), sedeId, usuarioId, estado, PageRequest.of(0, max));
        exigirTope(turnos.getTotalElements(), max, "turnos");

        Map<UUID, EfectivoPorTurnoProjection> efectivo = new HashMap<>();
        List<UUID> ids = turnos.getContent().stream().map(TurnoCaja::getId).toList();
        for (int i = 0; i < ids.size(); i += LOTE) {
            for (EfectivoPorTurnoProjection e : pagoRepository.efectivoPorTurnos(ids.subList(i, Math.min(i + LOTE, ids.size())))) {
                efectivo.put(e.getTurnoId(), e);
            }
        }
        List<ArqueoTurnoDTO> filas = new ArrayList<>(ids.size());
        for (TurnoCaja t : turnos.getContent()) filas.add(arqueo(t, efectivo.get(t.getId())));
        return filas;
    }

    /** Misma fórmula que TurnoCajaServiceImpl.construirResumen: inicial + efectivo − retiros − reembolsos. */
    static ArqueoTurnoDTO arqueo(TurnoCaja t, EfectivoPorTurnoProjection e) {
        BigDecimal inicial = dinero(t.getMontoInicial());
        BigDecimal ventasEfectivo = dinero(e != null ? e.getTotal() : null);
        BigDecimal retiros = dinero(t.getTotalRetirosSociosSeguro());
        BigDecimal reembolsos = dinero(t.getTotalReembolsosSeguro());
        BigDecimal esperado = inicial.add(ventasEfectivo).subtract(retiros).subtract(reembolsos);
        BigDecimal declarado = t.getMontoFinalDeclarado() != null ? dinero(t.getMontoFinalDeclarado()) : null;
        BigDecimal diferencia = null;
        ResultadoArqueo resultado;
        if (t.getEstado() != EstadoTurno.CERRADO) {
            resultado = ResultadoArqueo.ABIERTO;
        } else if (declarado == null) {
            resultado = ResultadoArqueo.SIN_DECLARAR;
        } else {
            diferencia = declarado.subtract(esperado);
            resultado = diferencia.signum() == 0 ? ResultadoArqueo.CUADRADO
                    : diferencia.signum() < 0 ? ResultadoArqueo.FALTANTE : ResultadoArqueo.SOBRANTE;
        }
        return new ArqueoTurnoDTO(t.getId(), t.getSede() != null ? t.getSede().getNombre() : null,
                nombreCompleto(t.getUser()), t.getFechaApertura(), t.getFechaCierre(), t.getEstado(), inicial,
                ventasEfectivo, retiros, reembolsos, esperado, declarado, diferencia, resultado,
                dinero(e != null ? e.getAnuladas() : null));
    }

    static ResumenArqueosDTO resumen(List<ArqueoTurnoDTO> filas) {
        long abiertos = 0, cerrados = 0, cuadrados = 0, faltantes = 0, sobrantes = 0;
        BigDecimal totalFaltante = BigDecimal.ZERO, totalSobrante = BigDecimal.ZERO, anuladas = BigDecimal.ZERO;
        for (ArqueoTurnoDTO a : filas) {
            if (a.estado() == EstadoTurno.CERRADO) cerrados++;
            else abiertos++;
            anuladas = anuladas.add(a.efectivoVentasAnuladas());
            switch (a.resultado()) {
                case CUADRADO -> cuadrados++;
                case FALTANTE -> {
                    faltantes++;
                    totalFaltante = totalFaltante.add(a.diferencia().negate());
                }
                case SOBRANTE -> {
                    sobrantes++;
                    totalSobrante = totalSobrante.add(a.diferencia());
                }
                default -> { }
            }
        }
        return new ResumenArqueosDTO(filas.size(), abiertos, cerrados, cuadrados, faltantes, sobrantes, totalFaltante,
                totalSobrante, totalSobrante.subtract(totalFaltante), anuladas);
    }

    // =================================================================== COMUNES

    /** Usuario, si es ADMIN y la sede efectiva (MULTIFUNCIONAL: siempre la suya). */
    record Acceso(User usuario, boolean admin, UUID sedeId) { }

    /** Rango inclusivo [desde, hasta] y su versión semiabierta [inicio, fin) para las consultas. */
    record Rango(LocalDate desde, LocalDate hasta, LocalDateTime inicio, LocalDateTime fin) { }

    /** Sin fechas: mes en curso. Solo hasta: desde el día 1 de ese mes. Solo desde: hasta hoy. */
    static Rango rango(LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta != null ? hasta : (desde != null && desde.isAfter(LocalDate.now()) ? desde : LocalDate.now());
        LocalDate d = desde != null ? desde : h.withDayOfMonth(1);
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        return new Rango(d, h, d.atStartOfDay(), h.plusDays(1).atStartOfDay());
    }

    private Acceso acceso(UUID sedeId) {
        User u = usuarioActual();
        boolean admin = esAdmin(u);
        if (admin) return new Acceso(u, true, sedeId);
        UUID propia = u.getSede() != null ? u.getSede().getId() : null;
        if (propia == null) throw new AccessDeniedException("Su usuario no tiene sede asignada");
        if (sedeId != null && !sedeId.equals(propia)) throw new AccessDeniedException("Solo puede consultar reportes de su propia sede");
        return new Acceso(u, false, propia);
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }

    private User exigirAdmin() {
        User u = usuarioActual();
        if (!esAdmin(u)) throw new AccessDeniedException("Solo un administrador puede exportar reportes (incluyen costos)");
        return u;
    }

    private static boolean esAdmin(User u) {
        return u.getRol() != null && u.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    static int top(Integer top) {
        int n = top != null ? top : TOP_DEFAULT;
        if (n < 1 || n > TOP_MAX) throw new IllegalArgumentException("top debe estar entre 1 y " + TOP_MAX);
        return n;
    }

    private int maxFilas() {
        return configuracionService.reportes().maxFilasExportacion();   // Configuración > reportes
    }

    private static void exigirTope(long filas, int max, String que) {
        if (filas > max) {
            throw new IllegalArgumentException("El reporte tiene " + filas + " " + que + " y el máximo es " + max
                    + ". Acote las fechas o los filtros.");
        }
    }

    private Map<String, Object> datos(String reporte, Rango r, UUID sedeId, long filas) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("reporte", reporte);
        d.put("formato", "CSV");
        d.put("desde", r.desde().toString());
        d.put("hasta", r.hasta().toString());
        d.put("sedeId", texto(sedeId));
        d.put("filas", filas);
        return d;
    }

    private ArchivoDescargaDTO archivo(User admin, String nombre, StringBuilder csv, Map<String, Object> datos) {
        auditService.registrar(TABLA_AUDITORIA, admin.getId(), AccionAudit.ACCESS, datos);
        log.info("Reporte {} exportado a CSV por {}: {} filas", nombre, admin.getEmail(), datos.get("filas"));
        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        return new ArchivoDescargaDTO(new ByteArrayResource(bytes),
                "reporte_" + nombre + "_" + LocalDateTime.now().format(FORMATO_ARCHIVO) + ".csv", "text/csv; charset=UTF-8");
    }

    private static Pageable sinOrden(Pageable pageable) {
        return pageable.isPaged() ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) : pageable;
    }

    static BigDecimal dinero(BigDecimal v) {
        return nvl(v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cantidad(BigDecimal v) {
        return nvl(v).setScale(2, RoundingMode.HALF_UP);
    }

    /** total / cantidad a 2 decimales; null si no hay cantidad. */
    static BigDecimal promedio(BigDecimal total, long cantidad) {
        return cantidad > 0 ? nvl(total).divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP) : null;
    }

    /** parte / total × 100 a 2 decimales; null si el total no es positivo. */
    static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
        return total != null && total.signum() > 0
                ? nvl(parte).multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP) : null;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static long n(Long v) {
        return v != null ? v : 0L;
    }

    private static String texto(Object o) {
        return o != null ? o.toString() : null;
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    /** Totales mutables de un periodo de la serie de ventas. */
    private static final class Acumulado {
        long cantidad;
        BigDecimal neto = BigDecimal.ZERO;
        BigDecimal igv = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
    }

    /** "Nombre Apellido" (los métodos estáticos del arqueo no usan el mapper). */
    private static String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
