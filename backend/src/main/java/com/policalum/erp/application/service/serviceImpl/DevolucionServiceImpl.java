package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.AprobarDevolucionRequestDTO;
import com.policalum.erp.application.dto.request.DetalleDevolucionRequestDTO;
import com.policalum.erp.application.dto.request.DevolucionRequestDTO;
import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.RechazarDevolucionRequestDTO;
import com.policalum.erp.application.dto.response.DevolucionDashboardDTO;
import com.policalum.erp.application.dto.response.DevolucionListadoDTO;
import com.policalum.erp.application.dto.response.DevolucionResponseDTO;
import com.policalum.erp.application.mapper.DetalleDevolucionMapper;
import com.policalum.erp.application.mapper.DevolucionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.DevolucionService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoDevolucion;
import com.policalum.erp.domain.enums.EstadoProducto;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.DetalleDevolucion;
import com.policalum.erp.domain.model.DetalleVenta;
import com.policalum.erp.domain.model.Devolucion;
import com.policalum.erp.domain.model.KardexMovimiento;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.DetalleDevolucionRepository;
import com.policalum.erp.infrastructure.repository.DetalleVentaRepository;
import com.policalum.erp.infrastructure.repository.DevolucionRepository;
import com.policalum.erp.infrastructure.repository.KardexRepository;
import com.policalum.erp.infrastructure.repository.ReembolsoRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import com.policalum.erp.infrastructure.repository.projection.DevolucionesPorEstadoProjection;
import com.policalum.erp.infrastructure.repository.projection.TotalesFinancierosProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DevolucionServiceImpl implements DevolucionService {

    private static final String TABLA = "devoluciones";
    static final LocalDateTime FECHA_MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    static final LocalDateTime FECHA_MAX = LocalDateTime.of(2100, 1, 1, 0, 0);
    /** Estados que "consumen" cantidad devolvible de una línea de venta. */
    static final Set<EstadoDevolucion> VIGENTES = EnumSet.of(EstadoDevolucion.PENDIENTE, EstadoDevolucion.APROBADA);

    private final DevolucionRepository devolucionRepository;
    private final DetalleDevolucionRepository detalleRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final KardexRepository kardexRepository;
    private final KardexService kardexService;
    private final CorrelativoService correlativoService;
    private final UserRepository userRepository;
    private final DevolucionMapper mapper;
    private final DetalleDevolucionMapper detalleMapper;
    private final AuditService auditService;

    @Override
    public DevolucionResponseDTO crear(DevolucionRequestDTO r) {
        User actual = usuarioActual();
        // FOR UPDATE sobre la venta: dos devoluciones simultáneas no pueden pasarse del tope
        Venta venta = ventaRepository.findByIdParaActualizar(r.ventaId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + r.ventaId()));
        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
            throw new IllegalStateException("Solo se puede devolver una venta COMPLETADA (estado: " + venta.getEstado() + ")");
        }

        Map<UUID, DetalleVenta> lineasVenta = new HashMap<>();
        for (DetalleVenta dv : detalleVentaRepository.findByVenta_Id(venta.getId())) lineasVenta.put(dv.getId(), dv);

        Set<UUID> vistas = new HashSet<>();
        List<DetalleDevolucion> detalles = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleDevolucionRequestDTO l : r.lineas()) {
            if (!vistas.add(l.detalleVentaId())) {
                throw new IllegalArgumentException("La línea " + l.detalleVentaId() + " está repetida; agrupe la cantidad en una sola");
            }
            DetalleVenta dv = lineasVenta.get(l.detalleVentaId());
            if (dv == null) {
                throw new IllegalArgumentException("La línea " + l.detalleVentaId() + " no pertenece a la venta " + venta.getSerieCorrelativo());
            }
            if (dv.getServicio() != null || dv.getProducto() == null) {
                throw new IllegalArgumentException("Los servicios no se pueden devolver (línea " + dv.getId() + ")");
            }
            if (l.cantidad() == null || l.cantidad().signum() <= 0) {
                throw new IllegalArgumentException("La cantidad a devolver debe ser mayor a cero");
            }
            BigDecimal yaDevuelto = cero(detalleRepository.sumarCantidadDevuelta(dv.getId(), VIGENTES));
            BigDecimal disponible = dv.getCantidad().subtract(yaDevuelto);
            if (l.cantidad().compareTo(disponible) > 0) {
                throw new IllegalArgumentException("De " + dv.getProducto().getNombre() + " solo quedan "
                        + disponible.stripTrailingZeros().toPlainString() + " por devolver (vendido "
                        + dv.getCantidad().stripTrailingZeros().toPlainString() + ")");
            }
            BigDecimal factor = dv.getFraccion() != null && dv.getFraccion().getFactorDescuentoKardex() != null
                    ? dv.getFraccion().getFactorDescuentoKardex() : BigDecimal.ONE;
            BigDecimal subtotal = dv.getPrecioUnitarioConIgv().multiply(l.cantidad()).setScale(2, RoundingMode.HALF_UP);
            total = total.add(subtotal);
            detalles.add(DetalleDevolucion.builder()
                    .detalleVenta(dv).producto(dv.getProducto()).fraccion(dv.getFraccion())
                    .cantidadDevuelta(l.cantidad())
                    .cantidadStock(l.cantidad().multiply(factor).setScale(4, RoundingMode.HALF_UP))
                    .precioUnitarioRef(dv.getPrecioUnitarioConIgv())
                    .subtotal(subtotal)
                    .estadoProducto(l.estadoProducto())
                    .observacion(l.observacion() == null || l.observacion().isBlank() ? null : l.observacion().trim())
                    .build());
        }

        Devolucion dev = Devolucion.builder()
                .codigo(generarCodigo())
                .venta(venta).sede(venta.getSede()).cliente(venta.getCliente())
                .solicitadoPor(actual)
                .motivo(r.motivo().trim())
                .estado(EstadoDevolucion.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .montoTotal(total)
                .build();
        Devolucion guardada = devolucionRepository.save(dev);
        detalles.forEach(d -> d.setDevolucion(guardada));
        detalleRepository.saveAll(detalles);

        Map<String, Object> datos = datos(guardada, "SOLICITUD");
        datos.put("lineas", detalles.size());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.INSERT, datos);
        log.info("Devolución {} creada para venta {} - S/ {}", guardada.getCodigo(), venta.getSerieCorrelativo(), total);
        return respuesta(guardada, detalles);
    }

    @Override
    @Transactional(readOnly = true)
    public DevolucionResponseDTO obtener(UUID id) {
        Devolucion d = devolucionRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));
        return respuesta(d, detalleRepository.findByDevolucion_Id(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DevolucionListadoDTO> buscar(UUID sedeId, EstadoDevolucion estado, UUID ventaId,
                                             LocalDate desde, LocalDate hasta, Pageable pageable) {
        validarRango(desde, hasta);
        return devolucionRepository.search(sedeId, estado, ventaId, inicio(desde), finExclusivo(hasta), pageable)
                .map(mapper::toListadoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevolucionListadoDTO> porVenta(UUID ventaId) {
        if (!ventaRepository.existsById(ventaId)) throw new ResourceNotFoundException("Venta no encontrada: " + ventaId);
        return devolucionRepository.findByVenta_IdOrderByFechaSolicitudDesc(ventaId).stream()
                .map(mapper::toListadoDto).toList();
    }

    @Override
    public DevolucionResponseDTO aprobar(UUID id, AprobarDevolucionRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);
        Devolucion dev = bloquear(id);
        if (dev.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new IllegalStateException("Solo se puede aprobar una devolución PENDIENTE (estado: " + dev.getEstado() + ")");
        }
        Venta venta = ventaRepository.findByIdParaActualizar(dev.getVenta().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada"));
        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
            throw new IllegalStateException("La venta " + venta.getSerieCorrelativo() + " ya no está COMPLETADA");
        }

        // Almacén de origen de cada producto = el de la salida de Kardex de la venta
        Map<UUID, Almacen> almacenPorProducto = new HashMap<>();
        for (KardexMovimiento m : kardexRepository.findByReferenciaIdAndTipoReferencia(venta.getId(), TipoReferencia.VENTA)) {
            if (m.getTipoMovimiento() == TipoMovimiento.SALIDA && m.getProducto() != null) {
                almacenPorProducto.putIfAbsent(m.getProducto().getId(), m.getAlmacen());
            }
        }

        List<DetalleDevolucion> detalles = detalleRepository.findByDevolucion_Id(dev.getId());
        BigDecimal reingresado = BigDecimal.ZERO, merma = BigDecimal.ZERO;
        for (DetalleDevolucion d : detalles) {
            Almacen almacen = almacenPorProducto.get(d.getProducto().getId());
            if (almacen == null) {
                throw new IllegalStateException("No se encontró la salida de Kardex de " + d.getProducto().getNombre()
                        + " en la venta " + venta.getSerieCorrelativo() + "; no se puede aprobar de forma segura");
            }
            d.setAlmacen(almacen);
            BigDecimal cantidad = d.getCantidadStock();
            kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(d.getProducto(), almacen, dev.getId(),
                    TipoReferencia.DEVOLUCION, TipoMovimiento.ENTRADA, cantidad, BigDecimal.ZERO, admin,
                    "Devolución " + dev.getCodigo() + " (" + d.getEstadoProducto() + ") de venta " + venta.getSerieCorrelativo()));
            if (d.getEstadoProducto() == EstadoProducto.BUENO) {
                reingresado = reingresado.add(cantidad);
            } else {
                kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(d.getProducto(), almacen, dev.getId(),
                        TipoReferencia.MERMA, TipoMovimiento.SALIDA, cantidad, BigDecimal.ZERO, admin,
                        "Merma por devolución " + dev.getCodigo() + ": producto " + d.getEstadoProducto()));
                merma = merma.add(cantidad);
            }
        }
        detalleRepository.saveAll(detalles);

        dev.setEstado(EstadoDevolucion.APROBADA);
        dev.setAprobadoPor(admin);
        dev.setFechaResolucion(LocalDateTime.now());
        dev.setObservacionAdmin(request == null || request.observacion() == null || request.observacion().isBlank()
                ? null : request.observacion().trim());
        Devolucion guardada = devolucionRepository.save(dev);

        Map<String, Object> datos = datos(guardada, "APROBACION");
        datos.put("unidadesReingresadas", reingresado);
        datos.put("unidadesMerma", merma);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datos);
        log.info("Devolución {} APROBADA: reingreso {} / merma {}", guardada.getCodigo(), reingresado, merma);
        return respuesta(guardada, detalles);
    }

    @Override
    public DevolucionResponseDTO rechazar(UUID id, RechazarDevolucionRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);
        if (request == null || request.motivo() == null || request.motivo().isBlank()) {
            throw new IllegalArgumentException("El motivo del rechazo es obligatorio");
        }
        Devolucion dev = bloquear(id);
        if (dev.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new IllegalStateException("Solo se puede rechazar una devolución PENDIENTE (estado: " + dev.getEstado() + ")");
        }
        dev.setEstado(EstadoDevolucion.RECHAZADA);
        dev.setAprobadoPor(admin);
        dev.setFechaResolucion(LocalDateTime.now());
        dev.setObservacionAdmin(request.motivo().trim());
        Devolucion guardada = devolucionRepository.save(dev);

        Map<String, Object> datos = datos(guardada, "RECHAZO");
        datos.put("motivo", guardada.getObservacionAdmin());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datos);
        return respuesta(guardada, detalleRepository.findByDevolucion_Id(guardada.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public DevolucionDashboardDTO dashboard(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin(usuarioActual());
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        validarRango(d, h);
        LocalDateTime ini = d.atStartOfDay(), fin = h.plusDays(1).atStartOfDay();

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoDevolucion e : EstadoDevolucion.values()) porEstado.put(e.name(), 0L);
        for (DevolucionesPorEstadoProjection p : devolucionRepository.totalesPorEstado(sedeId, ini, fin)) {
            porEstado.put(p.getEstado().name(), p.getCantidad() != null ? p.getCantidad() : 0L);
        }
        BigDecimal aprobado = cero(devolucionRepository.sumarMontoResuelto(EstadoDevolucion.APROBADA, sedeId, ini, fin));
        TotalesFinancierosProjection ventas = ventaRepository.totalesPorPeriodo(EstadoVenta.COMPLETADA, ini, fin, sedeId);
        BigDecimal totalVentas = ventas != null ? cero(ventas.getTotal()) : BigDecimal.ZERO;
        BigDecimal tasa = totalVentas.signum() > 0
                ? aprobado.multiply(BigDecimal.valueOf(100)).divide(totalVentas, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        return new DevolucionDashboardDTO(sedeId, d, h, porEstado, aprobado, totalVentas, tasa,
                reembolsoRepository.contarPendientes(sedeId),
                cero(reembolsoRepository.sumarPendientes(sedeId)),
                cero(reembolsoRepository.sumarProcesados(sedeId, ini, fin)),
                cero(detalleRepository.sumarUnidadesAprobadas(EnumSet.of(EstadoProducto.BUENO), sedeId, ini, fin)),
                cero(detalleRepository.sumarUnidadesAprobadas(EnumSet.of(EstadoProducto.DAÑADO, EstadoProducto.USADO), sedeId, ini, fin)));
    }

    // ============ HELPERS ============

    private String generarCodigo() {
        int anio = LocalDate.now().getYear();
        return String.format("DEV-%d-%06d", anio, correlativoService.siguiente("DEV-" + anio));
    }

    private DevolucionResponseDTO respuesta(Devolucion d, List<DetalleDevolucion> detalles) {
        return mapper.toResponseDto(d, detalles.stream().map(detalleMapper::toResponseDto).toList(),
                d.getId() == null ? null : reembolsoRepository.findFirstByDevolucion_Id(d.getId()).orElse(null));
    }

    private Devolucion bloquear(UUID id) {
        return devolucionRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + id));
    }

    private Map<String, Object> datos(Devolucion d, String operacion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operacion", operacion);
        m.put("codigo", d.getCodigo());
        m.put("estado", d.getEstado() != null ? d.getEstado().name() : null);
        m.put("montoTotal", d.getMontoTotal());
        if (d.getVenta() != null) m.put("ventaId", d.getVenta().getId().toString());
        return m;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private LocalDateTime inicio(LocalDate d) {
        return d != null ? d.atStartOfDay() : FECHA_MIN;
    }

    private LocalDateTime finExclusivo(LocalDate h) {
        return h != null ? h.plusDays(1).atStartOfDay() : FECHA_MAX;
    }

    private static BigDecimal cero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void exigirAdmin(User user) {
        if (user.getRol() == null || user.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede resolver devoluciones o ver su dashboard");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
