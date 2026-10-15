package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.VentaService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import java.math.*;
import java.util.*;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VentaServiceImpl implements VentaService {
    private static final BigDecimal IGV = new BigDecimal("0.18");
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleRepository;
    private final PagoVentaRepository pagoRepository;
    private final SedeRepository sedeRepository;
    private final AlmacenRepository almacenRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final CatalogoFraccionRepository fraccionRepository;
    private final TurnoCajaRepository turnoRepository;
    private final UserRepository userRepository;
    private final KardexService kardexService;
    private final KardexRepository kardexRepository;
    private final com.policalum.erp.infrastructure.repository.DevolucionRepository devolucionRepository; // Módulo 17

    @Override @Transactional
    public VentaProcesadaResponseDTO registrar(RegistrarVentaRequestDTO r) {
        User user = usuarioActual();
        Sede sede = sedeRepository.findById(r.sedeId()).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        Almacen almacen = almacenRepository.findById(r.almacenId()).orElseThrow(() -> new ResourceNotFoundException("Almacén no encontrado"));
        if (!almacen.getSede().getId().equals(sede.getId())) throw new IllegalArgumentException("El almacén no pertenece a la sede indicada");
        TurnoCaja turno = turnoRepository.findById(r.turnoCajaId()).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        if (turno.getEstado() != EstadoTurno.ABIERTO || !turno.getUser().getId().equals(user.getId()) || !turno.getSede().getId().equals(sede.getId()))
            throw new IllegalStateException("La venta requiere un turno abierto del usuario en la sede indicada");
        Cliente cliente = r.clienteId() == null ? null : clienteRepository.findById(r.clienteId()).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado"));

        Venta venta = Venta.builder().sede(sede).user(user).cliente(cliente).turnoCaja(turno)
                .tipoComprobante(r.tipoComprobante()).canalVenta(r.canalVenta() == null ? CanalVenta.LOCAL : r.canalVenta())
                .cotizacionId(r.cotizacionId()).preformaId(r.preformaId()).referenciaPedidoOnline(r.referenciaPedidoOnline())
                .estado(EstadoVenta.COMPLETADA).estadoSunat(EstadoSunat.PENDIENTE).serieCorrelativo("INT-" + UUID.randomUUID()).total(BigDecimal.ZERO).build();
        venta = ventaRepository.save(venta);

        BigDecimal subtotal = BigDecimal.ZERO, igv = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (LineaVentaCommandDTO linea : r.lineas()) {
            LineaCalculada c = calcularLinea(linea);
            DetalleVenta detalle = DetalleVenta.builder().venta(venta).producto(c.producto).fraccion(c.fraccion).servicio(c.servicio)
                    .cantidad(linea.cantidad()).precioUnitarioConIgv(c.precio).valorUnitarioNeto(c.neto)
                    .igvLinea(c.igv).porcentajeIgv(new BigDecimal("18.00")).subtotal(c.total).build();
            detalleRepository.save(detalle);
            subtotal = subtotal.add(c.neto); igv = igv.add(c.igv); total = total.add(c.total);
            if (c.producto != null) {
                BigDecimal salida = linea.cantidad().multiply(c.fraccion == null ? BigDecimal.ONE : c.fraccion.getFactorDescuentoKardex());
                kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(c.producto, almacen, venta.getId(), TipoReferencia.VENTA,
                        TipoMovimiento.SALIDA, salida, BigDecimal.ZERO, user, "Salida automática por venta " + venta.getSerieCorrelativo()));
            }
        }
        BigDecimal pagos = r.pagos().stream().map(PagoVentaCommandDTO::monto).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (pagos.compareTo(total.setScale(2, RoundingMode.HALF_UP)) != 0) throw new IllegalArgumentException("La suma de pagos debe coincidir con el total de la venta");
        for (PagoVentaCommandDTO pago : r.pagos()) pagoRepository.save(PagoVenta.builder().venta(venta).metodoPago(pago.metodoPago()).monto(pago.monto()).referencia(pago.referencia()).build());
        venta.setSubtotalNeto(subtotal.setScale(2, RoundingMode.HALF_UP)); venta.setTotalIgv(igv.setScale(2, RoundingMode.HALF_UP)); venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        ventaRepository.save(venta);
        return new VentaProcesadaResponseDTO(venta.getId(), venta.getSerieCorrelativo(), venta.getSubtotalNeto(), venta.getTotalIgv(), venta.getTotal(), venta.getEstado().name());
    }

    @Override @Transactional(readOnly = true)
    public VentaDetalleResponseDTO obtener(UUID id) {
        Venta venta = ventaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        return detalle(venta);
    }

    @Override @Transactional(readOnly = true)
    public Page<VentaListadoResponseDTO> buscar(UUID sedeId, UUID userId, UUID clienteId, EstadoVenta estado,
                                                  CanalVenta canal, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        return ventaRepository.buscar(sedeId, userId, clienteId, estado, canal, desde, hasta, pageable).map(v ->
                new VentaListadoResponseDTO(v.getId(), v.getSerieCorrelativo(), v.getEstado(), v.getCanalVenta(),
                        v.getTipoComprobante(), v.getTotal(), v.getCliente() == null ? null : v.getCliente().getId(),
                        v.getCliente() == null ? null : v.getCliente().getNombre(), v.getUser().getId(), v.getCreatedAt()));
    }

    @Override @Transactional
    public VentaProcesadaResponseDTO anular(UUID id, AnularVentaRequestDTO request) {
        Venta venta = ventaRepository.findByIdParaActualizar(id).orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        if (venta.getEstado() == EstadoVenta.ANULADA) throw new IllegalStateException("La venta ya está anulada");
        // Módulo 17: anular reingresa TODA la salida; con devoluciones vigentes el stock entraría dos veces
        if (devolucionRepository.existsByVenta_IdAndEstadoIn(venta.getId(),
                java.util.EnumSet.of(com.policalum.erp.domain.enums.EstadoDevolucion.PENDIENTE,
                        com.policalum.erp.domain.enums.EstadoDevolucion.APROBADA)))
            throw new IllegalStateException("La venta tiene devoluciones pendientes o aprobadas; no se puede anular. Gestione la devolución en su lugar.");
        User usuario = usuarioActual();
        boolean esAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!esAdmin && !venta.getUser().getId().equals(usuario.getId())) throw new IllegalStateException("Solo el vendedor responsable o un administrador puede anular la venta");
        List<KardexMovimiento> movimientos = kardexRepository.findByReferenciaIdAndTipoReferencia(venta.getId(), TipoReferencia.VENTA);
        if (movimientos.isEmpty()) throw new IllegalStateException("No se encontró la salida de Kardex de la venta; no se puede anular de forma segura");
        for (KardexMovimiento movimiento : movimientos) {
            if (movimiento.getTipoMovimiento() != TipoMovimiento.SALIDA) continue;
            kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(movimiento.getProducto(), movimiento.getAlmacen(), venta.getId(),
                    TipoReferencia.DEVOLUCION, TipoMovimiento.ENTRADA, movimiento.getCantidadMovida(), BigDecimal.ZERO, usuario,
                    "Reversión por anulación de venta " + venta.getSerieCorrelativo() + ": " + request.motivo().trim()));
        }
        venta.setEstado(EstadoVenta.ANULADA);
        ventaRepository.save(venta);
        return new VentaProcesadaResponseDTO(venta.getId(), venta.getSerieCorrelativo(), venta.getSubtotalNeto(), venta.getTotalIgv(), venta.getTotal(), venta.getEstado().name());
    }

    private VentaDetalleResponseDTO detalle(Venta v) {
        List<LineaVentaResponseDTO> lineas = detalleRepository.findByVenta_Id(v.getId()).stream().map(d -> new LineaVentaResponseDTO(
                d.getId(), d.getProducto() == null ? null : d.getProducto().getId(), d.getProducto() == null ? null : d.getProducto().getNombre(),
                d.getFraccion() == null ? null : d.getFraccion().getId(), d.getFraccion() == null ? null : d.getFraccion().getNombreFraccion(),
                d.getServicio() == null ? null : d.getServicio().getId(), d.getServicio() == null ? null : d.getServicio().getNombre(),
                d.getCantidad(), d.getPrecioUnitarioConIgv(), d.getSubtotal())).toList();
        List<PagoVentaDetalleResponseDTO> pagos = pagoRepository.findByVenta_Id(v.getId()).stream()
                .map(p -> new PagoVentaDetalleResponseDTO(p.getId(), p.getMetodoPago(), p.getMonto(), p.getReferencia())).toList();
        String nombreCliente = v.getCliente() == null ? null : v.getCliente().getNombre();
        return new VentaDetalleResponseDTO(v.getId(), v.getSede().getId(), v.getUser().getId(), v.getUser().getNombre() + " " + v.getUser().getApellido(),
                v.getCliente() == null ? null : v.getCliente().getId(), nombreCliente, v.getTurnoCaja() == null ? null : v.getTurnoCaja().getId(),
                v.getTipoComprobante(), v.getSerieCorrelativo(), v.getEstado(), v.getCanalVenta(), v.getEstadoSunat(), v.getSubtotalNeto(),
                v.getTotalIgv(), v.getTotal(), v.getCreatedAt(), lineas, pagos);
    }
    private LineaCalculada calcularLinea(LineaVentaCommandDTO l) {
        boolean producto = l.productoId() != null, servicio = l.servicioId() != null;
        if (producto == servicio) throw new IllegalArgumentException("Cada línea debe indicar exactamente un producto o un servicio");
        Producto p = null; Servicio s = null; CatalogoFraccion f = null; BigDecimal precio;
        if (producto) { p = productoRepository.findById(l.productoId()).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            if (!Boolean.TRUE.equals(p.getActivo())) throw new IllegalArgumentException("El producto está inactivo");
            if (l.fraccionId() != null) { f = fraccionRepository.findWithProductoById(l.fraccionId()).orElseThrow(() -> new ResourceNotFoundException("Fracción no encontrada"));
                if (!f.getProducto().getId().equals(p.getId()) || !Boolean.TRUE.equals(f.getActivo())) throw new IllegalArgumentException("La fracción no pertenece al producto o está inactiva"); precio = f.getPrecioVenta();
            } else { if (Boolean.TRUE.equals(p.getEsFraccionable())) throw new IllegalArgumentException("Seleccione una fracción para este producto"); precio = p.getPrecioVentaBase(); }
        } else { if (l.fraccionId() != null) throw new IllegalArgumentException("Un servicio no admite fracción"); s = servicioRepository.findById(l.servicioId()).orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado")); if (!Boolean.TRUE.equals(s.getActivo())) throw new IllegalArgumentException("El servicio está inactivo"); precio = s.getPrecio(); }
        BigDecimal total = precio.multiply(l.cantidad()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.divide(BigDecimal.ONE.add(IGV), 4, RoundingMode.HALF_UP);
        return new LineaCalculada(p, s, f, precio, neto, total.subtract(neto).setScale(2, RoundingMode.HALF_UP), total);
    }
    private User usuarioActual() { String email = SecurityContextHolder.getContext().getAuthentication().getName(); return userRepository.findByEmailAndActivoTrue(email).orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido")); }
    private record LineaCalculada(Producto producto, Servicio servicio, CatalogoFraccion fraccion, BigDecimal precio, BigDecimal neto, BigDecimal igv, BigDecimal total) { }
}
