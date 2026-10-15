package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.CancelarPreformaRequestDTO;
import com.policalum.erp.application.dto.request.ConfirmarPreformaRequestDTO;
import com.policalum.erp.application.dto.request.DetallePreformaRequestDTO;
import com.policalum.erp.application.dto.request.LineaVentaCommandDTO;
import com.policalum.erp.application.dto.request.PagoVentaCommandDTO;
import com.policalum.erp.application.dto.request.PreformaVentaRequestDTO;
import com.policalum.erp.application.dto.request.RegistrarVentaRequestDTO;
import com.policalum.erp.application.dto.response.ConfirmacionPreformaDTO;
import com.policalum.erp.application.dto.response.ParametrosVentasDTO;
import com.policalum.erp.application.dto.response.PreformaDashboardDTO;
import com.policalum.erp.application.dto.response.PreformaListadoDTO;
import com.policalum.erp.application.dto.response.PreformaVentaResponseDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.mapper.DetallePreformaMapper;
import com.policalum.erp.application.mapper.PreformaVentaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.PreformaService;
import com.policalum.erp.application.service.VentaService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoPreforma;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Cliente;
import com.policalum.erp.domain.model.DetallePreforma;
import com.policalum.erp.domain.model.PreformaVenta;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.Servicio;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CatalogoFraccionRepository;
import com.policalum.erp.infrastructure.repository.ClienteRepository;
import com.policalum.erp.infrastructure.repository.DetallePreformaRepository;
import com.policalum.erp.infrastructure.repository.PreformaVentaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.ServicioRepository;
import com.policalum.erp.infrastructure.repository.TurnoCajaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import com.policalum.erp.infrastructure.repository.projection.PreformasPorEstadoProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class PreformaServiceImpl implements PreformaService {

    private static final String TABLA = "preformas_venta";
    private static final BigDecimal IGV = new BigDecimal("0.18");
    private static final BigDecimal IGV_PORCENTAJE = new BigDecimal("18.00");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PreformaVentaRepository preformaRepository;
    private final DetallePreformaRepository detalleRepository;
    private final SedeRepository sedeRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final CatalogoFraccionRepository fraccionRepository;
    private final ServicioRepository servicioRepository;
    private final TurnoCajaRepository turnoRepository;
    private final VentaRepository ventaRepository;
    private final UserRepository userRepository;
    private final VentaService ventaService;
    private final CorrelativoService correlativoService;
    private final PreformaVentaMapper mapper;
    private final DetallePreformaMapper detalleMapper;
    private final AuditService auditService;
    private final ConfiguracionService configuracionService;

    // ============ CRUD ============

    @Override
    public PreformaVentaResponseDTO crear(PreformaVentaRequestDTO r) {
        User actual = usuarioActual();
        int minutos = resolverVigencia(r.minutosVigencia(), configuracionService.ventas());
        Sede sede = sedeActiva(r.sedeId());
        Cliente cliente = cliente(r.clienteId());
        List<Calculada> lineas = calcularTodas(r.lineas());

        PreformaVenta p = PreformaVenta.builder()
                .codigo(generarCodigo())
                .sede(sede).user(actual).cliente(cliente)
                .estado(EstadoPreforma.ABIERTA)
                .fechaExpiracion(LocalDateTime.now().plusMinutes(minutos))
                .observaciones(blankANull(r.observaciones()))
                .build();
        aplicarTotales(p, lineas);
        PreformaVenta guardada = preformaRepository.save(p);
        List<DetallePreforma> detalles = guardarLineas(guardada, lineas);

        auditService.registrar(TABLA, guardada.getId(), AccionAudit.INSERT, datos(guardada, "CREACION"));
        log.info("Preforma {} creada: S/ {} - vence {}", guardada.getCodigo(), guardada.getTotalEstimado(),
                guardada.getFechaExpiracion().format(FMT));
        return respuesta(guardada, detalles);
    }

    @Override
    public PreformaVentaResponseDTO obtener(UUID id) {
        PreformaVenta p = preformaRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preforma no encontrada: " + id));
        expirarSiVencida(p);
        return respuesta(p, detalleRepository.findByPreforma_Id(id));
    }

    @Override
    public PreformaVentaResponseDTO actualizar(UUID id, PreformaVentaRequestDTO r) {
        User actual = usuarioActual();
        PreformaVenta p = bloquear(id);
        exigirCreadorOAdmin(actual, p, "editar");
        exigirAbiertaVigente(p, "editar");

        Sede sede = sedeActiva(r.sedeId());
        List<Calculada> lineas = calcularTodas(r.lineas());
        BigDecimal totalAnterior = p.getTotalEstimado();

        p.setSede(sede);
        p.setCliente(cliente(r.clienteId()));
        p.setObservaciones(blankANull(r.observaciones()));
        if (r.minutosVigencia() != null) {
            p.setFechaExpiracion(LocalDateTime.now().plusMinutes(resolverVigencia(r.minutosVigencia(), configuracionService.ventas())));
        }
        aplicarTotales(p, lineas);
        detalleRepository.eliminarPorPreforma(p.getId());
        List<DetallePreforma> detalles = guardarLineas(p, lineas);
        PreformaVenta guardada = preformaRepository.save(p);

        Map<String, Object> d = datos(guardada, "EDICION");
        d.put("totalAnterior", totalAnterior);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        return respuesta(guardada, detalles);
    }

    @Override
    public void eliminar(UUID id) {
        User actual = usuarioActual();
        PreformaVenta p = bloquear(id);
        exigirCreadorOAdmin(actual, p, "eliminar");
        if (p.getEstado() != EstadoPreforma.ABIERTA) {
            throw new IllegalStateException("Solo se puede eliminar una preforma ABIERTA (estado: " + p.getEstado() + ")");
        }
        Map<String, Object> d = datos(p, "ELIMINACION");
        detalleRepository.eliminarPorPreforma(p.getId());
        preformaRepository.delete(p);
        auditService.registrar(TABLA, id, AccionAudit.DELETE, d);
        log.info("Preforma {} eliminada", p.getCodigo());
    }

    // ============ CONFIRMAR / CANCELAR ============

    @Override
    public ConfirmacionPreformaDTO confirmar(UUID id, ConfirmarPreformaRequestDTO request) {
        User actual = usuarioActual();
        PreformaVenta p = bloquear(id); // evita doble confirmación (dos cajeros a la vez)
        exigirAbiertaVigente(p, "confirmar");

        TurnoCaja turno = turnoRepository.findByUser_IdAndSede_IdAndEstado(actual.getId(), p.getSede().getId(), EstadoTurno.ABIERTO)
                .orElseThrow(() -> new IllegalStateException("Para confirmar debe tener un turno de caja ABIERTO en la sede "
                        + p.getSede().getNombre()));

        List<DetallePreforma> detalles = detalleRepository.findByPreforma_Id(p.getId());
        if (detalles.isEmpty()) throw new IllegalStateException("La preforma no tiene líneas");

        // Los precios pueden haber cambiado en el catálogo: si el total ya no coincide, no se cobra
        BigDecimal totalActual = BigDecimal.ZERO;
        for (DetallePreforma d : detalles) {
            totalActual = totalActual.add(calcular(comando(d)).total());
        }
        if (totalActual.compareTo(p.getTotalEstimado()) != 0) {
            throw new IllegalStateException("Los precios cambiaron: la preforma dice S/ " + p.getTotalEstimado().toPlainString()
                    + " pero hoy el total es S/ " + totalActual.toPlainString() + ". Edite la preforma para actualizarla.");
        }
        BigDecimal pagos = request.pagos().stream().map(PagoVentaCommandDTO::monto)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (pagos.compareTo(p.getTotalEstimado()) != 0) {
            throw new IllegalArgumentException("Los pagos (S/ " + pagos.toPlainString() + ") deben sumar el total de la preforma (S/ "
                    + p.getTotalEstimado().toPlainString() + ")");
        }

        RegistrarVentaRequestDTO ventaRequest = new RegistrarVentaRequestDTO(
                p.getSede().getId(), request.almacenId(),
                p.getCliente() != null ? p.getCliente().getId() : null,
                turno.getId(), request.tipoComprobante(),
                request.canalVenta() != null ? request.canalVenta() : CanalVenta.LOCAL,
                p.getCotizacion() != null ? p.getCotizacion().getId() : null,
                p.getId(), null,
                detalles.stream().map(d -> new LineaVentaCommandDTO(
                        d.getProducto() != null ? d.getProducto().getId() : null,
                        d.getFraccion() != null ? d.getFraccion().getId() : null,
                        d.getServicio() != null ? d.getServicio().getId() : null,
                        d.getCantidad())).toList(),
                request.pagos());
        VentaProcesadaResponseDTO venta = ventaService.registrar(ventaRequest);
        Venta ventaEntity = ventaRepository.findById(venta.id())
                .orElseThrow(() -> new ResourceNotFoundException("Venta recién creada no encontrada: " + venta.id()));

        p.setEstado(EstadoPreforma.CONFIRMADA);
        p.setVenta(ventaEntity);
        p.setTurnoCaja(turno);
        p.setConfirmadoPor(actual);
        p.setFechaConfirmacion(LocalDateTime.now());
        PreformaVenta guardada = preformaRepository.save(p);

        Map<String, Object> d = datos(guardada, "CONFIRMACION");
        d.put("ventaId", venta.id().toString());
        d.put("ventaSerie", venta.serieCorrelativo());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        log.info("Preforma {} confirmada → venta {}", guardada.getCodigo(), venta.serieCorrelativo());
        return new ConfirmacionPreformaDTO(respuesta(guardada, detalles), venta);
    }

    @Override
    public PreformaVentaResponseDTO cancelar(UUID id, CancelarPreformaRequestDTO request) {
        User actual = usuarioActual();
        PreformaVenta p = bloquear(id);
        exigirCreadorOAdmin(actual, p, "cancelar");
        exigirAbiertaVigente(p, "cancelar");

        p.setEstado(EstadoPreforma.CANCELADA);
        p.setCanceladoPor(actual);
        p.setFechaCancelacion(LocalDateTime.now());
        p.setMotivoCancelacion(request == null ? null : blankANull(request.motivo()));
        PreformaVenta guardada = preformaRepository.save(p);

        Map<String, Object> d = datos(guardada, "CANCELACION");
        if (guardada.getMotivoCancelacion() != null) d.put("motivo", guardada.getMotivoCancelacion());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        return respuesta(guardada, detalleRepository.findByPreforma_Id(guardada.getId()));
    }

    // ============ CONSULTAS ============

    @Override
    public List<PreformaListadoDTO> abiertasDelUsuario() {
        User actual = usuarioActual();
        List<PreformaListadoDTO> vigentes = new ArrayList<>();
        for (PreformaVenta p : preformaRepository.findByUser_IdAndEstadoOrderByFechaExpiracionAsc(actual.getId(), EstadoPreforma.ABIERTA)) {
            if (!expirarSiVencida(p)) vigentes.add(mapper.toListadoDto(p));
        }
        return vigentes;
    }

    @Override
    public Page<PreformaListadoDTO> vencidas(UUID sedeId, Pageable pageable) {
        User actual = usuarioActual();
        preformaRepository.expirarVencidas(LocalDateTime.now());
        UUID userId = esAdmin(actual) ? null : actual.getId();
        return preformaRepository.buscarPorEstado(EstadoPreforma.EXPIRADA, sedeId, userId, pageable).map(mapper::toListadoDto);
    }

    @Override
    public PreformaDashboardDTO dashboard(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin(usuarioActual());
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        LocalDateTime ahora = LocalDateTime.now();
        preformaRepository.expirarVencidas(ahora);

        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoPreforma e : EstadoPreforma.values()) porEstado.put(e.name(), 0L);
        BigDecimal confirmado = BigDecimal.ZERO, perdido = BigDecimal.ZERO;
        long confirmadas = 0, cerradas = 0;
        for (PreformasPorEstadoProjection t : preformaRepository.totalesPorEstado(sedeId, d.atStartOfDay(), h.plusDays(1).atStartOfDay())) {
            long n = t.getCantidad() != null ? t.getCantidad() : 0;
            BigDecimal monto = t.getMonto() != null ? t.getMonto() : BigDecimal.ZERO;
            porEstado.put(t.getEstado().name(), n);
            switch (t.getEstado()) {
                case CONFIRMADA -> { confirmado = confirmado.add(monto); confirmadas += n; cerradas += n; }
                case CANCELADA, EXPIRADA -> { perdido = perdido.add(monto); cerradas += n; }
                default -> { }
            }
        }
        BigDecimal tasa = cerradas == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(confirmadas * 100).divide(BigDecimal.valueOf(cerradas), 2, RoundingMode.HALF_UP);
        BigDecimal montoAbierto = preformaRepository.sumarAbiertasVigentes(sedeId, ahora);
        return new PreformaDashboardDTO(sedeId, d, h, porEstado, confirmado, perdido, tasa,
                preformaRepository.contarAbiertasVigentes(sedeId, ahora), montoAbierto != null ? montoAbierto : BigDecimal.ZERO);
    }

    @Override
    public int expirarVencidas() {
        int n = preformaRepository.expirarVencidas(LocalDateTime.now());
        if (n > 0) log.info("{} preforma(s) marcadas como EXPIRADA", n);
        return n;
    }

    // ============ PRECIOS (mismas reglas que VentaServiceImpl/CotizacionServiceImpl) ============

    record Calculada(Producto producto, Servicio servicio, CatalogoFraccion fraccion, BigDecimal cantidad,
                     BigDecimal precio, BigDecimal neto, BigDecimal igv, BigDecimal total) { }

    private List<Calculada> calcularTodas(List<DetallePreformaRequestDTO> lineas) {
        if (lineas == null || lineas.isEmpty()) throw new IllegalArgumentException("Debe incluir al menos una línea");
        return lineas.stream().map(this::calcular).toList();
    }

    Calculada calcular(DetallePreformaRequestDTO l) {
        boolean esProducto = l.productoId() != null, esServicio = l.servicioId() != null;
        if (esProducto == esServicio) throw new IllegalArgumentException("Cada línea debe indicar exactamente un producto o un servicio");
        if (l.cantidad() == null || l.cantidad().signum() <= 0) throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        Producto p = null; Servicio s = null; CatalogoFraccion f = null; BigDecimal precio;
        if (esProducto) {
            p = productoRepository.findById(l.productoId()).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + l.productoId()));
            if (!Boolean.TRUE.equals(p.getActivo())) throw new IllegalArgumentException("El producto " + p.getNombre() + " está inactivo");
            if (l.fraccionId() != null) {
                f = fraccionRepository.findWithProductoById(l.fraccionId()).orElseThrow(() -> new ResourceNotFoundException("Fracción no encontrada"));
                if (!f.getProducto().getId().equals(p.getId()) || !Boolean.TRUE.equals(f.getActivo())) {
                    throw new IllegalArgumentException("La fracción no pertenece al producto o está inactiva");
                }
                precio = f.getPrecioVenta();
            } else {
                if (Boolean.TRUE.equals(p.getEsFraccionable())) throw new IllegalArgumentException("Seleccione una fracción para " + p.getNombre());
                precio = p.getPrecioVentaBase();
            }
        } else {
            if (l.fraccionId() != null) throw new IllegalArgumentException("Un servicio no admite fracción");
            s = servicioRepository.findById(l.servicioId()).orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
            if (!Boolean.TRUE.equals(s.getActivo())) throw new IllegalArgumentException("El servicio está inactivo");
            precio = s.getPrecio();
        }
        if (precio == null) throw new IllegalStateException("El ítem no tiene precio de venta configurado");
        BigDecimal total = precio.multiply(l.cantidad()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal neto = total.divide(BigDecimal.ONE.add(IGV), 4, RoundingMode.HALF_UP);
        return new Calculada(p, s, f, l.cantidad(), precio, neto, total.subtract(neto).setScale(2, RoundingMode.HALF_UP), total);
    }

    private void aplicarTotales(PreformaVenta p, List<Calculada> lineas) {
        BigDecimal neto = BigDecimal.ZERO, igv = BigDecimal.ZERO, total = BigDecimal.ZERO;
        for (Calculada c : lineas) { neto = neto.add(c.neto()); igv = igv.add(c.igv()); total = total.add(c.total()); }
        p.setSubtotalNetoEstimado(neto.setScale(2, RoundingMode.HALF_UP));
        p.setTotalIgvEstimado(igv.setScale(2, RoundingMode.HALF_UP));
        p.setTotalEstimado(total.setScale(2, RoundingMode.HALF_UP));
    }

    private List<DetallePreforma> guardarLineas(PreformaVenta p, List<Calculada> lineas) {
        List<DetallePreforma> detalles = lineas.stream().map(c -> DetallePreforma.builder()
                .preforma(p).producto(c.producto()).servicio(c.servicio()).fraccion(c.fraccion())
                .cantidad(c.cantidad()).precioUnitarioConIgv(c.precio()).valorUnitarioNeto(c.neto())
                .igvLinea(c.igv()).porcentajeIgv(IGV_PORCENTAJE).subtotal(c.total()).build()).toList();
        detalleRepository.saveAll(detalles);
        return detalles;
    }

    private DetallePreformaRequestDTO comando(DetallePreforma d) {
        return new DetallePreformaRequestDTO(d.getProducto() != null ? d.getProducto().getId() : null,
                d.getFraccion() != null ? d.getFraccion().getId() : null,
                d.getServicio() != null ? d.getServicio().getId() : null, d.getCantidad());
    }

    // ============ HELPERS ============

    /** Si está ABIERTA y venció, la marca EXPIRADA. Devuelve true si la expiró. */
    private boolean expirarSiVencida(PreformaVenta p) {
        if (!p.estaVencida(LocalDateTime.now())) return false;
        p.setEstado(EstadoPreforma.EXPIRADA);
        preformaRepository.save(p);
        log.info("Preforma {} expirada al consultarla", p.getCodigo());
        return true;
    }

    private void exigirAbiertaVigente(PreformaVenta p, String accion) {
        if (p.getEstado() != EstadoPreforma.ABIERTA) {
            throw new IllegalStateException("No se puede " + accion + " una preforma " + p.getEstado());
        }
        if (p.estaVencida(LocalDateTime.now())) {
            throw new IllegalStateException("La preforma " + p.getCodigo() + " expiró el "
                    + p.getFechaExpiracion().format(FMT) + "; cree una nueva");
        }
    }

    /** Minutos de vigencia: los pedidos o el default, dentro del rango configurado (Configuración > ventas). */
    static int resolverVigencia(Integer minutos, ParametrosVentasDTO p) {
        int m = minutos != null ? minutos : p.preformaMinutosDefault();
        if (m < p.preformaMinutosMin() || m > p.preformaMinutosMax()) {
            throw new IllegalArgumentException("La vigencia debe estar entre " + p.preformaMinutosMin() + " y "
                    + p.preformaMinutosMax() + " minutos");
        }
        return m;
    }

    private String generarCodigo() {
        int anio = LocalDate.now().getYear();
        return String.format("PRE-%d-%06d", anio, correlativoService.siguiente("PRE-" + anio));
    }

    private Sede sedeActiva(UUID sedeId) {
        Sede s = sedeRepository.findById(sedeId).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + sedeId));
        if (Boolean.FALSE.equals(s.getActiva())) throw new IllegalStateException("La sede " + s.getNombre() + " está inactiva");
        return s;
    }

    private Cliente cliente(UUID clienteId) {
        if (clienteId == null) return null;
        return clienteRepository.findById(clienteId).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado: " + clienteId));
    }

    private PreformaVenta bloquear(UUID id) {
        return preformaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Preforma no encontrada: " + id));
    }

    private PreformaVentaResponseDTO respuesta(PreformaVenta p, List<DetallePreforma> detalles) {
        return mapper.toResponseDto(p, detalles.stream().map(detalleMapper::toResponseDto).toList());
    }

    private Map<String, Object> datos(PreformaVenta p, String operacion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operacion", operacion);
        m.put("codigo", p.getCodigo());
        m.put("estado", p.getEstado() != null ? p.getEstado().name() : null);
        m.put("totalEstimado", p.getTotalEstimado());
        return m;
    }

    private static String blankANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private boolean esAdmin(User u) {
        return u.getRol() != null && u.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    private void exigirAdmin(User u) {
        if (!esAdmin(u)) throw new AccessDeniedException("Solo un administrador puede ver el dashboard de preformas");
    }

    private void exigirCreadorOAdmin(User u, PreformaVenta p, String accion) {
        if (!esAdmin(u) && !p.getUser().getId().equals(u.getId())) {
            throw new AccessDeniedException("Solo quien creó la preforma o un administrador puede " + accion + "la");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
