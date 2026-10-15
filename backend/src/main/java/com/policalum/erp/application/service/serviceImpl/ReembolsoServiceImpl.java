package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.ProcesarReembolsoRequestDTO;
import com.policalum.erp.application.dto.request.ReembolsoRequestDTO;
import com.policalum.erp.application.dto.response.ReembolsoResponseDTO;
import com.policalum.erp.application.mapper.ReembolsoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ReembolsoService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoDevolucion;
import com.policalum.erp.domain.enums.EstadoReembolso;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.MetodoReembolso;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Devolucion;
import com.policalum.erp.domain.model.Reembolso;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.DevolucionRepository;
import com.policalum.erp.infrastructure.repository.PagoVentaRepository;
import com.policalum.erp.infrastructure.repository.ReembolsoRepository;
import com.policalum.erp.infrastructure.repository.TurnoCajaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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
public class ReembolsoServiceImpl implements ReembolsoService {

    private static final String TABLA = "reembolsos";
    static final LocalDateTime FECHA_MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    static final LocalDateTime FECHA_MAX = LocalDateTime.of(2100, 1, 1, 0, 0);
    private static final DateTimeFormatter FMT_OBS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ReembolsoRepository reembolsoRepository;
    private final DevolucionRepository devolucionRepository;
    private final TurnoCajaRepository turnoRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final UserRepository userRepository;
    private final ReembolsoMapper mapper;
    private final AuditService auditService;

    @Override
    public ReembolsoResponseDTO crear(ReembolsoRequestDTO r) {
        User actual = usuarioActual();
        // FOR UPDATE sobre la devolución: evita dos reembolsos simultáneos de la misma devolución
        Devolucion dev = devolucionRepository.findByIdParaActualizar(r.devolucionId())
                .orElseThrow(() -> new ResourceNotFoundException("Devolución no encontrada: " + r.devolucionId()));
        if (dev.getEstado() != EstadoDevolucion.APROBADA) {
            throw new IllegalStateException("Solo se reembolsa una devolución APROBADA (estado: " + dev.getEstado() + ")");
        }
        if (reembolsoRepository.existsByDevolucion_Id(dev.getId())) {
            throw new IllegalStateException("La devolución " + dev.getCodigo() + " ya tiene un reembolso registrado");
        }

        BigDecimal maximo = cero(dev.getMontoTotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monto = r.monto() != null ? r.monto().setScale(2, RoundingMode.HALF_UP) : maximo;
        if (monto.signum() <= 0) throw new IllegalArgumentException("El monto del reembolso debe ser mayor a cero");
        if (monto.compareTo(maximo) > 0) {
            throw new IllegalArgumentException("El reembolso (S/ " + monto.toPlainString()
                    + ") no puede superar el total devuelto (S/ " + maximo.toPlainString() + ")");
        }
        String motivoAjuste = blankANull(r.motivoAjuste());
        if (monto.compareTo(maximo) < 0 && motivoAjuste == null) {
            throw new IllegalArgumentException("Indique el motivo para reembolsar menos del total (S/ " + maximo.toPlainString() + ")");
        }

        Reembolso reembolso = Reembolso.builder()
                .devolucion(dev).venta(dev.getVenta()).cliente(dev.getCliente())
                .montoMaximo(maximo).montoTotalReembolso(monto)
                .motivoAjuste(monto.compareTo(maximo) < 0 ? motivoAjuste : null)
                .metodoReembolso(r.metodoReembolso())
                .referencia(blankANull(r.referencia()))
                .observaciones(blankANull(r.observaciones()))
                .estado(EstadoReembolso.PENDIENTE)
                .creadoPor(actual)
                .build();
        Reembolso guardado = reembolsoRepository.save(reembolso);

        Map<String, Object> datos = datos(guardado, "REGISTRO");
        if (guardado.getMotivoAjuste() != null) datos.put("motivoAjuste", guardado.getMotivoAjuste());
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.INSERT, datos);
        log.info("Reembolso registrado para {}: S/ {} ({})", dev.getCodigo(), monto, r.metodoReembolso());
        return mapper.toResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ReembolsoResponseDTO obtener(UUID id) {
        return reembolsoRepository.findDetalleById(id).map(mapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReembolsoResponseDTO> buscar(UUID sedeId, EstadoReembolso estado, MetodoReembolso metodo,
                                             LocalDate desde, LocalDate hasta, Pageable pageable) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        return reembolsoRepository.search(sedeId, estado, metodo,
                        desde != null ? desde.atStartOfDay() : FECHA_MIN,
                        hasta != null ? hasta.plusDays(1).atStartOfDay() : FECHA_MAX, pageable)
                .map(mapper::toResponseDto);
    }

    @Override
    public ReembolsoResponseDTO procesar(UUID id, ProcesarReembolsoRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);
        Reembolso r = reembolsoRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reembolso no encontrado: " + id));
        if (r.getEstado() != EstadoReembolso.PENDIENTE) {
            throw new IllegalStateException("El reembolso ya fue procesado");
        }
        BigDecimal monto = r.getMontoTotalReembolso();

        if (r.getMetodoReembolso() == MetodoReembolso.EFECTIVO) {
            Sede sede = r.getDevolucion().getSede();
            TurnoCaja turno = turnoRepository.findParaActualizar(admin.getId(), sede.getId(), EstadoTurno.ABIERTO)
                    .orElseThrow(() -> new IllegalStateException(
                            "Para reembolsar en efectivo debe tener un turno de caja ABIERTO en la sede " + sede.getNombre()));
            BigDecimal disponible = efectivoDisponible(turno);
            if (monto.compareTo(disponible) > 0) {
                throw new IllegalStateException("Efectivo insuficiente en caja. Disponible: S/ "
                        + disponible.setScale(2, RoundingMode.HALF_UP).toPlainString() + ", reembolso: S/ " + monto.toPlainString());
            }
            turno.setTotalReembolsos(turno.getTotalReembolsosSeguro().add(monto));
            String linea = "[REEMBOLSO] -S/ " + monto.toPlainString() + " | " + r.getDevolucion().getCodigo()
                    + " | " + LocalDateTime.now().format(FMT_OBS);
            String obs = turno.getObservaciones();
            turno.setObservaciones(obs == null || obs.isBlank() ? linea : obs + "\n" + linea);
            turnoRepository.save(turno);
            r.setTurnoCaja(turno);
        }

        r.setEstado(EstadoReembolso.PROCESADO);
        r.setProcesadoPor(admin);
        r.setFechaProcesado(LocalDateTime.now());
        if (request != null && blankANull(request.referencia()) != null) r.setReferencia(request.referencia().trim());
        if (request != null && blankANull(request.observaciones()) != null) {
            String obs = r.getObservaciones();
            r.setObservaciones(obs == null ? request.observaciones().trim() : obs + "\n" + request.observaciones().trim());
        }
        Reembolso guardado = reembolsoRepository.save(r);

        Map<String, Object> datos = datos(guardado, "PROCESADO");
        if (guardado.getTurnoCaja() != null) datos.put("turnoCajaId", guardado.getTurnoCaja().getId().toString());
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.UPDATE, datos);
        log.info("Reembolso {} PROCESADO: S/ {} ({})", guardado.getId(), monto, guardado.getMetodoReembolso());
        return mapper.toResponseDto(guardado);
    }

    // ============ HELPERS ============

    /** Efectivo físico = inicial + ventas en efectivo - retiros de socios - reembolsos en efectivo. */
    private BigDecimal efectivoDisponible(TurnoCaja turno) {
        BigDecimal ventasEfectivo = cero(pagoVentaRepository.sumMontoByTurnoAndMetodo(turno.getId(), MetodoPago.EFECTIVO));
        return cero(turno.getMontoInicial()).add(ventasEfectivo).subtract(turno.getEgresosEfectivoSeguro());
    }

    private Map<String, Object> datos(Reembolso r, String operacion) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("operacion", operacion);
        m.put("devolucionId", r.getDevolucion().getId().toString());
        m.put("monto", r.getMontoTotalReembolso());
        m.put("metodo", r.getMetodoReembolso().name());
        m.put("estado", r.getEstado().name());
        return m;
    }

    private static String blankANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal cero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private void exigirAdmin(User user) {
        if (user.getRol() == null || user.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede procesar reembolsos");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
