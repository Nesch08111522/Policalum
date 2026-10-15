package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.AsignarConductorRequestDTO;
import com.policalum.erp.application.dto.request.MovilidadRequestDTO;
import com.policalum.erp.application.dto.response.MovilidadDashboardDTO;
import com.policalum.erp.application.dto.response.MovilidadListadoDTO;
import com.policalum.erp.application.dto.response.MovilidadResponseDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.application.dto.response.ReporteUsoMovilidadDTO;
import com.policalum.erp.application.dto.response.UsoMovilidadDTO;
import com.policalum.erp.application.mapper.MovilidadMapper;
import com.policalum.erp.application.mapper.OrdenDespachoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.MovilidadService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoMovilidad;
import com.policalum.erp.domain.model.Movilidad;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.MovilidadRepository;
import com.policalum.erp.infrastructure.repository.OrdenDespachoRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.ConteoPorTextoProjection;
import com.policalum.erp.infrastructure.repository.projection.UsoMovilidadProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MovilidadServiceImpl implements MovilidadService {

    private static final String TABLA = "movilidades";
    private static final List<EstadoDespacho> ACTIVOS = List.of(EstadoDespacho.PENDIENTE, EstadoDespacho.EN_CAMINO);
    /** Para el chequeo de placa al crear (ningún id real coincide). */
    private static final UUID SIN_ID = new UUID(0L, 0L);

    private final MovilidadRepository movilidadRepository;
    private final OrdenDespachoRepository despachoRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final MovilidadMapper mapper;
    private final OrdenDespachoMapper despachoMapper;
    private final AuditService auditService;

    @Override
    public MovilidadResponseDTO crear(MovilidadRequestDTO r) {
        exigirAdmin();
        String placa = formatearPlaca(r.placa());
        validarPlacaUnica(placa, SIN_ID);
        Movilidad m = mapper.toEntity(r);
        m.setPlaca(placa);
        m.setSede(sede(r.sedeId()));
        m.setActivo(true);
        Movilidad guardada = movilidadRepository.save(m);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.INSERT, datos(guardada, "CREACION"));
        log.info("Movilidad creada: {} ({})", guardada.getPlaca(), guardada.getTipo());
        return mapper.toResponseDto(guardada, 0L);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovilidadListadoDTO> listar(UUID sedeId, Boolean activo, TipoMovilidad tipo, String texto, Pageable pageable) {
        String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
        Pageable p = pageable.isPaged() ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) : pageable;
        return movilidadRepository.buscar(sedeId, activo, tipo != null ? tipo.name() : null, patron, p).map(mapper::toListadoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public MovilidadResponseDTO obtener(UUID id) {
        Movilidad m = movilidadRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movilidad no encontrada: " + id));
        return mapper.toResponseDto(m, despachoRepository.countByMovilidad_IdAndEstadoIn(id, ACTIVOS));
    }

    @Override
    public MovilidadResponseDTO actualizar(UUID id, MovilidadRequestDTO r) {
        exigirAdmin();
        Movilidad m = bloquear(id);
        String placa = formatearPlaca(r.placa());
        validarPlacaUnica(placa, id);
        if (Boolean.FALSE.equals(r.activo()) && Boolean.TRUE.equals(m.getActivo())) {
            exigirSinDespachosActivos(m);
        }
        UUID sedeAnterior = m.getSede() != null ? m.getSede().getId() : null;
        if (!r.sedeId().equals(sedeAnterior)) exigirSinDespachosActivos(m);   // no mover de sede con rutas abiertas

        mapper.updateEntityFromDto(r, m);
        m.setPlaca(placa);
        m.setSede(sede(r.sedeId()));
        if (r.activo() != null) m.setActivo(r.activo());
        Movilidad guardada = movilidadRepository.save(m);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datos(guardada, "ACTUALIZACION"));
        return mapper.toResponseDto(guardada, despachoRepository.countByMovilidad_IdAndEstadoIn(id, ACTIVOS));
    }

    @Override
    public void desactivar(UUID id) {
        exigirAdmin();
        Movilidad m = bloquear(id);
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new IllegalStateException("La movilidad " + m.getPlaca() + " ya está inactiva");
        }
        exigirSinDespachosActivos(m);
        m.setActivo(false);
        movilidadRepository.save(m);
        auditService.registrar(TABLA, m.getId(), AccionAudit.DELETE, datos(m, "DESACTIVACION"));
        log.info("Movilidad desactivada: {}", m.getPlaca());
    }

    @Override
    public MovilidadResponseDTO asignarConductor(UUID id, AsignarConductorRequestDTO r) {
        exigirAdmin();
        Movilidad m = bloquear(id);
        if (!Boolean.TRUE.equals(m.getActivo())) {
            throw new IllegalStateException("No se puede asignar conductor a una movilidad inactiva (" + m.getPlaca() + ")");
        }
        UUID conductorId = r != null ? r.conductorId() : null;
        User conductor = null;
        if (conductorId != null) {
            conductor = userRepository.findById(conductorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + conductorId));
            if (!Boolean.TRUE.equals(conductor.getActivo())) {
                throw new IllegalArgumentException("El conductor " + conductor.getEmail() + " está inactivo");
            }
            if (conductor.getRol() == null || conductor.getRol().getNombre() != RolNombre.ROLE_MULTIFUNCIONAL) {
                throw new IllegalArgumentException("Solo un usuario MULTIFUNCIONAL puede ser conductor");
            }
        }
        m.setConductor(conductor);
        Movilidad guardada = movilidadRepository.save(m);
        Map<String, Object> d = datos(guardada, conductor != null ? "ASIGNACION_CONDUCTOR" : "RETIRO_CONDUCTOR");
        d.put("conductorId", conductor != null ? conductor.getId().toString() : null);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, d);
        return mapper.toResponseDto(guardada, despachoRepository.countByMovilidad_IdAndEstadoIn(id, ACTIVOS));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdenDespachoResponseDTO> historial(UUID id, EstadoDespacho estado, Pageable pageable) {
        if (!movilidadRepository.existsById(id)) throw new ResourceNotFoundException("Movilidad no encontrada: " + id);
        Pageable p = pageable.isPaged() ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()) : pageable;
        return despachoRepository.historialMovilidad(id, estado, p).map(despachoMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteUsoMovilidadDTO reporteUso(UUID sedeId, LocalDate desde, LocalDate hasta) {
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        LocalDate d = desde != null ? desde : h.withDayOfMonth(1);
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");

        List<UsoMovilidadDTO> vehiculos = new ArrayList<>();
        long entregados = 0, fallidos = 0;
        for (UsoMovilidadProjection u : movilidadRepository.uso(sedeId, d, h)) {
            long e = n(u.getEntregados()), f = n(u.getFallidos());
            entregados += e;
            fallidos += f;
            BigDecimal tasa = e + f > 0
                    ? BigDecimal.valueOf(e * 100).divide(BigDecimal.valueOf(e + f), 2, RoundingMode.HALF_UP) : null;
            String conductor = u.getConductorNombre() == null && u.getConductorApellido() == null ? null
                    : ((u.getConductorNombre() != null ? u.getConductorNombre() : "") + " "
                    + (u.getConductorApellido() != null ? u.getConductorApellido() : "")).trim();
            vehiculos.add(new UsoMovilidadDTO(u.getMovilidadId(), u.getPlaca(), u.getTipo(), conductor,
                    Boolean.TRUE.equals(u.getActivo()), n(u.getDespachos()), e, f, n(u.getPendientes()), n(u.getIntentosFallidos()), tasa));
        }
        vehiculos.sort(Comparator.comparingLong(UsoMovilidadDTO::entregados).reversed().thenComparing(UsoMovilidadDTO::placa));
        return new ReporteUsoMovilidadDTO(sedeId, d, h, entregados, fallidos,
                despachoRepository.contarSinMovilidad(sedeId, d, h), vehiculos);
    }

    @Override
    @Transactional(readOnly = true)
    public MovilidadDashboardDTO dashboard(UUID sedeId) {
        long activos = movilidadRepository.contarPorEstado(sedeId, true);
        long inactivos = movilidadRepository.contarPorEstado(sedeId, false);
        Map<String, Long> porTipo = new LinkedHashMap<>();
        for (TipoMovilidad t : TipoMovilidad.values()) porTipo.put(t.name(), 0L);
        for (ConteoPorTextoProjection c : movilidadRepository.activosPorTipo(sedeId)) {
            porTipo.merge(c.getClave() != null ? c.getClave() : "SIN_TIPO", n(c.getCantidad()), Long::sum);
        }
        LocalDate hoy = LocalDate.now();
        return new MovilidadDashboardDTO(sedeId, activos + inactivos, activos, inactivos,
                movilidadRepository.contarActivosSinConductor(sedeId), despachoRepository.contarVehiculosEnRuta(sedeId), porTipo,
                despachoRepository.contarEntregasConMovilidad(sedeId, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay()));
    }

    // ===================== HELPERS =====================

    /** Mayúsculas, sin espacios; "ABC123" → "ABC-123" (formato peruano de auto). */
    static String formatearPlaca(String placa) {
        String p = placa.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        if (p.matches("[A-Z0-9]{3}[0-9]{3}")) p = p.substring(0, 3) + "-" + p.substring(3);
        if (p.length() > 10) throw new IllegalArgumentException("La placa no puede tener más de 10 caracteres");
        return p;
    }

    static String normalizarPlaca(String placa) {
        return placa.toUpperCase(Locale.ROOT).replace("-", "").replace(" ", "");
    }

    private void validarPlacaUnica(String placa, UUID excluirId) {
        if (movilidadRepository.existePlaca(normalizarPlaca(placa), excluirId)) {
            throw new IllegalStateException("Ya existe un vehículo con la placa " + placa);
        }
    }

    private void exigirSinDespachosActivos(Movilidad m) {
        long activos = despachoRepository.countByMovilidad_IdAndEstadoIn(m.getId(), ACTIVOS);
        if (activos > 0) {
            throw new IllegalStateException("La movilidad " + m.getPlaca() + " tiene " + activos
                    + " despacho(s) PENDIENTE o EN_CAMINO; ciérrelos o reasígnelos primero");
        }
    }

    private Movilidad bloquear(UUID id) {
        return movilidadRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movilidad no encontrada: " + id));
    }

    private Sede sede(UUID id) {
        return sedeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + id));
    }

    private Map<String, Object> datos(Movilidad m, String operacion) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("operacion", operacion);
        d.put("placa", m.getPlaca());
        d.put("tipo", m.getTipo());
        d.put("activo", m.getActivo());
        d.put("sedeId", m.getSede() != null ? m.getSede().getId().toString() : null);
        return d;
    }

    private static long n(Long v) {
        return v != null ? v : 0L;
    }

    private User exigirAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
        if (u.getRol() == null || u.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede modificar las movilidades");
        }
        return u;
    }
}
