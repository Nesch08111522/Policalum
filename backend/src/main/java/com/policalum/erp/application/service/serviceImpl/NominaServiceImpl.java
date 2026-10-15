package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.GenerarNominaRequestDTO;
import com.policalum.erp.application.dto.request.PagarNominaRequestDTO;
import com.policalum.erp.application.dto.response.BoletaConceptoDTO;
import com.policalum.erp.application.dto.response.BoletaPagoDTO;
import com.policalum.erp.application.dto.response.GeneracionNominaResponseDTO;
import com.policalum.erp.application.dto.response.NominaDashboardDTO;
import com.policalum.erp.application.dto.response.NominaListadoDTO;
import com.policalum.erp.application.dto.response.NominaResponseDTO;
import com.policalum.erp.application.mapper.NominaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.NominaService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.EstadoComision;
import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.SistemaPension;
import com.policalum.erp.domain.model.RrhhComision;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.RrhhNomina;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhComisionRepository;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDigitalRepository;
import com.policalum.erp.infrastructure.repository.RrhhNominaRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.NominaTotalesPorEstadoProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
public class NominaServiceImpl implements NominaService {

    private static final String TABLA = "rrhh_nominas";
    private static final String RECIBO_HONORARIOS = "RECIBO_HONORARIOS";
    /** Tasa ONP vigente (13%) usada si el legajo no define una. */
    static final BigDecimal TASA_ONP_DEFAULT = new BigDecimal("13.00");
    /** Mes laboral estándar (Perú) para prorratear ingresos a mitad de mes. */
    static final int DIAS_MES = 30;
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final RrhhNominaRepository nominaRepository;
    private final RrhhLegajoDigitalRepository legajoRepository;
    private final RrhhComisionRepository comisionRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final NominaMapper mapper;
    private final AuditService auditService;

    @Override
    public GeneracionNominaResponseDTO generar(GenerarNominaRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);

        YearMonth mes = parsePeriodo(request.periodo());
        if (mes.isAfter(YearMonth.now())) {
            throw new IllegalArgumentException("No se puede generar la nómina de un periodo futuro: " + mes);
        }
        String periodo = mes.toString();
        Sede sede = sedeRepository.findById(request.sedeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + request.sedeId()));

        long sinAprobar = comisionRepository.countByPeriodoAndEstadoAndSede_Id(periodo, EstadoComision.CALCULADA, sede.getId());
        if (sinAprobar > 0) {
            throw new IllegalStateException("Hay " + sinAprobar + " comisión(es) CALCULADA(S) sin aprobar en "
                    + periodo + " para la sede " + sede.getNombre() + ". Apruébelas antes de generar la nómina.");
        }

        List<String> advertencias = new ArrayList<>();
        List<NominaListadoDTO> generadas = new ArrayList<>();
        BigDecimal totalNeto = BigDecimal.ZERO;
        int omitidas = 0;

        for (RrhhLegajoDigital legajo : legajoRepository.findActivos(sede.getId())) {
            User empleado = legajo.getUser();
            String nombre = nombre(empleado);

            if (legajo.getFechaIngreso() != null && legajo.getFechaIngreso().isAfter(mes.atEndOfMonth())) {
                advertencias.add(nombre + ": ingresó después del periodo, no se genera nómina");
                continue;
            }
            if (nominaRepository.existsByUser_IdAndPeriodo(empleado.getId(), periodo)) {
                omitidas++;
                continue;
            }

            RrhhComision comision = comisionRepository.findByUser_IdAndPeriodo(empleado.getId(), periodo)
                    .filter(c -> c.getEstado() == EstadoComision.APROBADA)
                    .orElse(null);

            List<String> notas = new ArrayList<>();
            RrhhNomina nomina = calcular(legajo, mes, comision, notas);
            nomina.setSede(sede);
            nomina.setGeneradoPor(admin);
            if (!notas.isEmpty()) {
                nomina.setObservaciones(String.join("\n", notas));
                notas.forEach(n -> advertencias.add(nombre + ": " + n));
            }

            RrhhNomina guardada = nominaRepository.save(nomina);
            auditService.registrar(TABLA, guardada.getId(), AccionAudit.INSERT, datosAuditoria(guardada, "GENERACION"));
            generadas.add(mapper.toListadoDto(guardada));
            totalNeto = totalNeto.add(guardada.getTotalNeto());
        }

        log.info("Nómina {} sede {}: {} generadas, {} omitidas, total neto S/ {}",
                periodo, sede.getNombre(), generadas.size(), omitidas, totalNeto);

        return new GeneracionNominaResponseDTO(periodo, sede.getId(), generadas.size(), omitidas,
                totalNeto, advertencias, generadas);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NominaListadoDTO> buscar(String periodo, UUID sedeId, EstadoNomina estado, UUID userId, Pageable pageable) {
        exigirAdmin(usuarioActual());
        String p = periodo == null || periodo.isBlank() ? null : parsePeriodo(periodo).toString();
        return nominaRepository.search(p, sedeId, estado, userId, pageable).map(mapper::toListadoDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NominaResponseDTO obtener(UUID id) {
        exigirAdmin(usuarioActual());
        return mapper.toResponseDto(buscarDetalle(id));
    }

    @Override
    public NominaResponseDTO aprobar(UUID id) {
        User admin = usuarioActual();
        exigirAdmin(admin);

        RrhhNomina nomina = bloquear(id);
        if (nomina.getEstado() != EstadoNomina.PENDIENTE) {
            throw new IllegalStateException("Solo se puede aprobar una nómina PENDIENTE (estado actual: "
                    + nomina.getEstado() + ")");
        }
        nomina.setEstado(EstadoNomina.APROBADA);
        nomina.setAprobadoPor(admin);
        nomina.setFechaAprobacion(LocalDateTime.now());
        RrhhNomina guardada = nominaRepository.save(nomina);

        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datosAuditoria(guardada, "APROBACION"));
        log.info("Nómina aprobada: {} - periodo {} - neto S/ {}", guardada.getId(), guardada.getPeriodo(), guardada.getTotalNeto());
        return mapper.toResponseDto(guardada);
    }

    @Override
    public NominaResponseDTO pagar(UUID id, PagarNominaRequestDTO request) {
        User admin = usuarioActual();
        exigirAdmin(admin);

        RrhhNomina nomina = bloquear(id);
        if (nomina.getEstado() != EstadoNomina.APROBADA) {
            throw new IllegalStateException("Solo se puede pagar una nómina APROBADA (estado actual: "
                    + nomina.getEstado() + ")");
        }
        nomina.setEstado(EstadoNomina.PAGADA);
        nomina.setPagadoPor(admin);
        nomina.setFechaPago(LocalDateTime.now());
        nomina.setMetodoPago(request.metodoPago());
        nomina.setReferenciaPago(request.referencia());

        RrhhComision comision = nomina.getComision();
        if (comision != null) {
            comision.setEstado(EstadoComision.LIQUIDADA);
            comisionRepository.save(comision);
        }
        RrhhNomina guardada = nominaRepository.save(nomina);

        Map<String, Object> datos = datosAuditoria(guardada, "PAGO");
        datos.put("metodoPago", request.metodoPago().name());
        if (request.referencia() != null) datos.put("referencia", request.referencia());
        auditService.registrar(TABLA, guardada.getId(), AccionAudit.UPDATE, datos);

        log.info("Nómina pagada: {} - S/ {} - {}", guardada.getId(), guardada.getTotalNeto(), request.metodoPago());
        return mapper.toResponseDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public BoletaPagoDTO boleta(UUID id) {
        User actual = usuarioActual();
        RrhhNomina n = buscarDetalle(id);
        if (!esAdmin(actual) && !n.getUser().getId().equals(actual.getId())) {
            throw new AccessDeniedException("Solo puede ver su propia boleta de pago");
        }

        LocalDate fechaIngreso = legajoRepository.findByUser_Id(n.getUser().getId())
                .map(RrhhLegajoDigital::getFechaIngreso).orElse(null);

        List<BoletaConceptoDTO> ingresos = new ArrayList<>();
        Integer dias = n.getDiasComputados();
        ingresos.add(new BoletaConceptoDTO(dias != null && dias < DIAS_MES
                ? "Remuneración básica (" + dias + " días)" : "Remuneración básica", cero(n.getSueldoBaseCalculado())));
        if (cero(n.getTotalComisiones()).signum() > 0) {
            ingresos.add(new BoletaConceptoDTO("Comisión " + porcentaje(n.getPorcentajeComision())
                    + " s/ ventas S/ " + cero(n.getVentasBaseComision()).toPlainString(), n.getTotalComisiones()));
        }

        List<BoletaConceptoDTO> descuentos = new ArrayList<>();
        if (cero(n.getDescuentoPension()).signum() > 0) {
            descuentos.add(new BoletaConceptoDTO(n.getSistemaPension() + " " + porcentaje(n.getTasaPension()),
                    n.getDescuentoPension()));
        }
        if (cero(n.getOtrosDescuentos()).signum() > 0) {
            descuentos.add(new BoletaConceptoDTO("Otros descuentos", n.getOtrosDescuentos()));
        }

        BigDecimal totalIngresos = cero(n.getSueldoBaseCalculado()).add(cero(n.getTotalComisiones()));
        return new BoletaPagoDTO(n.getId(), n.getPeriodo(), n.getUser().getId(), nombre(n.getUser()),
                n.getUser().getEmail(), n.getSede() != null ? n.getSede().getNombre() : null,
                n.getTipoContrato(), fechaIngreso, dias, ingresos, totalIngresos, descuentos,
                cero(n.getDescuentos()), n.getTotalNeto(), n.getEstado(), n.getFechaPago());
    }

    @Override
    public void eliminar(UUID id) {
        exigirAdmin(usuarioActual());
        RrhhNomina nomina = bloquear(id);
        if (nomina.getEstado() != EstadoNomina.PENDIENTE) {
            throw new IllegalStateException("Solo se puede eliminar una nómina PENDIENTE (estado actual: "
                    + nomina.getEstado() + ")");
        }
        Map<String, Object> datos = datosAuditoria(nomina, "ELIMINACION");
        nominaRepository.delete(nomina);
        auditService.registrar(TABLA, id, AccionAudit.DELETE, datos);
        log.info("Nómina eliminada: {} - periodo {}", id, nomina.getPeriodo());
    }

    @Override
    @Transactional(readOnly = true)
    public NominaDashboardDTO dashboard(String periodo, UUID sedeId) {
        exigirAdmin(usuarioActual());
        String p = periodo == null || periodo.isBlank() ? YearMonth.now().toString() : parsePeriodo(periodo).toString();

        long pendientes = 0, aprobadas = 0, pagadas = 0;
        BigDecimal sueldos = BigDecimal.ZERO, comisiones = BigDecimal.ZERO, descuentos = BigDecimal.ZERO;
        BigDecimal neto = BigDecimal.ZERO, pagado = BigDecimal.ZERO, porPagar = BigDecimal.ZERO;

        for (NominaTotalesPorEstadoProjection t : nominaRepository.totalesPorEstado(p, sedeId)) {
            long cant = t.getCantidad() != null ? t.getCantidad() : 0;
            BigDecimal netoEstado = cero(t.getNeto());
            sueldos = sueldos.add(cero(t.getSueldos()));
            comisiones = comisiones.add(cero(t.getComisiones()));
            descuentos = descuentos.add(cero(t.getDescuentos()));
            neto = neto.add(netoEstado);
            switch (t.getEstado()) {
                case PENDIENTE -> { pendientes = cant; porPagar = porPagar.add(netoEstado); }
                case APROBADA -> { aprobadas = cant; porPagar = porPagar.add(netoEstado); }
                case PAGADA -> { pagadas = cant; pagado = pagado.add(netoEstado); }
            }
        }
        return new NominaDashboardDTO(p, sedeId, pendientes, aprobadas, pagadas,
                sueldos, comisiones, descuentos, neto, pagado, porPagar);
    }

    // ============ CÁLCULO ============

    /**
     * Arma la nómina (sin guardar) a partir del legajo y la comisión aprobada.
     * - Sueldo: prorrateado por días si ingresó dentro del periodo (mes de 30 días).
     * - Pensión: sobre (sueldo + comisión), solo si no es RECIBO_HONORARIOS.
     */
    RrhhNomina calcular(RrhhLegajoDigital legajo, YearMonth mes, RrhhComision comision, List<String> notas) {
        int dias = diasComputados(legajo.getFechaIngreso(), mes);
        BigDecimal sueldoBase = cero(legajo.getSueldoBase());
        BigDecimal sueldo = dias >= DIAS_MES ? dinero(sueldoBase)
                : dinero(sueldoBase.multiply(BigDecimal.valueOf(dias)).divide(BigDecimal.valueOf(DIAS_MES), 4, RoundingMode.HALF_UP));

        BigDecimal montoComision = comision != null ? dinero(comision.getMontoComision()) : BigDecimal.ZERO.setScale(2);

        SistemaPension sistema = legajo.getSistemaPension();
        BigDecimal tasa = BigDecimal.ZERO;
        if (RECIBO_HONORARIOS.equalsIgnoreCase(legajo.getTipoContrato())) {
            sistema = SistemaPension.NINGUNO;
        } else if (sistema == null) {
            notas.add("legajo sin sistema de pensión (ONP/AFP); no se aplicó descuento");
        } else if (sistema == SistemaPension.ONP) {
            tasa = legajo.getTasaPension() != null ? legajo.getTasaPension() : TASA_ONP_DEFAULT;
        } else if (sistema == SistemaPension.AFP) {
            if (legajo.getTasaPension() == null) {
                notas.add("AFP sin tasa registrada en el legajo; no se aplicó descuento");
            } else {
                tasa = legajo.getTasaPension();
            }
        }
        BigDecimal bruto = sueldo.add(montoComision);
        BigDecimal descuentoPension = dinero(bruto.multiply(tasa).divide(CIEN, 4, RoundingMode.HALF_UP));
        BigDecimal otros = BigDecimal.ZERO.setScale(2);
        BigDecimal descuentos = descuentoPension.add(otros);

        return RrhhNomina.builder()
                .user(legajo.getUser())
                .periodo(mes.toString())
                .tipoContrato(legajo.getTipoContrato())
                .diasComputados(dias)
                .sueldoBaseCalculado(sueldo)
                .comision(comision)
                .ventasBaseComision(comision != null ? comision.getTotalVentasNetas() : null)
                .porcentajeComision(comision != null ? comision.getPorcentajeComision() : null)
                .totalComisiones(montoComision)
                .sistemaPension(sistema)
                .tasaPension(tasa.signum() > 0 ? tasa : null)
                .descuentoPension(descuentoPension)
                .otrosDescuentos(otros)
                .descuentos(descuentos)
                .totalNeto(bruto.subtract(descuentos))
                .estado(EstadoNomina.PENDIENTE)
                .build();
    }

    /** 30 si trabajó todo el mes; si ingresó en el periodo, 30 - (día de ingreso - 1), mínimo 1. */
    static int diasComputados(LocalDate fechaIngreso, YearMonth mes) {
        if (fechaIngreso == null || fechaIngreso.isBefore(mes.atDay(1))) return DIAS_MES;
        if (fechaIngreso.isAfter(mes.atEndOfMonth())) return 0;
        return Math.max(1, Math.min(DIAS_MES, DIAS_MES - fechaIngreso.getDayOfMonth() + 1));
    }

    // ============ HELPERS ============

    private RrhhNomina buscarDetalle(UUID id) {
        return nominaRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nómina no encontrada: " + id));
    }

    private RrhhNomina bloquear(UUID id) {
        return nominaRepository.findByIdParaActualizar(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nómina no encontrada: " + id));
    }

    private Map<String, Object> datosAuditoria(RrhhNomina n, String operacion) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("operacion", operacion);
        datos.put("periodo", n.getPeriodo());
        if (n.getUser() != null) datos.put("userId", n.getUser().getId().toString());
        datos.put("estado", n.getEstado() != null ? n.getEstado().name() : null);
        datos.put("totalNeto", n.getTotalNeto());
        return datos;
    }

    private YearMonth parsePeriodo(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Periodo inválido: '" + periodo + "'. Formato esperado: YYYY-MM");
        }
    }

    private static BigDecimal dinero(BigDecimal valor) {
        return cero(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal cero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String porcentaje(BigDecimal valor) {
        return valor == null ? "" : valor.stripTrailingZeros().toPlainString() + "%";
    }

    private String nombre(User user) {
        return user.getNombre() + " " + user.getApellido();
    }

    private boolean esAdmin(User user) {
        return user.getRol() != null && user.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    /** Defensa en profundidad: además del @PreAuthorize del controller. */
    private void exigirAdmin(User user) {
        if (!esAdmin(user)) {
            throw new AccessDeniedException("Solo un administrador puede gestionar nóminas");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
