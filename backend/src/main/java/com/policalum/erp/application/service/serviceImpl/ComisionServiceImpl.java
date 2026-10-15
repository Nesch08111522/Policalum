package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.CalcularComisionesRequestDTO;
import com.policalum.erp.application.dto.response.CalculoComisionesResponseDTO;
import com.policalum.erp.application.dto.response.ComisionResponseDTO;
import com.policalum.erp.application.mapper.ComisionMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.ComisionService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoComision;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.RrhhComision;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhComisionRepository;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDigitalRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import com.policalum.erp.infrastructure.repository.projection.VentasPorVendedorProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
public class ComisionServiceImpl implements ComisionService {

    private static final String TABLA = "rrhh_comisiones";
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final RrhhComisionRepository comisionRepository;
    private final RrhhLegajoDigitalRepository legajoRepository;
    private final VentaRepository ventaRepository;
    private final UserRepository userRepository;
    private final ComisionMapper mapper;
    private final AuditService auditService;

    @Override
    public CalculoComisionesResponseDTO calcular(CalcularComisionesRequestDTO request) {
        exigirAdmin(usuarioActual());

        YearMonth mes = parsePeriodo(request.periodo());
        if (mes.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException("No se pueden calcular comisiones de un periodo futuro: " + mes);
        }
        String periodo = mes.toString();
        LocalDateTime desde = mes.atDay(1).atStartOfDay();
        LocalDateTime hasta = mes.plusMonths(1).atDay(1).atStartOfDay();

        Map<UUID, VentasPorVendedorProjection> ventas = new HashMap<>();
        for (VentasPorVendedorProjection v : ventaRepository.totalesPorVendedor(EstadoVenta.COMPLETADA, desde, hasta)) {
            ventas.put(v.getUserId(), v);
        }

        List<String> advertencias = new ArrayList<>();
        List<ComisionResponseDTO> resultado = new ArrayList<>();
        Set<UUID> conLegajo = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        int nuevas = 0, actualizadas = 0, omitidas = 0;

        for (RrhhLegajoDigital legajo : legajoRepository.findActivos(request.sedeId())) {
            User vendedor = legajo.getUser();
            conLegajo.add(vendedor.getId());
            BigDecimal pct = legajo.getPorcentajeComision() != null ? legajo.getPorcentajeComision() : BigDecimal.ZERO;
            if (pct.signum() <= 0) continue; // no comisiona

            VentasPorVendedorProjection v = ventas.get(vendedor.getId());
            BigDecimal neto = v != null && v.getNeto() != null ? v.getNeto().setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2);
            long cantidad = v != null && v.getCantidad() != null ? v.getCantidad() : 0L;
            BigDecimal monto = neto.multiply(pct).divide(CIEN, 2, RoundingMode.HALF_UP);

            RrhhComision comision = comisionRepository.findByUser_IdAndPeriodo(vendedor.getId(), periodo).orElse(null);
            if (comision != null && comision.getEstado() != EstadoComision.CALCULADA) {
                omitidas++;
                advertencias.add(vendedor.getNombre() + " " + vendedor.getApellido()
                        + ": comisión ya " + comision.getEstado() + ", no se recalcula");
                continue;
            }
            boolean esNueva = comision == null;
            if (esNueva) {
                comision = RrhhComision.builder().user(vendedor).sede(vendedor.getSede()).periodo(periodo)
                        .estado(EstadoComision.CALCULADA).build();
            }
            comision.setTotalVentasNetas(neto);
            comision.setCantidadVentas(cantidad);
            comision.setPorcentajeComision(pct);
            comision.setMontoComision(monto);
            RrhhComision guardada = comisionRepository.save(comision);

            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("operacion", esNueva ? "CALCULO" : "RECALCULO");
            datos.put("periodo", periodo);
            datos.put("ventasNetas", neto);
            datos.put("porcentaje", pct);
            datos.put("monto", monto);
            auditService.registrar(TABLA, guardada.getId(), esNueva ? AccionAudit.INSERT : AccionAudit.UPDATE, datos);

            if (esNueva) nuevas++; else actualizadas++;
            total = total.add(monto);
            resultado.add(mapper.toResponseDto(guardada));
        }

        if (request.sedeId() == null) {
            long sinLegajo = ventas.keySet().stream().filter(id -> !conLegajo.contains(id)).count();
            if (sinLegajo > 0) {
                advertencias.add(sinLegajo + " usuario(s) con ventas en el periodo no tienen legajo activo; no se les calculó comisión");
            }
        }

        log.info("Comisiones {}: {} nuevas, {} recalculadas, {} omitidas, total S/ {}",
                periodo, nuevas, actualizadas, omitidas, total);
        return new CalculoComisionesResponseDTO(periodo, request.sedeId(), nuevas, actualizadas, omitidas,
                total, advertencias, resultado);
    }

    @Override
    public ComisionResponseDTO aprobar(UUID id) {
        User admin = usuarioActual();
        exigirAdmin(admin);

        RrhhComision comision = comisionRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comisión no encontrada: " + id));
        if (comision.getEstado() != EstadoComision.CALCULADA) {
            throw new IllegalStateException("Solo se puede aprobar una comisión CALCULADA (estado actual: "
                    + comision.getEstado() + ")");
        }
        comision.setEstado(EstadoComision.APROBADA);
        comision.setAprobadoPor(admin);
        comision.setFechaAprobacion(LocalDateTime.now());
        RrhhComision guardada = comisionRepository.save(comision);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("operacion", "APROBACION");
        datos.put("periodo", guardada.getPeriodo());
        datos.put("monto", guardada.getMontoComision());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datos);

        log.info("Comisión aprobada: {} - {} - S/ {}", guardada.getId(), guardada.getPeriodo(), guardada.getMontoComision());
        return mapper.toResponseDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComisionResponseDTO> buscar(String periodo, UUID sedeId, EstadoComision estado, Pageable pageable) {
        exigirAdmin(usuarioActual());
        String p = periodo == null || periodo.isBlank() ? null : parsePeriodo(periodo).toString();
        return comisionRepository.search(p, sedeId, estado, null, pageable).map(mapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ComisionResponseDTO> porEmpleado(UUID userId, Pageable pageable) {
        User actual = usuarioActual();
        if (!esAdmin(actual) && !actual.getId().equals(userId)) {
            throw new AccessDeniedException("Solo puede consultar sus propias comisiones");
        }
        return comisionRepository.search(null, null, null, userId, pageable).map(mapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComisionResponseDTO> porPeriodo(String periodo, UUID sedeId) {
        exigirAdmin(usuarioActual());
        return comisionRepository.findByPeriodo(parsePeriodo(periodo).toString(), sedeId).stream()
                .map(mapper::toResponseDto).toList();
    }

    // ============ HELPERS ============

    private YearMonth parsePeriodo(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Periodo inválido: '" + periodo + "'. Formato esperado: YYYY-MM");
        }
    }

    private boolean esAdmin(User user) {
        return user.getRol() != null && user.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    private void exigirAdmin(User user) {
        if (!esAdmin(user)) {
            throw new AccessDeniedException("Solo un administrador puede gestionar comisiones");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
