package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.AsignarRepartidorRequestDTO;
import com.policalum.erp.application.dto.request.CambiarEstadoDespachoRequestDTO;
import com.policalum.erp.application.dto.request.CrearDespachoRequestDTO;
import com.policalum.erp.application.dto.request.EvidenciaEntregaRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.DespachoDashboardDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.application.mapper.OrdenDespachoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CorrelativoService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.OrdenDespachoService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Movilidad;
import com.policalum.erp.domain.model.OrdenDespacho;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.MovilidadRepository;
import com.policalum.erp.infrastructure.repository.OrdenDespachoRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import com.policalum.erp.infrastructure.repository.projection.DespachosPorEstadoProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrdenDespachoServiceImpl implements OrdenDespachoService {

    private static final String TABLA = "ordenes_despacho";
    private static final String SUB_CARPETA = "despachos";
    static final LocalDate FECHA_MIN = LocalDate.of(2000, 1, 1);
    static final LocalDate FECHA_MAX = LocalDate.of(2100, 1, 1);
    private static final Set<EstadoDespacho> ACTIVOS = EnumSet.of(EstadoDespacho.PENDIENTE, EstadoDespacho.EN_CAMINO);

    /** Transiciones permitidas por PATCH /estado (ENTREGADO va por /evidencia). */
    private static final Map<EstadoDespacho, Set<EstadoDespacho>> TRANSICIONES = Map.of(
            EstadoDespacho.PENDIENTE, EnumSet.of(EstadoDespacho.EN_CAMINO, EstadoDespacho.FALLIDO),
            EstadoDespacho.EN_CAMINO, EnumSet.of(EstadoDespacho.FALLIDO),
            EstadoDespacho.FALLIDO, EnumSet.of(EstadoDespacho.PENDIENTE),
            EstadoDespacho.ENTREGADO, EnumSet.noneOf(EstadoDespacho.class));

    private final OrdenDespachoRepository despachoRepository;
    private final VentaRepository ventaRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final MovilidadRepository movilidadRepository;
    private final CorrelativoService correlativoService;
    private final FileStorageService fileStorageService;
    private final OrdenDespachoMapper mapper;
    private final AuditService auditService;

    // ============ CREAR / CONSULTAR ============

    @Override
    public OrdenDespachoResponseDTO crear(CrearDespachoRequestDTO r) {
        User actual = usuarioActual();
        if (r.fechaProgramada() != null && r.fechaProgramada().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha programada no puede ser pasada");
        }
        Venta venta = ventaRepository.findById(r.ventaId())
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + r.ventaId()));
        if (venta.getEstado() != EstadoVenta.COMPLETADA) {
            throw new IllegalStateException("Solo se puede despachar una venta COMPLETADA (estado: " + venta.getEstado() + ")");
        }
        Sede sede = r.sedeId() != null
                ? sedeRepository.findById(r.sedeId()).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + r.sedeId()))
                : venta.getSede();

        OrdenDespacho orden = mapper.toEntity(r);
        orden.setVenta(venta);
        orden.setSede(sede);
        orden.setDireccionEntrega(r.direccionEntrega().trim());
        orden.setFechaProgramada(r.fechaProgramada() != null ? r.fechaProgramada() : LocalDate.now());
        orden.setEstado(EstadoDespacho.PENDIENTE);
        orden.setIntentosFallidos(0);
        orden.setCreadoPor(actual);
        orden.setCodigoSeguimiento(generarCodigo());

        OrdenDespacho guardada = despachoRepository.save(orden);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.INSERT, datos(guardada, "CREACION"));
        log.info("Orden de despacho {} creada para venta {}", guardada.getCodigoSeguimiento(), venta.getId());
        return mapper.toResponseDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenDespachoResponseDTO obtener(UUID id) {
        return mapper.toResponseDto(detalle(id));
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenDespachoResponseDTO seguimiento(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("El código es obligatorio");
        return despachoRepository.findByCodigo(codigo.trim())
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("No existe despacho con código " + codigo));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenDespachoResponseDTO> buscar(UUID sedeId, EstadoDespacho estado, UUID responsableId, UUID ventaId,
                                                 LocalDate desde, LocalDate hasta, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        boolean sinRango = desde == null && hasta == null;
        return despachoRepository.search(sedeId, estado, responsableId, ventaId,
                desde != null ? desde : FECHA_MIN, hasta != null ? hasta : FECHA_MAX, sinRango, pageable)
                .map(mapper::toResponseDto);
    }

    // ============ ASIGNACIÓN Y ESTADOS ============

    @Override
    public OrdenDespachoResponseDTO asignarRepartidor(UUID id, AsignarRepartidorRequestDTO r) {
        usuarioActual();
        OrdenDespacho orden = bloquear(id);
        if (orden.getEstado() != EstadoDespacho.PENDIENTE) {
            throw new IllegalStateException("Solo se asigna repartidor a una orden PENDIENTE (estado: " + orden.getEstado() + ")");
        }
        User repartidor = userRepository.findById(r.responsableId())
                .orElseThrow(() -> new ResourceNotFoundException("Repartidor no encontrado: " + r.responsableId()));
        if (!Boolean.TRUE.equals(repartidor.getActivo())) {
            throw new IllegalStateException("El repartidor " + nombre(repartidor) + " está inactivo");
        }
        Movilidad movilidad = resolverMovilidad(r.movilidadId(), repartidor, orden.getSede());

        UUID anterior = orden.getResponsable() != null ? orden.getResponsable().getId() : null;
        orden.setResponsable(repartidor);
        orden.setMovilidad(movilidad);
        orden.setFechaAsignacion(LocalDateTime.now());
        OrdenDespacho guardada = despachoRepository.save(orden);

        Map<String, Object> d = datos(guardada, "ASIGNACION");
        d.put("responsableId", repartidor.getId().toString());
        if (anterior != null) d.put("responsableAnteriorId", anterior.toString());
        if (movilidad != null) d.put("placa", movilidad.getPlaca());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        return mapper.toResponseDto(guardada);
    }

    @Override
    public OrdenDespachoResponseDTO cambiarEstado(UUID id, CambiarEstadoDespachoRequestDTO r) {
        User actual = usuarioActual();
        OrdenDespacho orden = bloquear(id);
        exigirAdminOResponsable(actual, orden);

        EstadoDespacho actualEstado = orden.getEstado();
        EstadoDespacho nuevo = r.estado();
        if (nuevo == EstadoDespacho.ENTREGADO) {
            throw new IllegalArgumentException("Para marcar ENTREGADO use POST /despachos/{id}/evidencia (foto y quién recibe)");
        }
        if (!TRANSICIONES.getOrDefault(actualEstado, Set.of()).contains(nuevo)) {
            throw new IllegalStateException("Transición no permitida: " + actualEstado + " → " + nuevo);
        }

        switch (nuevo) {
            case EN_CAMINO -> {
                if (orden.getResponsable() == null) {
                    throw new IllegalStateException("Asigne un repartidor antes de despachar la orden");
                }
                orden.setFechaSalida(LocalDateTime.now());
            }
            case FALLIDO -> {
                if (r.motivo() == null || r.motivo().isBlank()) {
                    throw new IllegalArgumentException("El motivo es obligatorio para marcar FALLIDO");
                }
                orden.setMotivoFallo(r.motivo().trim());
                orden.setIntentosFallidos((orden.getIntentosFallidos() == null ? 0 : orden.getIntentosFallidos()) + 1);
            }
            case PENDIENTE -> { // reprogramación tras FALLIDO
                if (r.fechaProgramada() != null) {
                    if (r.fechaProgramada().isBefore(LocalDate.now())) {
                        throw new IllegalArgumentException("La nueva fecha programada no puede ser pasada");
                    }
                    orden.setFechaProgramada(r.fechaProgramada());
                } else if (orden.getFechaProgramada() == null || orden.getFechaProgramada().isBefore(LocalDate.now())) {
                    orden.setFechaProgramada(LocalDate.now());
                }
                orden.setFechaSalida(null);
            }
            default -> { }
        }
        orden.setEstado(nuevo);
        OrdenDespacho guardada = despachoRepository.save(orden);

        Map<String, Object> d = datos(guardada, "CAMBIO_ESTADO");
        d.put("estadoAnterior", actualEstado.name());
        if (r.motivo() != null) d.put("motivo", r.motivo());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        log.info("Despacho {}: {} → {}", guardada.getCodigoSeguimiento(), actualEstado, nuevo);
        return mapper.toResponseDto(guardada);
    }

    @Override
    public OrdenDespachoResponseDTO registrarEntrega(UUID id, MultipartFile foto, EvidenciaEntregaRequestDTO e) {
        User actual = usuarioActual();
        validarEvidencia(foto, e);
        OrdenDespacho orden = bloquear(id);
        exigirAdminOResponsable(actual, orden);
        if (orden.getEstado() != EstadoDespacho.EN_CAMINO) {
            throw new IllegalStateException("Solo se registra la entrega de una orden EN_CAMINO (estado: " + orden.getEstado() + ")");
        }

        String ruta = fileStorageService.guardar(foto, SUB_CARPETA);
        borrarArchivoSiHayRollback(ruta);
        String rutaAnterior = orden.getUrlEvidenciaEntrega();

        orden.setUrlEvidenciaEntrega(ruta);
        orden.setEvidenciaContentType(foto.getContentType());
        orden.setEvidenciaNombreOriginal(nombreArchivo(foto));
        orden.setRecibidoPorNombre(e.recibidoPorNombre().trim());
        orden.setRecibidoPorDocumento(blankANull(e.recibidoPorDocumento()));
        orden.setLatitudEntrega(e.latitud());
        orden.setLongitudEntrega(e.longitud());
        if (e.observaciones() != null && !e.observaciones().isBlank()) {
            String obs = orden.getObservaciones();
            orden.setObservaciones(obs == null || obs.isBlank() ? "[ENTREGA] " + e.observaciones().trim()
                    : obs + "\n[ENTREGA] " + e.observaciones().trim());
        }
        orden.setFechaEntrega(LocalDateTime.now());
        orden.setEstado(EstadoDespacho.ENTREGADO);
        OrdenDespacho guardada = despachoRepository.save(orden);
        if (rutaAnterior != null) borrarArchivoTrasCommit(rutaAnterior);

        Map<String, Object> d = datos(guardada, "ENTREGA");
        d.put("recibidoPor", guardada.getRecibidoPorNombre());
        if (guardada.getLatitudEntrega() != null) {
            d.put("geo", guardada.getLatitudEntrega() + "," + guardada.getLongitudEntrega());
        }
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        log.info("Despacho {} ENTREGADO a {}", guardada.getCodigoSeguimiento(), guardada.getRecibidoPorNombre());
        return mapper.toResponseDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescargaDTO descargarEvidencia(UUID id) {
        OrdenDespacho orden = detalle(id);
        if (orden.getUrlEvidenciaEntrega() == null) {
            throw new ResourceNotFoundException("La orden " + orden.getCodigoSeguimiento() + " no tiene evidencia de entrega");
        }
        Resource resource = fileStorageService.cargar(orden.getUrlEvidenciaEntrega());
        String nombre = orden.getEvidenciaNombreOriginal() != null ? orden.getEvidenciaNombreOriginal()
                : "evidencia-" + orden.getCodigoSeguimiento();
        return new ArchivoDescargaDTO(resource, nombre, orden.getEvidenciaContentType());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenDespachoResponseDTO> porRepartidor(UUID userId, boolean soloActivas) {
        Set<EstadoDespacho> estados = soloActivas ? ACTIVOS : EnumSet.allOf(EstadoDespacho.class);
        return despachoRepository.findPorResponsable(userId, estados).stream().map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DespachoDashboardDTO dashboard(UUID sedeId, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now();
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (EstadoDespacho e : EstadoDespacho.values()) porEstado.put(e.name(), 0L);
        for (DespachosPorEstadoProjection p : despachoRepository.contarPorEstadoEnFecha(sedeId, dia)) {
            porEstado.put(p.getEstado().name(), p.getCantidad() != null ? p.getCantidad() : 0L);
        }
        LocalDateTime ini = dia.atStartOfDay();
        LocalDateTime fin = dia.plusDays(1).atStartOfDay();
        long entregados = despachoRepository.contarEntregados(sedeId, ini, fin);
        long primerIntento = despachoRepository.contarEntregadosPrimerIntento(sedeId, ini, fin);
        BigDecimal tasa = entregados == 0 ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(primerIntento * 100).divide(BigDecimal.valueOf(entregados), 2, RoundingMode.HALF_UP);

        List<DespachoDashboardDTO.CargaRepartidorDTO> carga = despachoRepository.cargaPorRepartidor(sedeId).stream()
                .map(p -> new DespachoDashboardDTO.CargaRepartidorDTO(p.getResponsableId(),
                        p.getNombre() + " " + p.getApellido(),
                        p.getPendientes() != null ? p.getPendientes() : 0,
                        p.getEnCamino() != null ? p.getEnCamino() : 0))
                .toList();

        return new DespachoDashboardDTO(sedeId, dia, porEstado, despachoRepository.contarAtrasados(sedeId, dia),
                entregados, tasa, carga);
    }

    // ============ HELPERS ============

    /** DESP-2026-000123, correlativo por año con bloqueo (sin duplicados ni huecos por rollback). */
    private String generarCodigo() {
        int anio = LocalDate.now().getYear();
        long n = correlativoService.siguiente("DESP-" + anio);
        return String.format("DESP-%d-%06d", anio, n);
    }

    private Movilidad resolverMovilidad(UUID movilidadId, User repartidor, Sede sedeOrden) {
        if (movilidadId == null) {
            List<Movilidad> propias = movilidadRepository.findByConductor_IdAndActivoTrue(repartidor.getId());
            return propias.size() == 1 ? propias.get(0) : null; // 0 o varias: queda sin movilidad
        }
        Movilidad m = movilidadRepository.findById(movilidadId)
                .orElseThrow(() -> new ResourceNotFoundException("Movilidad no encontrada: " + movilidadId));
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new IllegalStateException("La movilidad " + m.getPlaca() + " está inactiva");
        }
        if (sedeOrden != null && m.getSede() != null && !m.getSede().getId().equals(sedeOrden.getId())) {
            throw new IllegalArgumentException("La movilidad " + m.getPlaca() + " pertenece a otra sede");
        }
        return m;
    }

    void validarEvidencia(MultipartFile foto, EvidenciaEntregaRequestDTO e) {
        if (foto == null || foto.isEmpty()) {
            throw new IllegalArgumentException("La foto de entrega es obligatoria");
        }
        String archivo = nombreArchivo(foto).toLowerCase(Locale.ROOT);
        if (!(archivo.endsWith(".jpg") || archivo.endsWith(".jpeg") || archivo.endsWith(".png") || archivo.endsWith(".webp"))) {
            throw new IllegalArgumentException("La evidencia debe ser una foto JPG, PNG o WEBP");
        }
        String tipo = foto.getContentType() == null ? "" : foto.getContentType().toLowerCase(Locale.ROOT);
        if (!tipo.isBlank() && !tipo.startsWith("image/")) {
            throw new IllegalArgumentException("La evidencia debe ser una foto");
        }
        if (e == null || e.recibidoPorNombre() == null || e.recibidoPorNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de quien recibe es obligatorio");
        }
        if (e.recibidoPorNombre().trim().length() > 150) {
            throw new IllegalArgumentException("El nombre de quien recibe no puede superar 150 caracteres");
        }
        String doc = blankANull(e.recibidoPorDocumento());
        if (doc != null && !doc.matches("^\\d{8}$|^[A-Za-z0-9]{9,12}$")) {
            throw new IllegalArgumentException("Documento inválido: DNI de 8 dígitos o CE/pasaporte de 9 a 12 caracteres");
        }
        if ((e.latitud() == null) != (e.longitud() == null)) {
            throw new IllegalArgumentException("Envíe latitud y longitud juntas, o ninguna");
        }
        if (e.latitud() != null && (e.latitud().abs().compareTo(BigDecimal.valueOf(90)) > 0
                || e.longitud().abs().compareTo(BigDecimal.valueOf(180)) > 0)) {
            throw new IllegalArgumentException("Coordenadas fuera de rango");
        }
    }

    private OrdenDespacho detalle(UUID id) {
        return despachoRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de despacho no encontrada: " + id));
    }

    private OrdenDespacho bloquear(UUID id) {
        return despachoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden de despacho no encontrada: " + id));
    }

    private Map<String, Object> datos(OrdenDespacho o, String operacion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operacion", operacion);
        m.put("codigo", o.getCodigoSeguimiento());
        m.put("estado", o.getEstado() != null ? o.getEstado().name() : null);
        if (o.getVenta() != null) m.put("ventaId", o.getVenta().getId().toString());
        return m;
    }

    private static String blankANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private String nombreArchivo(MultipartFile f) {
        String n = f.getOriginalFilename();
        if (n == null || n.isBlank()) return "evidencia";
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.length() > 255 ? n.substring(n.length() - 255) : n;
    }

    private void borrarArchivoSiHayRollback(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) fileStorageService.eliminar(ruta);
            }
        });
    }

    private void borrarArchivoTrasCommit(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.eliminar(ruta);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.eliminar(ruta);
            }
        });
    }

    private String nombre(User u) {
        return u.getNombre() + " " + u.getApellido();
    }

    private boolean esAdmin(User user) {
        return user.getRol() != null && user.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    /** Cambios de estado y entrega: el ADMIN o el repartidor asignado. */
    private void exigirAdminOResponsable(User user, OrdenDespacho orden) {
        boolean esResponsable = orden.getResponsable() != null && orden.getResponsable().getId().equals(user.getId());
        if (!esAdmin(user) && !esResponsable) {
            throw new AccessDeniedException("Solo el repartidor asignado o un administrador puede actualizar esta orden");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
