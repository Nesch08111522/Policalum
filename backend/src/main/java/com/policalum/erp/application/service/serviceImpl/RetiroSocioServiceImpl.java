package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.AnularRetiroRequestDTO;
import com.policalum.erp.application.dto.request.RetiroSocioRequestDTO;
import com.policalum.erp.application.dto.response.RetiroPorAdminDTO;
import com.policalum.erp.application.dto.response.RetiroSocioDashboardDTO;
import com.policalum.erp.application.dto.response.RetiroSocioListadoDTO;
import com.policalum.erp.application.dto.response.RetiroSocioResponseDTO;
import com.policalum.erp.application.mapper.RetiroSocioMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.RetiroSocioService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.OrigenFondos;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.FinanzasRetiroSocio;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.FinanzasRetiroSocioRepository;
import com.policalum.erp.infrastructure.repository.PagoVentaRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.TurnoCajaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorOrigenProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class RetiroSocioServiceImpl implements RetiroSocioService {

    private static final String TABLA = "finanzas_retiros_socio";
    /** Límites usados cuando no se envían fechas (evita parámetros timestamp nulos en PostgreSQL). */
    static final LocalDateTime FECHA_MIN = LocalDateTime.of(2000, 1, 1, 0, 0);
    static final LocalDateTime FECHA_MAX = LocalDateTime.of(2100, 1, 1, 0, 0);
    private static final DateTimeFormatter FMT_OBS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final FinanzasRetiroSocioRepository retiroRepository;
    private final TurnoCajaRepository turnoRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final RetiroSocioMapper mapper;
    private final AuditService auditService;

    @Override
    public RetiroSocioResponseDTO registrar(RetiroSocioRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);

        Sede sede = resolverSede(request.sedeId(), admin);
        BigDecimal monto = request.montoRetirado().setScale(2, RoundingMode.HALF_UP);

        FinanzasRetiroSocio retiro = mapper.toEntity(request);
        retiro.setMontoRetirado(monto);
        retiro.setAdmin(admin);
        retiro.setSede(sede);
        retiro.setEstado(EstadoRetiro.REGISTRADO);
        retiro.setFechaRetiro(LocalDateTime.now());
        retiro.setImpactoCajaRevertido(false);

        TurnoCaja turno = null;
        if (request.origenFondos() == OrigenFondos.CAJA_FISICA) {
            // SELECT ... FOR UPDATE: serializa retiros/cierres concurrentes sobre el mismo turno
            turno = turnoRepository.findParaActualizar(admin.getId(), sede.getId(), EstadoTurno.ABIERTO)
                    .orElseThrow(() -> new IllegalStateException(
                            "Para retirar de caja física debe tener un turno de caja ABIERTO en la sede "
                                    + sede.getNombre()));

            BigDecimal disponible = efectivoDisponible(turno);
            if (monto.compareTo(disponible) > 0) {
                throw new IllegalStateException("Efectivo insuficiente en caja. Disponible: S/ "
                        + disponible.setScale(2, RoundingMode.HALF_UP).toPlainString()
                        + ", solicitado: S/ " + monto.toPlainString());
            }
            turno.setTotalRetirosSocios(turno.getTotalRetirosSociosSeguro().add(monto));
            retiro.setTurnoCaja(turno);
        }

        FinanzasRetiroSocio guardado = retiroRepository.save(retiro);

        if (turno != null) {
            agregarObservacion(turno, "[RETIRO SOCIO] -S/ " + monto.toPlainString()
                    + " | " + nombre(admin) + " | " + guardado.getFechaRetiro().format(FMT_OBS)
                    + " | retiro " + guardado.getId());
            turnoRepository.save(turno);
        }

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("monto", monto);
        datos.put("origenFondos", request.origenFondos().name());
        datos.put("sedeId", sede.getId().toString());
        if (turno != null) datos.put("turnoCajaId", turno.getId().toString());
        if (request.descripcion() != null) datos.put("descripcion", request.descripcion());
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.INSERT, datos);

        log.info("Retiro de socio registrado: {} - S/ {} - {} - admin {}",
                guardado.getId(), monto, request.origenFondos(), admin.getEmail());

        return mapper.toResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public RetiroSocioResponseDTO obtener(UUID id) {
        exigirAdmin(usuarioActual());
        return mapper.toResponseDto(buscarRetiro(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetiroSocioListadoDTO> buscar(UUID adminId, UUID sedeId, EstadoRetiro estado, OrigenFondos origen,
                                              LocalDate desde, LocalDate hasta, Pageable pageable) {
        exigirAdmin(usuarioActual());
        // TODO(Módulo 5.1): cuando haya múltiples socios, forzar adminId = usuarioActual.getId()
        // para que cada socio solo vea sus propios retiros.
        validarRango(desde, hasta);
        return retiroRepository.search(adminId, sedeId, estado, origen,
                        inicio(desde), finExclusivo(hasta), pageable)
                .map(mapper::toListadoDto);
    }

    @Override
    public RetiroSocioResponseDTO anular(UUID id, AnularRetiroRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);

        FinanzasRetiroSocio retiro = buscarRetiro(id);
        if (retiro.getEstado() == EstadoRetiro.ANULADO) {
            throw new IllegalStateException("El retiro ya está anulado");
        }

        boolean revertido = false;
        if (retiro.getTurnoCaja() != null) {
            TurnoCaja turno = turnoRepository.findByIdParaActualizar(retiro.getTurnoCaja().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Turno de caja no encontrado"));
            if (turno.getEstado() == EstadoTurno.ABIERTO) {
                BigDecimal nuevoTotal = turno.getTotalRetirosSociosSeguro().subtract(retiro.getMontoRetirado());
                turno.setTotalRetirosSocios(nuevoTotal.max(BigDecimal.ZERO));
                agregarObservacion(turno, "[ANULACIÓN RETIRO SOCIO] +S/ "
                        + retiro.getMontoRetirado().toPlainString() + " | " + nombre(admin)
                        + " | " + LocalDateTime.now().format(FMT_OBS) + " | retiro " + retiro.getId());
                turnoRepository.save(turno);
                revertido = true;
            } else {
                log.warn("Retiro {} anulado con turno {} CERRADO: no se revierte el impacto en caja",
                        retiro.getId(), turno.getId());
            }
        }

        retiro.setEstado(EstadoRetiro.ANULADO);
        retiro.setMotivoAnulacion(request.motivo().trim());
        retiro.setAnuladoPor(admin);
        retiro.setFechaAnulacion(LocalDateTime.now());
        retiro.setImpactoCajaRevertido(revertido);
        FinanzasRetiroSocio guardado = retiroRepository.save(retiro);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("accion", "ANULACION");
        datos.put("monto", retiro.getMontoRetirado());
        datos.put("motivo", guardado.getMotivoAnulacion());
        datos.put("impactoCajaRevertido", revertido);
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.UPDATE, datos);

        log.info("Retiro de socio anulado: {} - S/ {} - caja revertida: {}",
                guardado.getId(), guardado.getMontoRetirado(), revertido);

        return mapper.toResponseDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public RetiroSocioDashboardDTO dashboard(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin(usuarioActual());
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        validarRango(d, h);
        LocalDateTime ini = inicio(d);
        LocalDateTime fin = finExclusivo(h);

        BigDecimal totalCaja = BigDecimal.ZERO;
        BigDecimal totalBanco = BigDecimal.ZERO;
        long cantidad = 0;
        for (RetiroPorOrigenProjection p : retiroRepository.totalesPorOrigen(EstadoRetiro.REGISTRADO, sedeId, ini, fin)) {
            BigDecimal total = cero(p.getTotal());
            cantidad += p.getCantidad() != null ? p.getCantidad() : 0;
            if (p.getOrigen() == OrigenFondos.CAJA_FISICA) totalCaja = totalCaja.add(total);
            else if (p.getOrigen() == OrigenFondos.CUENTA_BANCARIA) totalBanco = totalBanco.add(total);
        }

        List<RetiroPorAdminDTO> porAdmin = retiroRepository
                .totalesPorAdmin(EstadoRetiro.REGISTRADO, sedeId, ini, fin).stream()
                .map(p -> new RetiroPorAdminDTO(p.getAdminId(), p.getNombre() + " " + p.getApellido(),
                        p.getCantidad() != null ? p.getCantidad() : 0, cero(p.getTotal())))
                .toList();

        long anulados = retiroRepository.contarPorEstado(EstadoRetiro.ANULADO, sedeId, ini, fin);

        return new RetiroSocioDashboardDTO(sedeId, d, h, totalCaja.add(totalBanco), cantidad, anulados,
                totalCaja, totalBanco, porAdmin);
    }

    // ============ HELPERS ============

    /** Efectivo físico en el turno = inicial + ventas en efectivo - retiros de socios - reembolsos (M17). */
    private BigDecimal efectivoDisponible(TurnoCaja turno) {
        BigDecimal ventasEfectivo = cero(pagoVentaRepository.sumMontoByTurnoAndMetodo(turno.getId(), MetodoPago.EFECTIVO));
        return cero(turno.getMontoInicial()).add(ventasEfectivo).subtract(turno.getEgresosEfectivoSeguro());
    }

    private Sede resolverSede(UUID sedeId, User admin) {
        UUID id = sedeId != null ? sedeId : admin.getSede().getId();
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + id));
        if (!Boolean.TRUE.equals(sede.getActiva())) {
            throw new IllegalStateException("La sede " + sede.getNombre() + " está inactiva");
        }
        return sede;
    }

    private FinanzasRetiroSocio buscarRetiro(UUID id) {
        return retiroRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Retiro de socio no encontrado: " + id));
    }

    private void agregarObservacion(TurnoCaja turno, String linea) {
        String actual = turno.getObservaciones();
        turno.setObservaciones(actual == null || actual.isBlank() ? linea : actual + "\n" + linea);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
    }

    private LocalDateTime inicio(LocalDate desde) {
        return desde != null ? desde.atStartOfDay() : FECHA_MIN;
    }

    private LocalDateTime finExclusivo(LocalDate hasta) {
        return hasta != null ? hasta.plusDays(1).atStartOfDay() : FECHA_MAX;
    }

    private BigDecimal cero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String nombre(User user) {
        return user.getNombre() + " " + user.getApellido();
    }

    /** Defensa en profundidad: además del @PreAuthorize del controller. */
    private void exigirAdmin(User user) {
        if (user.getRol() == null || user.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede gestionar retiros de socios");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
