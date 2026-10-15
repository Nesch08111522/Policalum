package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.AsignarTurnoRequestDTO;
import com.policalum.erp.application.dto.request.TurnoTrabajoRequestDTO;
import com.policalum.erp.application.dto.response.AsignacionTurnoResponseDTO;
import com.policalum.erp.application.dto.response.TurnoDashboardDTO;
import com.policalum.erp.application.dto.response.TurnoDelDiaDTO;
import com.policalum.erp.application.dto.response.TurnoTrabajoResponseDTO;
import com.policalum.erp.application.mapper.TurnoTrabajoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.TurnoTrabajoService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.RrhhTurnoTrabajo;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDigitalRepository;
import com.policalum.erp.infrastructure.repository.RrhhTurnoTrabajoRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.util.HorarioUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
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
public class TurnoTrabajoServiceImpl implements TurnoTrabajoService {

    private static final String TABLA = "rrhh_turnos_trabajo";
    /** Jornada máxima legal semanal en Perú. */
    static final int MAX_MINUTOS_SEMANA = 48 * 60;

    private final RrhhTurnoTrabajoRepository turnoRepository;
    private final RrhhLegajoDigitalRepository legajoRepository;
    private final UserRepository userRepository;
    private final SedeRepository sedeRepository;
    private final TurnoTrabajoMapper mapper;
    private final AuditService auditService;

    @Override
    public TurnoTrabajoResponseDTO crear(TurnoTrabajoRequestDTO r) {
        exigirAdmin(usuarioActual());
        Horario h = validarHorario(r.nombreTurno(), r.horaInicio(), r.horaFin(), r.diasSemana(), r.toleranciaMinutos());
        User empleado = empleadoActivo(r.userId());
        Sede sede = resolverSede(r.sedeId(), empleado);
        verificarCruces(empleado, h, null, turnoRepository.findActivosByUserId(empleado.getId()));

        RrhhTurnoTrabajo turno = mapper.toEntity(r);
        aplicar(turno, h, empleado, sede);
        turno.setActivo(r.activo() == null ? Boolean.TRUE : r.activo());

        RrhhTurnoTrabajo guardado = turnoRepository.save(turno);
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.INSERT, snapshot(guardado));
        log.info("Turno creado: {} - {} {}-{} {} - empleado {}", guardado.getId(), h.nombre(), h.inicio(), h.fin(),
                h.dias(), empleado.getEmail());
        return mapper.toResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public TurnoTrabajoResponseDTO obtener(UUID id) {
        RrhhTurnoTrabajo turno = buscarTurno(id);
        exigirAdminODueno(usuarioActual(), turno.getUser().getId());
        return mapper.toResponseDto(turno);
    }

    @Override
    public TurnoTrabajoResponseDTO actualizar(UUID id, TurnoTrabajoRequestDTO r) {
        exigirAdmin(usuarioActual());
        RrhhTurnoTrabajo turno = buscarTurno(id);
        if (!turno.getUser().getId().equals(r.userId())) {
            throw new IllegalArgumentException("No se puede cambiar el empleado de un turno; cree uno nuevo para el otro empleado");
        }
        Horario h = validarHorario(r.nombreTurno(), r.horaInicio(), r.horaFin(), r.diasSemana(), r.toleranciaMinutos());
        boolean quedaActivo = r.activo() != null ? r.activo() : Boolean.TRUE.equals(turno.getActivo());
        if (quedaActivo) {
            verificarCruces(turno.getUser(), h, turno.getId(), turnoRepository.findActivosByUserId(turno.getUser().getId()));
        }
        Map<String, Object> antes = snapshot(turno);

        mapper.updateEntityFromDto(r, turno);
        Sede sede = r.sedeId() != null ? resolverSede(r.sedeId(), turno.getUser()) : turno.getSede();
        aplicar(turno, h, turno.getUser(), sede);
        turno.setActivo(quedaActivo);

        RrhhTurnoTrabajo guardado = turnoRepository.save(turno);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("antes", antes);
        datos.put("despues", snapshot(guardado));
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.UPDATE, datos);
        return mapper.toResponseDto(guardado);
    }

    @Override
    public void eliminar(UUID id) {
        exigirAdmin(usuarioActual());
        RrhhTurnoTrabajo turno = buscarTurno(id);
        if (!Boolean.TRUE.equals(turno.getActivo())) {
            throw new IllegalStateException("El turno ya está inactivo");
        }
        turno.setActivo(false);
        turnoRepository.save(turno);
        Map<String, Object> datos = snapshot(turno);
        datos.put("operacion", "BAJA_LOGICA");
        auditService.registrar(TABLA, id, AccionAudit.DELETE, datos);
        log.info("Turno desactivado: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TurnoTrabajoResponseDTO> buscar(UUID sedeId, UUID userId, Boolean activo, String nombreTurno, Pageable pageable) {
        exigirAdmin(usuarioActual());
        String patron = nombreTurno == null || nombreTurno.isBlank() ? "%"
                : "%" + nombreTurno.trim().toLowerCase(Locale.ROOT) + "%";
        return turnoRepository.search(sedeId, userId, activo, patron, pageable).map(mapper::toResponseDto);
    }

    @Override
    public AsignacionTurnoResponseDTO asignar(AsignarTurnoRequestDTO r) {
        exigirAdmin(usuarioActual());
        Horario h = validarHorario(r.nombreTurno(), r.horaInicio(), r.horaFin(), r.diasSemana(), r.toleranciaMinutos());

        // 1) Validar a TODOS antes de guardar nada (todo o nada)
        List<String> errores = new ArrayList<>();
        List<User> empleados = new ArrayList<>();
        for (UUID userId : new LinkedHashSet<>(r.userIds())) {
            User u = userRepository.findById(userId).orElse(null);
            if (u == null) { errores.add(userId + ": empleado no encontrado"); continue; }
            if (!Boolean.TRUE.equals(u.getActivo())) { errores.add(nombre(u) + ": empleado inactivo"); continue; }
            String cruce = describirCruce(h, null, turnoRepository.findActivosByUserId(u.getId()));
            if (cruce != null) { errores.add(nombre(u) + ": " + cruce); continue; }
            empleados.add(u);
        }
        if (!errores.isEmpty()) {
            throw new IllegalStateException("No se asignó ningún turno. " + String.join("; ", errores));
        }

        // 2) Crear
        Sede sedeComun = r.sedeId() != null ? sede(r.sedeId()) : null;
        List<TurnoTrabajoResponseDTO> creados = new ArrayList<>();
        for (User u : empleados) {
            RrhhTurnoTrabajo turno = RrhhTurnoTrabajo.builder().activo(true).build();
            aplicar(turno, h, u, sedeComun != null ? sedeComun : resolverSede(null, u));
            RrhhTurnoTrabajo guardado = turnoRepository.save(turno);
            Map<String, Object> datos = snapshot(guardado);
            datos.put("operacion", "ASIGNACION_MASIVA");
            auditService.registrar(TABLA, guardado.getId(), AccionAudit.INSERT, datos);
            creados.add(mapper.toResponseDto(guardado));
        }
        log.info("Turno '{}' asignado a {} empleado(s)", h.nombre(), creados.size());
        return new AsignacionTurnoResponseDTO(creados.size(), creados);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TurnoTrabajoResponseDTO> porEmpleado(UUID userId) {
        exigirAdminODueno(usuarioActual(), userId);
        return turnoRepository.findActivosByUserId(userId).stream()
                .sorted(Comparator.comparing((RrhhTurnoTrabajo t) -> primerDia(t.getDiasSemana()))
                        .thenComparing(RrhhTurnoTrabajo::getHoraInicio))
                .map(mapper::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TurnoDashboardDTO dashboard(UUID sedeId) {
        exigirAdmin(usuarioActual());
        List<RrhhTurnoTrabajo> turnos = turnoRepository.findActivosPorSede(sedeId);

        Map<String, Long> porNombre = new TreeMap<>();
        Map<String, Long> cobertura = new LinkedHashMap<>();
        for (DayOfWeek d : DayOfWeek.values()) cobertura.put(HorarioUtil.nombre(d), 0L);
        Map<UUID, Set<String>> diasPorEmpleado = new HashMap<>();
        Map<UUID, Integer> minutosPorEmpleado = new HashMap<>();
        Map<UUID, String> nombres = new HashMap<>();
        long minutosTotales = 0;

        for (RrhhTurnoTrabajo t : turnos) {
            UUID uid = t.getUser().getId();
            nombres.put(uid, nombre(t.getUser()));
            porNombre.merge(t.getNombreTurno().toUpperCase(Locale.ROOT), 1L, Long::sum);
            Set<String> dias = new HashSet<>();
            if (t.getDiasSemana() != null) {
                for (String d : t.getDiasSemana()) {
                    DayOfWeek dow = HorarioUtil.aDayOfWeekONull(d);
                    if (dow != null) dias.add(HorarioUtil.nombre(dow));
                }
            }
            int minutos = HorarioUtil.duracionMinutos(t.getHoraInicio(), t.getHoraFin()) * dias.size();
            minutosTotales += minutos;
            minutosPorEmpleado.merge(uid, minutos, Integer::sum);
            diasPorEmpleado.computeIfAbsent(uid, k -> new HashSet<>()).addAll(dias);
        }
        // Cobertura: un empleado cuenta una vez por día aunque tenga dos turnos ese día
        for (Set<String> dias : diasPorEmpleado.values()) {
            for (String d : dias) cobertura.merge(d, 1L, Long::sum);
        }
        List<String> sobre48 = minutosPorEmpleado.entrySet().stream()
                .filter(e -> e.getValue() > MAX_MINUTOS_SEMANA)
                .map(e -> nombres.get(e.getKey()) + " (" + horas(e.getValue()).toPlainString() + " h)")
                .sorted().toList();

        long sinTurno = 0;
        for (RrhhLegajoDigital l : legajoRepository.findActivos(sedeId)) {
            if (!diasPorEmpleado.containsKey(l.getUser().getId())) sinTurno++;
        }

        return new TurnoDashboardDTO(sedeId, turnos.size(), diasPorEmpleado.size(), sinTurno,
                horas(minutosTotales), porNombre, cobertura, sobre48);
    }

    /** Sin readOnly: se ejecuta dentro de la transacción de la marcación de asistencia. */
    @Override
    public Optional<TurnoDelDiaDTO> turnoDelDia(UUID userId, LocalDateTime marcacion) {
        DayOfWeek hoy = marcacion.getDayOfWeek();
        LocalTime hora = marcacion.toLocalTime();
        return turnoRepository.findActivosByUserId(userId).stream()
                .filter(t -> HorarioUtil.incluyeDia(t.getDiasSemana(), hoy))
                .min(Comparator.comparingInt(t -> Math.abs(TurnoDelDiaDTO.minutosDeDiferencia(t.getHoraInicio(), hora))))
                .map(t -> new TurnoDelDiaDTO(t.getId(), t.getNombreTurno(), t.getHoraInicio(), t.getToleranciaSegura()));
    }

    // ============ VALIDACIONES ============

    /** Horario ya validado y normalizado. */
    record Horario(String nombre, LocalTime inicio, LocalTime fin, List<String> dias, int tolerancia) { }

    Horario validarHorario(String nombre, LocalTime inicio, LocalTime fin, List<String> dias, Integer tolerancia) {
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("El nombre del turno es obligatorio");
        if (nombre.trim().length() > 50) throw new IllegalArgumentException("El nombre del turno no puede superar 50 caracteres");
        if (inicio == null || fin == null) throw new IllegalArgumentException("Las horas de inicio y fin son obligatorias");
        if (inicio.equals(fin)) throw new IllegalArgumentException("La hora de inicio y fin no pueden ser iguales");
        int tol = tolerancia != null ? tolerancia : RrhhTurnoTrabajo.TOLERANCIA_DEFAULT_MINUTOS;
        if (tol < 0 || tol > 120) throw new IllegalArgumentException("La tolerancia debe estar entre 0 y 120 minutos");
        if (tol >= HorarioUtil.duracionMinutos(inicio, fin)) {
            throw new IllegalArgumentException("La tolerancia no puede ser igual o mayor que la duración del turno");
        }
        return new Horario(nombre.trim().toUpperCase(Locale.ROOT), inicio, fin, HorarioUtil.normalizarDias(dias), tol);
    }

    private void verificarCruces(User empleado, Horario h, UUID excluirId, List<RrhhTurnoTrabajo> existentes) {
        String cruce = describirCruce(h, excluirId, existentes);
        if (cruce != null) throw new IllegalStateException(nombre(empleado) + ": " + cruce);
    }

    /** null si no hay cruce; si lo hay, describe con qué turno. */
    private String describirCruce(Horario h, UUID excluirId, List<RrhhTurnoTrabajo> existentes) {
        for (RrhhTurnoTrabajo t : existentes) {
            if (t.getId().equals(excluirId)) continue;
            if (HorarioUtil.seCruzan(h.dias(), h.inicio(), h.fin(), t.getDiasSemana(), t.getHoraInicio(), t.getHoraFin())) {
                return "se cruza con el turno " + t.getNombreTurno() + " " + t.getHoraInicio() + "-" + t.getHoraFin()
                        + " " + t.getDiasSemana();
            }
        }
        return null;
    }

    private void aplicar(RrhhTurnoTrabajo turno, Horario h, User empleado, Sede sede) {
        turno.setUser(empleado);
        turno.setSede(sede);
        turno.setNombreTurno(h.nombre());
        turno.setHoraInicio(h.inicio());
        turno.setHoraFin(h.fin());
        turno.setDiasSemana(h.dias());
        turno.setToleranciaMinutos(h.tolerancia());
    }

    // ============ HELPERS ============

    private User empleadoActivo(UUID userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + userId));
        if (!Boolean.TRUE.equals(u.getActivo())) {
            throw new IllegalStateException("El empleado " + nombre(u) + " está inactivo");
        }
        return u;
    }

    private Sede resolverSede(UUID sedeId, User empleado) {
        Sede s = sedeId != null ? sede(sedeId) : empleado.getSede();
        if (s == null) throw new IllegalArgumentException("Debe indicar la sede del turno");
        if (Boolean.FALSE.equals(s.getActiva())) throw new IllegalStateException("La sede " + s.getNombre() + " está inactiva");
        return s;
    }

    private Sede sede(UUID sedeId) {
        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + sedeId));
    }

    private RrhhTurnoTrabajo buscarTurno(UUID id) {
        return turnoRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado: " + id));
    }

    private static int primerDia(List<String> dias) {
        if (dias == null || dias.isEmpty()) return 8;
        DayOfWeek d = HorarioUtil.aDayOfWeekONull(dias.get(0));
        return d == null ? 8 : d.getValue();
    }

    private static BigDecimal horas(long minutos) {
        return BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> snapshot(RrhhTurnoTrabajo t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", t.getUser() != null ? t.getUser().getId().toString() : null);
        m.put("nombreTurno", t.getNombreTurno());
        m.put("horaInicio", t.getHoraInicio() != null ? t.getHoraInicio().toString() : null);
        m.put("horaFin", t.getHoraFin() != null ? t.getHoraFin().toString() : null);
        m.put("diasSemana", t.getDiasSemana());
        m.put("toleranciaMinutos", t.getToleranciaMinutos());
        m.put("activo", t.getActivo());
        return m;
    }

    private String nombre(User u) {
        return u.getNombre() + " " + u.getApellido();
    }

    private boolean esAdmin(User user) {
        return user.getRol() != null && user.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    private void exigirAdmin(User user) {
        if (!esAdmin(user)) throw new AccessDeniedException("Solo un administrador puede gestionar turnos");
    }

    private void exigirAdminODueno(User user, UUID duenoId) {
        if (!esAdmin(user) && !user.getId().equals(duenoId)) {
            throw new AccessDeniedException("Solo puede consultar sus propios turnos");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
