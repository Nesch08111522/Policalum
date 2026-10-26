package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.CuentaFinancieraRequestDTO;
import com.policalum.erp.application.dto.request.RegistrarSaldoCuentaRequestDTO;
import com.policalum.erp.application.dto.response.CuentaFinancieraResponseDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaResponseDTO;
import com.policalum.erp.application.mapper.CuentaFinancieraMapper;
import com.policalum.erp.application.mapper.SaldoCuentaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.CuentaFinancieraService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.CuentaFinanciera;
import com.policalum.erp.domain.model.SaldoCuentaFinanciera;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CuentaFinancieraRepository;
import com.policalum.erp.infrastructure.repository.SaldoCuentaFinancieraRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
public class CuentaFinancieraServiceImpl implements CuentaFinancieraService {

    private static final String TABLA = "finanzas_cuentas";

    private final CuentaFinancieraRepository cuentaRepository;
    private final SaldoCuentaFinancieraRepository saldoRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final CuentaFinancieraMapper mapper;
    private final SaldoCuentaMapper saldoMapper;
    private final AuditService auditService;

    @Override
    public CuentaFinancieraResponseDTO crear(CuentaFinancieraRequestDTO request) {
        User admin = exigirAdmin();
        CuentaFinanciera cuenta = mapper.toEntity(request);
        cuenta.setNombre(request.nombre().trim());
        cuenta.setSede(sede(request.sedeId()));
        cuenta.setMetodosPago(metodos(request.metodosPago()));
        cuenta.setActiva(true);
        cuenta.setCreadoPor(admin);
        validarConflictos(cuenta, null);

        CuentaFinanciera guardada = cuentaRepository.save(cuenta);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.INSERT, datos(guardada, "CREACION"));
        log.info("Cuenta financiera creada: {} ({})", guardada.getNombre(), guardada.getTipo());
        return mapper.toResponseDto(guardada, null);
    }

    @Override
    public CuentaFinancieraResponseDTO actualizar(UUID id, CuentaFinancieraRequestDTO request) {
        exigirAdmin();
        CuentaFinanciera cuenta = buscar(id);
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new IllegalStateException("La cuenta " + cuenta.getNombre() + " está inactiva");
        }
        mapper.updateEntityFromDto(request, cuenta);
        cuenta.setNombre(request.nombre().trim());
        cuenta.setSede(sede(request.sedeId()));
        cuenta.setMetodosPago(metodos(request.metodosPago()));
        validarConflictos(cuenta, cuenta.getId());

        CuentaFinanciera guardada = cuentaRepository.save(cuenta);
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datos(guardada, "ACTUALIZACION"));
        return mapper.toResponseDto(guardada, ultimoSaldo(guardada.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaFinancieraResponseDTO obtener(UUID id) {
        exigirAdmin();
        CuentaFinanciera cuenta = buscar(id);
        return mapper.toResponseDto(cuenta, ultimoSaldo(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaFinancieraResponseDTO> listar(UUID sedeId, boolean incluirInactivas) {
        exigirAdmin();
        List<CuentaFinanciera> cuentas = incluirInactivas
                ? cuentaRepository.findAllByOrderByNombreAsc()
                : cuentaRepository.findByActivaTrueOrderByNombreAsc();
        return cuentas.stream()
                .filter(c -> sedeId == null || (c.getSede() != null && sedeId.equals(c.getSede().getId())))
                .map(c -> mapper.toResponseDto(c, ultimoSaldo(c.getId())))
                .toList();
    }

    @Override
    public void desactivar(UUID id) {
        exigirAdmin();
        CuentaFinanciera cuenta = buscar(id);
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new IllegalStateException("La cuenta " + cuenta.getNombre() + " ya está inactiva");
        }
        cuenta.setActiva(false);
        cuentaRepository.save(cuenta);
        auditService.registrar(TABLA, cuenta.getId(), AccionAudit.DELETE, datos(cuenta, "DESACTIVACION"));
        log.info("Cuenta financiera desactivada: {}", cuenta.getNombre());
    }

    @Override
    public SaldoCuentaResponseDTO registrarSaldo(UUID cuentaId, RegistrarSaldoCuentaRequestDTO request) {
        User admin = exigirAdmin();
        CuentaFinanciera cuenta = buscar(cuentaId);
        if (!Boolean.TRUE.equals(cuenta.getActiva())) {
            throw new IllegalStateException("No se puede registrar saldo en una cuenta inactiva");
        }
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime corte = corte(request.fechaCorte(), ahora);

        SaldoCuentaFinanciera saldo = saldoMapper.toEntity(request);
        saldo.setSaldo(request.saldo().setScale(2, java.math.RoundingMode.HALF_UP));
        saldo.setFechaCorte(corte);
        saldo.setObservacion(request.observacion() != null && !request.observacion().isBlank() ? request.observacion().trim() : null);
        saldo.setCuenta(cuenta);
        saldo.setRegistradoPor(admin);
        SaldoCuentaFinanciera guardado = saldoRepository.save(saldo);

        Map<String, Object> d = datos(cuenta, "REGISTRO_SALDO");
        d.put("saldo", guardado.getSaldo());
        d.put("fechaCorte", guardado.getFechaCorte().toString());
        auditService.registrar(TABLA, cuenta.getId(), AccionAudit.UPDATE, d);
        return saldoMapper.toResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaldoCuentaResponseDTO> historialSaldos(UUID cuentaId, Pageable pageable) {
        exigirAdmin();
        if (!cuentaRepository.existsById(cuentaId)) {
            throw new ResourceNotFoundException("Cuenta financiera no encontrada: " + cuentaId);
        }
        return saldoRepository.findByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(cuentaId, pageable)
                .map(saldoMapper::toResponseDto);
    }

    // ============ HELPERS ============

    /**
     * Un método de pago solo puede caer en una cuenta por sede; si no, el sistema no sabría
     * a qué cuenta sumar un cobro por Yape de la sede Centro.
     */
    void validarConflictos(CuentaFinanciera cuenta, UUID excluirId) {
        if (cuenta.getMetodosPago().isEmpty()) return;
        for (CuentaFinanciera otra : cuentaRepository.findByActivaTrueOrderByNombreAsc()) {
            if (otra.getId().equals(excluirId) || !cuenta.compartePosibleSede(otra)) continue;
            Set<MetodoPago> repetidos = new TreeSet<>(cuenta.getMetodosPago());
            repetidos.retainAll(otra.getMetodosPago());
            if (!repetidos.isEmpty()) {
                throw new IllegalStateException("Los métodos " + repetidos + " ya están asignados a la cuenta '"
                        + otra.getNombre() + "'" + (otra.getSede() != null ? " (sede " + otra.getSede().getNombre() + ")" : " (corporativa)")
                        + ". Un método solo puede ir a una cuenta por sede.");
            }
        }
    }

    private Set<MetodoPago> metodos(Set<MetodoPago> solicitados) {
        if (solicitados == null || solicitados.isEmpty()) return new HashSet<>();
        return new HashSet<>(EnumSet.copyOf(solicitados));
    }

    private Sede sede(UUID sedeId) {
        if (sedeId == null) return null;
        return sedeRepository.findById(sedeId).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + sedeId));
    }

    private CuentaFinanciera buscar(UUID id) {
        return cuentaRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta financiera no encontrada: " + id));
    }

    /** Acepta YYYY-MM-DD del formulario o una fecha-hora. */
    private LocalDateTime corte(String texto, LocalDateTime ahora) {
        if (texto == null || texto.isBlank()) return ahora;
        try {
            if (texto.length() == 10) {
                LocalDate dia = LocalDate.parse(texto);
                if (dia.isAfter(ahora.toLocalDate())) throw new IllegalArgumentException("La fecha de corte no puede ser futura");
                return dia.atStartOfDay();
            }
            LocalDateTime fecha = LocalDateTime.parse(texto);
            if (fecha.isAfter(ahora)) throw new IllegalArgumentException("La fecha de corte no puede ser futura");
            return fecha;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("La fecha de corte no es válida");
        }
    }

    private SaldoCuentaFinanciera ultimoSaldo(UUID cuentaId) {
        return saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(cuentaId).orElse(null);
    }

    private Map<String, Object> datos(CuentaFinanciera c, String operacion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operacion", operacion);
        m.put("nombre", c.getNombre());
        m.put("tipo", c.getTipo() != null ? c.getTipo().name() : null);
        m.put("sedeId", c.getSede() != null ? c.getSede().getId().toString() : null);
        m.put("metodosPago", c.getMetodosPago() != null ? new TreeSet<>(c.getMetodosPago()).toString() : "[]");
        return m;
    }

    private User exigirAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
        if (u.getRol() == null || u.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede gestionar las cuentas financieras");
        }
        return u;
    }
}
