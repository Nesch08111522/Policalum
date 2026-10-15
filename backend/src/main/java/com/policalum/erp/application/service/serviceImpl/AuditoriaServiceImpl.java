package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.ActividadTablaDTO;
import com.policalum.erp.application.dto.response.ActividadUsuarioDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.AuditoriaDashboardDTO;
import com.policalum.erp.application.dto.response.ConteoDiarioDTO;
import com.policalum.erp.application.dto.response.LogAuditoriaResponseDTO;
import com.policalum.erp.application.mapper.LogAuditoriaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.AuditoriaService;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.AuditLog;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.AuditLogRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.ActividadUsuarioProjection;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorAccionProjection;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorFechaProjection;
import com.policalum.erp.util.CsvUtil;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuditoriaServiceImpl implements AuditoriaService {

    static final LocalDate FECHA_MIN = LocalDate.of(2000, 1, 1);
    static final LocalDate FECHA_MAX = LocalDate.of(2100, 1, 1);
    static final int LOTE_EXPORTACION = 1_000;
    static final int DIAS_DASHBOARD_DEFAULT = 30;
    static final int DIAS_DASHBOARD_MAX = 366;
    static final int TOP = 10;
    static final String[] COLUMNAS_CSV = {"fecha", "accion", "tabla", "registro_id", "usuario_id", "usuario", "email", "user_agent", "datos"};
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final LogAuditoriaMapper mapper;
    private final AuditService auditService;
    private final ConfiguracionService configuracionService;

    @Override
    public Page<LogAuditoriaResponseDTO> buscar(String tabla, AccionAudit accion, UUID userId,
                                                LocalDate desde, LocalDate hasta, Pageable pageable) {
        exigirAdmin();
        Rango r = rango(desde, hasta);
        return auditLogRepository.buscar(normalizarTabla(tabla), null, userId, accion, r.inicio(), r.fin(), r.incluirSinFecha(),
                sinOrden(pageable)).map(mapper::toResponseDto);
    }

    @Override
    public LogAuditoriaResponseDTO obtener(UUID id) {
        exigirAdmin();
        return auditLogRepository.findDetalleById(id).map(mapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Log de auditoría no encontrado: " + id));
    }

    @Override
    public Page<LogAuditoriaResponseDTO> porUsuario(UUID userId, AccionAudit accion, LocalDate desde, LocalDate hasta, Pageable pageable) {
        exigirAdmin();
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Usuario no encontrado: " + userId);
        }
        Rango r = rango(desde, hasta);
        return auditLogRepository.buscar(null, null, userId, accion, r.inicio(), r.fin(), r.incluirSinFecha(), sinOrden(pageable))
                .map(mapper::toResponseDto);
    }

    @Override
    public List<LogAuditoriaResponseDTO> porEntidad(String tabla, UUID registroId) {
        exigirAdmin();
        String t = normalizarTabla(tabla);
        if (t == null) throw new IllegalArgumentException("Indique la tabla");
        return auditLogRepository.findByTablaAndRegistroIdOrderByCreatedAtDesc(t, registroId).stream()
                .map(mapper::toResponseDto).toList();
    }

    @Override
    public ArchivoDescargaDTO exportarCsv(String tabla, AccionAudit accion, UUID userId, LocalDate desde, LocalDate hasta) {
        User admin = exigirAdmin();
        Rango r = rango(desde, hasta);
        String t = normalizarTabla(tabla);

        Page<AuditLog> pagina = auditLogRepository.buscarParaExportar(t, null, userId, accion, r.inicio(), r.fin(),
                r.incluirSinFecha(), PageRequest.of(0, LOTE_EXPORTACION));
        int maxFilas = configuracionService.reportes().maxFilasExportacion();   // Configuración > reportes
        if (pagina.getTotalElements() > maxFilas) {
            throw new IllegalArgumentException("La consulta tiene " + pagina.getTotalElements() + " registros y el máximo exportable es "
                    + maxFilas + ". Acote las fechas o los filtros.");
        }

        StringBuilder csv = new StringBuilder(CsvUtil.BOM);   // Excel abre bien tildes y ñ
        linea(csv, COLUMNAS_CSV);
        long filas = 0;
        while (true) {
            for (AuditLog a : pagina.getContent()) {
                linea(csv, filaCsv(a));
                filas++;
            }
            if (!pagina.hasNext() || filas >= maxFilas) break;
            pagina = auditLogRepository.buscarParaExportar(t, null, userId, accion, r.inicio(), r.fin(),
                    r.incluirSinFecha(), pagina.nextPageable());
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("operacion", "EXPORTACION_AUDITORIA_CSV");
        datos.put("filas", filas);
        datos.put("tabla", t);
        datos.put("accion", accion != null ? accion.name() : null);
        datos.put("userId", userId != null ? userId.toString() : null);
        datos.put("desde", desde != null ? desde.toString() : null);
        datos.put("hasta", hasta != null ? hasta.toString() : null);
        auditService.registrar("audit_log", admin.getId(), AccionAudit.ACCESS, datos);
        log.info("Auditoría exportada a CSV por {}: {} filas", admin.getEmail(), filas);

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        String nombre = "auditoria_" + LocalDateTime.now().format(FORMATO_ARCHIVO) + ".csv";
        return new ArchivoDescargaDTO(new ByteArrayResource(bytes), nombre, "text/csv; charset=UTF-8");
    }

    @Override
    public AuditoriaDashboardDTO dashboard(LocalDate desde, LocalDate hasta) {
        exigirAdmin();
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        LocalDate d = desde != null ? desde : h.minusDays(DIAS_DASHBOARD_DEFAULT - 1L);
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        if (ChronoUnit.DAYS.between(d, h) + 1 > DIAS_DASHBOARD_MAX) {
            throw new IllegalArgumentException("El rango del dashboard no puede superar " + DIAS_DASHBOARD_MAX + " días");
        }
        LocalDateTime ini = d.atStartOfDay(), fin = h.plusDays(1).atStartOfDay();

        Map<String, Long> porAccion = new LinkedHashMap<>();
        for (AccionAudit a : AccionAudit.values()) porAccion.put(a.name(), 0L);
        long total = 0;
        for (ConteoPorAccionProjection c : auditLogRepository.conteoPorAccion(ini, fin)) {
            long n = c.getCantidad() != null ? c.getCantidad() : 0L;
            porAccion.put(c.getAccion().name(), n);
            total += n;
        }

        Map<LocalDate, Long> conteoDia = new HashMap<>();
        for (ConteoPorFechaProjection c : auditLogRepository.conteoPorDia(ini, fin)) {
            conteoDia.put(c.getFecha(), c.getCantidad() != null ? c.getCantidad() : 0L);
        }
        List<ConteoDiarioDTO> porDia = new ArrayList<>();
        for (LocalDate dia = d; !dia.isAfter(h); dia = dia.plusDays(1)) {
            porDia.add(new ConteoDiarioDTO(dia, conteoDia.getOrDefault(dia, 0L)));
        }

        PageRequest top = PageRequest.of(0, TOP);
        List<ActividadUsuarioDTO> usuarios = new ArrayList<>();
        for (ActividadUsuarioProjection u : auditLogRepository.topUsuarios(ini, fin, top)) {
            String nombre = ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
            usuarios.add(new ActividadUsuarioDTO(u.getUserId(), nombre, u.getEmail(), u.getCantidad() != null ? u.getCantidad() : 0L));
        }
        List<ActividadTablaDTO> tablas = auditLogRepository.topTablas(ini, fin, top).stream()
                .map(t -> new ActividadTablaDTO(t.getTabla(), t.getCantidad() != null ? t.getCantidad() : 0L)).toList();

        return new AuditoriaDashboardDTO(d, h, total, porAccion, porDia, usuarios, tablas,
                auditLogRepository.contarSinUsuario(ini, fin), auditLogRepository.countByCreatedAtIsNull());
    }

    // ============ CSV ============

    static String[] filaCsv(AuditLog a) {
        User u = a.getUser();
        return new String[] {
                a.getCreatedAt() != null ? a.getCreatedAt().format(FORMATO_FECHA) : "",
                a.getAccion() != null ? a.getAccion().name() : "",
                a.getTabla(),
                a.getRegistroId() != null ? a.getRegistroId().toString() : "",
                u != null && u.getId() != null ? u.getId().toString() : "",
                u != null ? ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim() : "SISTEMA",
                u != null ? u.getEmail() : "",
                a.getUserAgent(),
                a.getDatosJson() != null ? JSON.writeValueAsString(a.getDatosJson()) : ""
        };
    }

    private static void linea(StringBuilder sb, String[] valores) {
        CsvUtil.linea(sb, valores);
    }

    /** RFC 4180 + protección contra inyección de fórmulas (ver CsvUtil). */
    static String celda(String valor) {
        return CsvUtil.celda(valor);
    }

    // ============ HELPERS ============

    private record Rango(LocalDateTime inicio, LocalDateTime fin, boolean incluirSinFecha) { }

    /** Fechas inclusivas → [inicio, fin). Sin ninguna fecha se incluyen también los logs antiguos sin fecha. */
    static Rango rango(LocalDate desde, LocalDate hasta) {
        LocalDate d = desde != null ? desde : FECHA_MIN;
        LocalDate h = hasta != null ? hasta : FECHA_MAX;
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        return new Rango(d.atStartOfDay(), h.plusDays(1).atStartOfDay(), desde == null && hasta == null);
    }

    /** El orden lo fija la consulta (más recientes primero, sin fecha al final). */
    private static Pageable sinOrden(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    private static String normalizarTabla(String tabla) {
        return tabla == null || tabla.isBlank() ? null : tabla.trim().toLowerCase(Locale.ROOT);
    }

    private User exigirAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
        if (u.getRol() == null || u.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede consultar la auditoría");
        }
        return u;
    }
}
