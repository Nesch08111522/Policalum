package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.mapper.CotizacionMapper;
import com.policalum.erp.application.mapper.DetalleCotizacionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.CotizacionService;
import com.policalum.erp.application.service.VentaService;
import com.policalum.erp.application.service.support.RangosConsulta;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CotizacionServiceImpl implements CotizacionService {
    private static final BigDecimal IGV = new BigDecimal("0.18");
    private static final BigDecimal IGV_PORCENTAJE = new BigDecimal("18.00");
    private final CotizacionRepository cotizacionRepository;
    private final DetalleCotizacionRepository detalleRepository;
    private final SedeRepository sedeRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final CatalogoFraccionRepository fraccionRepository;
    private final UserRepository userRepository;
    private final CotizacionMapper cotizacionMapper;
    private final DetalleCotizacionMapper detalleMapper;
    private final VentaService ventaService;
    private final VentaRepository ventaRepository;
    private final AuditService auditService;
    private final ConfiguracionService configuracionService;

    @Override
    public CotizacionResponseDTO create(CotizacionRequestDTO request) {
        User usuario = usuarioActual();
        Cotizacion cotizacion = cotizacionMapper.toEntity(request);
        cotizacion.setSede(sede(request.sedeId()));
        cotizacion.setCliente(cliente(request.clienteId()));
        cotizacion.setUser(usuario);
        cotizacion.setEstado(EstadoCotizacion.EMITIDA);
        cotizacion.setCanal(CanalCotizacion.INTERNO);
        cotizacion.setFechaVigencia(request.fechaVigencia() == null ? LocalDate.now().plusDays(configuracionService.ventas().vigenciaCotizacionDias()) : request.fechaVigencia());
        if (cotizacion.getFechaVigencia().isBefore(LocalDate.now())) throw new IllegalArgumentException("La vigencia no puede estar en el pasado");
        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        recalcularLineas(guardada, request.lineas());
        auditService.registrar("cotizaciones", guardada.getId(), AccionAudit.INSERT, Map.of("estado", guardada.getEstado().name(), "total", guardada.getTotal()));
        log.info("Cotización creada: {} - Total: {} - Cliente: {}", guardada.getId(), guardada.getTotal(),
                guardada.getCliente() != null ? guardada.getCliente().getNombre() : "sin cliente");
        return response(guardada);
    }

    @Override
    public CotizacionResponseDTO crearDesdeWeb(CotizacionWebRequestDTO request) {
        Sede sede = sedeRecepcionWeb();
        User receptor = userRepository.findFirstByActivoTrueAndRol_NombreOrderByEmailAsc(RolNombre.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Registra un administrador activo para recibir cotizaciones de la web"));
        Cliente cliente = clienteWeb(request);
        Cotizacion cotizacion = Cotizacion.builder()
                .sede(sede)
                .user(receptor)
                .cliente(cliente)
                .estado(EstadoCotizacion.PENDIENTE)
                .canal(CanalCotizacion.WEB)
                .fechaVigencia(LocalDate.now().plusDays(configuracionService.ventas().vigenciaCotizacionDias()))
                .observaciones(request.observaciones() == null || request.observaciones().isBlank() ? null : request.observaciones().trim())
                .contactoRuc(request.ruc())
                .contactoRazonSocial(request.razonSocial().trim())
                .contactoCorreo(request.correo().trim().toLowerCase())
                .contactoTelefono(request.telefono().trim())
                .build();
        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        recalcularLineas(guardada, request.lineas());
        auditService.registrar("cotizaciones", guardada.getId(), AccionAudit.INSERT,
                Map.of("estado", guardada.getEstado().name(), "canal", CanalCotizacion.WEB.name(), "total", guardada.getTotal()));
        log.info("Cotización web {} de {} por {}", guardada.getId(), request.razonSocial(), guardada.getTotal());
        return response(guardada);
    }

    @Override
    public CotizacionResponseDTO update(UUID id, CotizacionRequestDTO request) {
        Cotizacion cotizacion = obtener(id);
        exigirEstado(cotizacion, EstadoCotizacion.EMITIDA, "editar");
        exigirCreadorOAdmin(cotizacion);
        cotizacionMapper.updateEntityFromDto(request, cotizacion);
        cotizacion.setSede(sede(request.sedeId()));
        cotizacion.setCliente(cliente(request.clienteId()));
        LocalDate vigencia = request.fechaVigencia() == null ? cotizacion.getFechaVigencia() : request.fechaVigencia();
        if (vigencia.isBefore(LocalDate.now())) throw new IllegalArgumentException("La vigencia no puede estar en el pasado");
        cotizacion.setFechaVigencia(vigencia);
        recalcularLineas(cotizacion, request.lineas());
        auditService.registrar("cotizaciones", cotizacion.getId(), AccionAudit.UPDATE, Map.of("operacion", "actualizacion", "total", cotizacion.getTotal()));
        log.info("Cotización actualizada: {} - Total: {}", cotizacion.getId(), cotizacion.getTotal());
        return response(cotizacion);
    }

    @Override @Transactional(readOnly = true)
    public CotizacionResponseDTO findById(UUID id) { return response(obtener(id)); }

    @Override @Transactional(readOnly = true)
    public Page<CotizacionListadoDTO> search(UUID clienteId, UUID sedeId, EstadoCotizacion estado, LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        return cotizacionRepository.buscar(clienteId, sedeId, estado, RangosConsulta.desde(desde), RangosConsulta.hasta(hasta), pageable).map(this::listado);
    }

    @Override
    public void delete(UUID id) {
        Cotizacion cotizacion = obtener(id);
        exigirEstado(cotizacion, EstadoCotizacion.EMITIDA, "eliminar");
        exigirCreadorOAdmin(cotizacion);
        detalleRepository.deleteByCotizacion_Id(id);
        cotizacionRepository.delete(cotizacion);
        auditService.registrar("cotizaciones", id, AccionAudit.DELETE, Map.of("operacion", "eliminacion_borrador"));
        log.info("Cotización eliminada: {}", id);
    }

    @Override
    public CotizacionResponseDTO aceptar(UUID id) {
        Cotizacion cotizacion = obtener(id);
        exigirEstados(cotizacion, "aceptar", EstadoCotizacion.EMITIDA, EstadoCotizacion.PENDIENTE);
        exigirCreadorOAdmin(cotizacion);
        if (cotizacion.getFechaVigencia().isBefore(LocalDate.now())) { cotizacion.setEstado(EstadoCotizacion.VENCIDA); throw new IllegalStateException("La cotización está vencida"); }
        cotizacion.setEstado(EstadoCotizacion.ACEPTADA);
        auditService.registrar("cotizaciones", id, AccionAudit.UPDATE, Map.of("operacion", "aceptacion"));
        log.info("Cotización {} ACEPTADA", id);
        return response(cotizacion);
    }

    @Override
    public CotizacionResponseDTO rechazar(UUID id, RechazarCotizacionRequestDTO request) {
        Cotizacion cotizacion = obtener(id);
        exigirEstados(cotizacion, "rechazar", EstadoCotizacion.EMITIDA, EstadoCotizacion.PENDIENTE);
        exigirCreadorOAdmin(cotizacion);
        cotizacion.setEstado(EstadoCotizacion.RECHAZADA);
        cotizacion.setObservaciones((cotizacion.getObservaciones() == null ? "" : cotizacion.getObservaciones() + "\n") + "Rechazada: " + request.motivo().trim());
        auditService.registrar("cotizaciones", id, AccionAudit.UPDATE, Map.of("operacion", "rechazo", "motivo", request.motivo()));
        log.info("Cotización {} RECHAZADA. Motivo: {}", id, request.motivo());
        return response(cotizacion);
    }

    @Override
    public CotizacionResponseDTO marcarNoComprada(UUID id) {
        Cotizacion cotizacion = obtener(id);
        exigirEstados(cotizacion, "marcar como no comprada", EstadoCotizacion.EMITIDA, EstadoCotizacion.PENDIENTE);
        exigirCreadorOAdmin(cotizacion);
        cotizacion.setEstado(EstadoCotizacion.NO_COMPRADO);
        cotizacion.setObservaciones((cotizacion.getObservaciones() == null ? "" : cotizacion.getObservaciones() + "\n") + "El cliente no compró. El stock no se movió.");
        auditService.registrar("cotizaciones", id, AccionAudit.UPDATE, Map.of("operacion", "no_comprado"));
        log.info("Cotización {} NO_COMPRADO", id);
        return response(cotizacion);
    }

    @Override
    public VentaProcesadaResponseDTO convertir(UUID id, ConvertirCotizacionRequestDTO request) {
        Cotizacion cotizacion = obtener(id);
        exigirEstado(cotizacion, EstadoCotizacion.ACEPTADA, "convertir");
        exigirCreadorOAdmin(cotizacion);
        List<DetalleCotizacionCommandDTO> lineas = request.lineas();
        if (lineas == null || lineas.isEmpty()) lineas = detalleRepository.findByCotizacion_Id(id).stream().map(d -> new DetalleCotizacionCommandDTO(
                d.getProducto() == null ? null : d.getProducto().getId(), d.getFraccion() == null ? null : d.getFraccion().getId(),
                d.getServicio() == null ? null : d.getServicio().getId(), d.getCantidad())).toList();
        RegistrarVentaRequestDTO ventaRequest = new RegistrarVentaRequestDTO(cotizacion.getSede().getId(), request.almacenId(),
                cotizacion.getCliente() == null ? null : cotizacion.getCliente().getId(), request.turnoCajaId(), request.tipoComprobante(),
                request.canalVenta() == null ? CanalVenta.LOCAL : request.canalVenta(), cotizacion.getId(), null, null,
                lineas.stream().map(l -> new LineaVentaCommandDTO(l.productoId(), l.fraccionId(), l.servicioId(), l.cantidad())).toList(), request.pagos());
        VentaProcesadaResponseDTO venta = ventaService.registrar(ventaRequest);
        Venta ventaEntity = ventaRepository.findById(venta.id())
                .orElseThrow(() -> new ResourceNotFoundException("Venta recién creada no encontrada: " + venta.id()));
        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
        cotizacion.setVenta(ventaEntity);
        cotizacionRepository.save(cotizacion);
        auditService.registrar("cotizaciones", id, AccionAudit.UPDATE,
                Map.of("operacion", "conversion", "ventaId", venta.id().toString(), "ventaSerie", venta.serieCorrelativo()));
        log.info("Cotización {} convertida a venta {}", id, venta.serieCorrelativo());
        return venta;
    }

    @Override
    @Transactional
    public List<CotizacionListadoDTO> vencidas() {
        List<Cotizacion> vencidas = new ArrayList<>();
        vencidas.addAll(cotizacionRepository.findByEstadoAndFechaVigenciaBefore(EstadoCotizacion.EMITIDA, LocalDate.now()));
        vencidas.addAll(cotizacionRepository.findByEstadoAndFechaVigenciaBefore(EstadoCotizacion.PENDIENTE, LocalDate.now()));
        List<Cotizacion> actualizadas = new ArrayList<>();
        for (Cotizacion c : vencidas) {
            c.setEstado(EstadoCotizacion.VENCIDA);
            actualizadas.add(cotizacionRepository.save(c));
        }
        log.info("Marcadas {} cotizaciones como VENCIDAS", actualizadas.size());
        return actualizadas.stream().map(this::listado).toList();
    }

    @Override @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        return Map.of("emitidas", cotizacionRepository.countByEstado(EstadoCotizacion.EMITIDA),
                "pendientesWeb", cotizacionRepository.countByEstado(EstadoCotizacion.PENDIENTE),
                "aceptadas", cotizacionRepository.countByEstado(EstadoCotizacion.ACEPTADA),
                "convertidas", cotizacionRepository.countByEstado(EstadoCotizacion.CONVERTIDA),
                "vencidas", cotizacionRepository.countByEstado(EstadoCotizacion.VENCIDA));
    }

    private void recalcularLineas(Cotizacion cotizacion, List<DetalleCotizacionCommandDTO> lineas) {
        detalleRepository.deleteByCotizacion_Id(cotizacion.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCotizacionCommandDTO linea : lineas) {
            Calculada calculada = calcular(linea);
            DetalleCotizacion detalle = detalleMapper.toEntity(linea);
            detalle.setCotizacion(cotizacion); detalle.setProducto(calculada.producto()); detalle.setFraccion(calculada.fraccion()); detalle.setServicio(calculada.servicio());
            detalle.setPrecioUnitarioConIgv(calculada.precio()); detalle.setValorUnitarioNeto(calculada.neto()); detalle.setIgvLinea(calculada.igv()); detalle.setPorcentajeIgv(IGV_PORCENTAJE); detalle.setSubtotal(calculada.total());
            detalleRepository.save(detalle); total = total.add(calculada.total());
        }
        cotizacion.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        cotizacionRepository.save(cotizacion);
    }
    private Calculada calcular(DetalleCotizacionCommandDTO l) {
        boolean producto = l.productoId() != null, servicio = l.servicioId() != null;
        if (producto == servicio) throw new IllegalArgumentException("Cada línea debe ser un producto o un servicio, nunca ambos");
        Producto p = null; Servicio s = null; CatalogoFraccion f = null; BigDecimal precio;
        if (producto) { p = productoRepository.findById(l.productoId()).orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado")); if (!Boolean.TRUE.equals(p.getActivo())) throw new IllegalArgumentException("El producto está inactivo");
            if (l.fraccionId() != null) { f = fraccionRepository.findWithProductoById(l.fraccionId()).orElseThrow(() -> new ResourceNotFoundException("Fracción no encontrada")); if (!f.getProducto().getId().equals(p.getId()) || !Boolean.TRUE.equals(f.getActivo())) throw new IllegalArgumentException("La fracción no es válida para el producto"); precio = f.getPrecioVenta(); }
            else { if (Boolean.TRUE.equals(p.getEsFraccionable())) throw new IllegalArgumentException("Seleccione una fracción para el producto fraccionable"); precio = p.getPrecioVentaBase(); }
        } else { if (l.fraccionId() != null) throw new IllegalArgumentException("Un servicio no admite fracción"); s = servicioRepository.findById(l.servicioId()).orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado")); if (!Boolean.TRUE.equals(s.getActivo())) throw new IllegalArgumentException("El servicio está inactivo"); precio = s.getPrecio(); }
        BigDecimal bruto = precio.multiply(l.cantidad()).setScale(2, RoundingMode.HALF_UP); BigDecimal neto = bruto.divide(BigDecimal.ONE.add(IGV), 4, RoundingMode.HALF_UP);
        return new Calculada(p, s, f, precio, neto, bruto.subtract(neto).setScale(2, RoundingMode.HALF_UP), bruto);
    }
    private Cotizacion obtener(UUID id) { return cotizacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada")); }
    private Sede sede(UUID id) { return sedeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada")); }
    private Cliente cliente(UUID id) { return id == null ? null : clienteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado")); }
    private User usuarioActual() { String email = SecurityContextHolder.getContext().getAuthentication().getName(); return userRepository.findByEmailAndActivoTrue(email).orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido")); }
    private void exigirEstado(Cotizacion c, EstadoCotizacion estadoEsperado, String accion) {
        if (c.getEstado() != estadoEsperado) {
            throw new IllegalStateException(String.format(
                    "No se puede %s una cotización en estado %s. Se requiere estado %s.",
                    accion, c.getEstado().name(), estadoEsperado.name()));
        }
    }
    private void exigirEstados(Cotizacion c, String accion, EstadoCotizacion... esperados) {
        for (EstadoCotizacion esperado : esperados) {
            if (c.getEstado() == esperado) return;
        }
        throw new IllegalStateException(String.format("No se puede %s una cotización en estado %s.", accion, c.getEstado().name()));
    }
    private void exigirCreadorOAdmin(Cotizacion c) {
        User u = usuarioActual();
        var autoridades = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean admin = autoridades.stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (c.getCanal() == CanalCotizacion.WEB) {
            boolean ventas = autoridades.stream().anyMatch(a -> "ROLE_MULTIFUNCIONAL".equals(a.getAuthority()));
            if (admin || ventas) return;
            throw new IllegalStateException("Solo el equipo de ventas puede gestionar una cotización de la web");
        }
        if (!admin && (c.getUser() == null || !c.getUser().getId().equals(u.getId()))) {
            throw new IllegalStateException("Solo el creador o un administrador puede ejecutar esta acción");
        }
    }
    private Sede sedeRecepcionWeb() {
        return sedeRepository.findAll().stream()
                .filter(item -> !Boolean.FALSE.equals(item.getActiva()))
                .min(Comparator.comparing(item -> item.getNombre() == null ? "" : item.getNombre(), String.CASE_INSENSITIVE_ORDER))
                .orElseThrow(() -> new IllegalStateException("Registra una sede activa para recibir cotizaciones de la web"));
    }
    private Cliente clienteWeb(CotizacionWebRequestDTO request) {
        String ruc = request.ruc().trim();
        Cliente cliente = clienteRepository.findByNumeroDocumento(ruc).orElse(null);
        if (cliente == null) {
            cliente = clienteRepository.save(Cliente.builder()
                    .tipoDocumento("RUC")
                    .numeroDocumento(ruc)
                    .nombre(request.razonSocial().trim())
                    .email(request.correo().trim().toLowerCase())
                    .celular(request.telefono().trim())
                    .activo(true)
                    .build());
            return cliente;
        }
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) cliente.setEmail(request.correo().trim().toLowerCase());
        if (cliente.getCelular() == null || cliente.getCelular().isBlank()) cliente.setCelular(request.telefono().trim());
        return cliente;
    }
    private String origenDe(Cotizacion c) {
        CanalCotizacion canal = c.getCanal() == null ? CanalCotizacion.INTERNO : c.getCanal();
        if (canal == CanalCotizacion.WEB && c.getEstado() == EstadoCotizacion.PENDIENTE) return "NUEVO (WEB)";
        return canal == CanalCotizacion.WEB ? "WEB" : "INTERNO";
    }
    private CotizacionListadoDTO listado(Cotizacion c) {
        CanalCotizacion canal = c.getCanal() == null ? CanalCotizacion.INTERNO : c.getCanal();
        return new CotizacionListadoDTO(c.getId(), c.getSede().getId(), c.getCliente() == null ? null : c.getCliente().getId(), c.getCliente() == null ? null : c.getCliente().getNombre(), c.getEstado(), c.getTotal(), c.getFechaVigencia(), c.getCreatedAt(), canal, origenDe(c));
    }
    private CotizacionResponseDTO response(Cotizacion c) {
        List<DetalleCotizacionResponseDTO> lineas = detalleRepository.findByCotizacion_Id(c.getId()).stream().map(d -> new DetalleCotizacionResponseDTO(d.getId(), d.getProducto() == null ? null : d.getProducto().getId(), d.getProducto() == null ? null : d.getProducto().getNombre(), d.getFraccion() == null ? null : d.getFraccion().getId(), d.getFraccion() == null ? null : d.getFraccion().getNombreFraccion(), d.getServicio() == null ? null : d.getServicio().getId(), d.getServicio() == null ? null : d.getServicio().getNombre(), d.getCantidad(), d.getPrecioUnitarioConIgv(), d.getValorUnitarioNeto(), d.getIgvLinea(), d.getPorcentajeIgv(), d.getSubtotal())).toList();
        CanalCotizacion canal = c.getCanal() == null ? CanalCotizacion.INTERNO : c.getCanal();
        UUID usuarioId = c.getUser() == null ? null : c.getUser().getId();
        String usuarioNombre = c.getUser() == null ? "Canal web" : ((c.getUser().getNombre() == null ? "" : c.getUser().getNombre()) + " " + (c.getUser().getApellido() == null ? "" : c.getUser().getApellido())).trim();
        return new CotizacionResponseDTO(c.getId(), c.getSede().getId(), c.getSede().getNombre(), usuarioId, usuarioNombre, c.getCliente() == null ? null : c.getCliente().getId(), c.getCliente() == null ? null : c.getCliente().getNombre(), c.getEstado(), c.getTotal(), c.getFechaVigencia(), c.getObservaciones(), c.getVenta() == null ? null : c.getVenta().getId(), c.getCreatedAt(), lineas, canal, origenDe(c));
    }
    private record Calculada(Producto producto, Servicio servicio, CatalogoFraccion fraccion, BigDecimal precio, BigDecimal neto, BigDecimal igv, BigDecimal total) { }
}
