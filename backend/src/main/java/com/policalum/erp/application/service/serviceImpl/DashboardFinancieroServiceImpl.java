package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.DisponibleCalculadoDTO;
import com.policalum.erp.application.dto.response.EvolucionMensualResponseDTO;
import com.policalum.erp.application.dto.response.FlujoCajaResumenDTO;
import com.policalum.erp.application.dto.response.KpisFinancierosDTO;
import com.policalum.erp.application.dto.response.KpisFinancierosResponseDTO;
import com.policalum.erp.application.dto.response.KpisSedeDTO;
import com.policalum.erp.application.dto.response.MesEvolucionDTO;
import com.policalum.erp.application.dto.response.PatrimonioResponseDTO;
import com.policalum.erp.application.dto.response.PatrimonioSedeDTO;
import com.policalum.erp.application.dto.response.RetiroSocioResumenDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaEstimadoDTO;
import com.policalum.erp.application.dto.response.SeparacionPatrimonialDTO;
import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import com.policalum.erp.application.service.DashboardFinancieroService;
import com.policalum.erp.application.service.UtilidadNetaService;
import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.FinanzasCuentaPorPagarRepository;
import com.policalum.erp.infrastructure.repository.FinanzasRetiroSocioRepository;
import com.policalum.erp.infrastructure.repository.PreformaVentaRepository;
import com.policalum.erp.infrastructure.repository.ReembolsoRepository;
import com.policalum.erp.infrastructure.repository.RrhhNominaRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.StockRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import com.policalum.erp.infrastructure.repository.projection.PreformasPorEstadoProjection;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorAdminProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DashboardFinancieroServiceImpl implements DashboardFinancieroService {

    static final int MESES_EVOLUCION_DEFAULT = 12;
    static final int MESES_EVOLUCION_MAX = 24;
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final String CORPORATIVO = "Corporativo (cuentas sin sede)";

    private final MovimientosCajaCalculator calculator;
    private final UtilidadNetaService utilidadNetaService;
    private final StockRepository stockRepository;
    private final FinanzasCuentaPorPagarRepository cxpRepository;
    private final RrhhNominaRepository nominaRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final FinanzasRetiroSocioRepository retiroRepository;
    private final PreformaVentaRepository preformaRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;

    // ===================== PATRIMONIO =====================

    @Override
    public PatrimonioResponseDTO patrimonio(UUID sedeId) {
        exigirAdmin();
        List<Sede> sedes = sedesDelReporte(sedeId);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();

        DisponibleCalculadoDTO disponible = calculator.disponible(sedeId, ahora);
        Map<UUID, BigDecimal> inventario = porSede(stockRepository.valorizadoPorSede(sedeId));
        Map<UUID, BigDecimal> cxp = porSede(cxpRepository.saldoPendientePorSede(sedeId));
        Map<UUID, BigDecimal> nominas = porSede(nominaRepository.porPagarPorSede(List.of(EstadoNomina.PENDIENTE, EstadoNomina.APROBADA), sedeId));
        Map<UUID, BigDecimal> reembolsos = porSede(reembolsoRepository.pendientesPorSede(sedeId));
        BigDecimal cxpVencidas = total(porSede(cxpRepository.saldoVencidoPorSede(sedeId, hoy)));
        long sinCosto = stockRepository.contarSinCosto(sedeId);

        BigDecimal totalInventario = total(inventario);
        BigDecimal totalActivos = disponible.total().add(totalInventario);
        BigDecimal totalCxp = total(cxp), totalNominas = total(nominas), totalReembolsos = total(reembolsos);
        BigDecimal totalPasivos = totalCxp.add(totalNominas).add(totalReembolsos);

        // Desglose: cada sede + corporativo (cuentas sin sede)
        Set<UUID> conDatos = new HashSet<>();
        List.of(inventario, cxp, nominas, reembolsos).forEach(m -> conDatos.addAll(m.keySet()));
        List<PatrimonioSedeDTO> porSede = new ArrayList<>();
        for (Sede s : sedes) {
            BigDecimal disp = disponibleDe(disponible.cuentas(), s.getId());
            if (sedeId == null && !Boolean.TRUE.equals(s.getActiva()) && !conDatos.contains(s.getId()) && disp.signum() == 0) continue;
            BigDecimal inv = inventario.getOrDefault(s.getId(), cero2());
            BigDecimal pas = cxp.getOrDefault(s.getId(), BigDecimal.ZERO).add(nominas.getOrDefault(s.getId(), BigDecimal.ZERO))
                    .add(reembolsos.getOrDefault(s.getId(), BigDecimal.ZERO)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal act = disp.add(inv);
            porSede.add(new PatrimonioSedeDTO(s.getId(), s.getNombre(), disp, inv, act, pas, act.subtract(pas)));
        }
        if (sedeId == null && disponible.cuentas().stream().anyMatch(c -> c.sedeId() == null)) {
            BigDecimal disp = disponibleDe(disponible.cuentas(), null);
            porSede.add(new PatrimonioSedeDTO(null, CORPORATIVO, disp, cero2(), disp, cero2(), disp));
        }

        List<String> advertencias = new ArrayList<>(disponible.advertencias());
        if (sinCosto > 0) {
            advertencias.add(sinCosto + " productos con stock no tienen precio de compra: no suman al inventario");
        }

        return new PatrimonioResponseDTO(sedeId, ahora, disponible.total(), disponible.cuentas(),
                totalInventario, sinCosto, totalActivos.setScale(2, RoundingMode.HALF_UP),
                totalCxp, cxpVencidas, totalNominas, totalReembolsos, totalPasivos.setScale(2, RoundingMode.HALF_UP),
                totalActivos.subtract(totalPasivos).setScale(2, RoundingMode.HALF_UP),
                separacion(sedeId, hoy), porSede, advertencias);
    }

    /** Separación dueño/empresa del año en curso. */
    SeparacionPatrimonialDTO separacion(UUID sedeId, LocalDate hoy) {
        LocalDate inicioAnio = hoy.withDayOfYear(1);
        LocalDateTime finExclusivo = hoy.plusDays(1).atStartOfDay();
        BigDecimal utilidad = utilidadNetaService.calcular(sedeId, inicioAnio, hoy).utilidadNetaReal();
        BigDecimal retirosAnio = cero(retiroRepository.sumarMonto(EstadoRetiro.REGISTRADO, sedeId, inicioAnio.atStartOfDay(), finExclusivo));
        BigDecimal retirosMes = cero(retiroRepository.sumarMonto(EstadoRetiro.REGISTRADO, sedeId, hoy.withDayOfMonth(1).atStartOfDay(), finExclusivo));
        List<RetiroSocioResumenDTO> porSocio = new ArrayList<>();
        for (RetiroPorAdminProjection r : retiroRepository.totalesPorAdmin(EstadoRetiro.REGISTRADO, sedeId, inicioAnio.atStartOfDay(), finExclusivo)) {
            String nombre = ((r.getNombre() != null ? r.getNombre() : "") + " " + (r.getApellido() != null ? r.getApellido() : "")).trim();
            porSocio.add(new RetiroSocioResumenDTO(r.getAdminId(), nombre, r.getCantidad() != null ? r.getCantidad() : 0L, cero(r.getTotal())));
        }
        return new SeparacionPatrimonialDTO(hoy.getYear(), utilidad, escala(retirosAnio), escala(utilidad.subtract(retirosAnio)),
                escala(retirosMes), porSocio);
    }

    // ===================== KPIs =====================

    @Override
    public KpisFinancierosResponseDTO kpis(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin();
        List<Sede> sedes = sedesDelReporte(sedeId);
        RangoFechas rango = RangoFechas.de(desde, hasta, LocalDate.now());
        MovimientosCaja mov = calculator.movimientos(rango.inicio(), rango.finExclusivo(), sedeId);

        KpisFinancierosDTO consolidado = calcularKpis(sedeId, rango, mov.resumen());
        List<KpisSedeDTO> porSede = new ArrayList<>();
        if (sedeId != null) {
            porSede.add(new KpisSedeDTO(sedeId, sedes.get(0).getNombre(), consolidado));
        } else {
            Set<UUID> conMovimiento = mov.sedes();
            for (Sede s : sedes) {
                if (!Boolean.TRUE.equals(s.getActiva()) && !conMovimiento.contains(s.getId())) continue;
                porSede.add(new KpisSedeDTO(s.getId(), s.getNombre(), calcularKpis(s.getId(), rango, mov.resumenSede(s.getId()))));
            }
        }
        return new KpisFinancierosResponseDTO(sedeId, rango.desde(), rango.hasta(), consolidado, porSede);
    }

    KpisFinancierosDTO calcularKpis(UUID sedeId, RangoFechas rango, FlujoCajaResumenDTO flujo) {
        UtilidadNetaResponseDTO u = utilidadNetaService.calcular(sedeId, rango.desde(), rango.hasta());
        RangoFechas previo = rango.anterior();
        UtilidadNetaResponseDTO up = utilidadNetaService.calcular(sedeId, previo.desde(), previo.hasta());

        BigDecimal ticket = u.cantidadVentas() > 0
                ? u.ventasTotales().divide(BigDecimal.valueOf(u.cantidadVentas()), 2, RoundingMode.HALF_UP) : cero2();
        BigDecimal tasaDevolucion = u.ventasNetas().signum() > 0
                ? porcentaje(u.devolucionesNetas(), u.ventasNetas()) : cero2();
        BigDecimal retirosSobreUtilidad = u.utilidadNetaReal().signum() > 0
                ? porcentaje(u.retirosSocios(), u.utilidadNetaReal()) : null;
        BigDecimal cxpVencidas = total(porSede(cxpRepository.saldoVencidoPorSede(sedeId, LocalDate.now())));

        long confirmadas = 0, cerradas = 0;
        BigDecimal perdido = BigDecimal.ZERO;
        for (PreformasPorEstadoProjection p : preformaRepository.totalesPorEstado(sedeId, rango.inicio(), rango.finExclusivo())) {
            long cantidad = p.getCantidad() != null ? p.getCantidad() : 0;
            switch (p.getEstado()) {
                case CONFIRMADA -> { confirmadas += cantidad; cerradas += cantidad; }
                case CANCELADA, EXPIRADA -> { cerradas += cantidad; perdido = perdido.add(cero(p.getMonto())); }
                default -> { }
            }
        }
        BigDecimal conversion = cerradas > 0 ? porcentaje(BigDecimal.valueOf(confirmadas), BigDecimal.valueOf(cerradas)) : cero2();

        return new KpisFinancierosDTO(u.ventasNetasReales(), u.cantidadVentas(), ticket,
                variacion(u.ventasNetasReales(), up.ventasNetasReales()),
                u.utilidadNetaReal(), u.margenNetoPorcentaje(), variacion(u.utilidadNetaReal(), up.utilidadNetaReal()),
                flujo.flujoOperativo().total(), flujo.flujoNeto().total(),
                tasaDevolucion, u.valorMermas(), u.retirosSocios(), retirosSobreUtilidad, cxpVencidas,
                conversion, escala(perdido));
    }

    // ===================== EVOLUCIÓN =====================

    @Override
    public EvolucionMensualResponseDTO evolucionMensual(UUID sedeId, Integer meses) {
        exigirAdmin();
        int n = meses != null ? meses : MESES_EVOLUCION_DEFAULT;
        if (n < 1 || n > MESES_EVOLUCION_MAX) {
            throw new IllegalArgumentException("La evolución debe ser de 1 a " + MESES_EVOLUCION_MAX + " meses");
        }
        if (sedeId != null) sedesDelReporte(sedeId);
        LocalDate hoy = LocalDate.now();
        YearMonth actual = YearMonth.from(hoy);

        List<MesEvolucionDTO> serie = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            YearMonth ym = actual.minusMonths(i);
            LocalDate fin = i == 0 ? hoy : ym.atEndOfMonth();
            UtilidadNetaResponseDTO u = utilidadNetaService.calcular(sedeId, ym.atDay(1), fin);
            FlujoCajaResumenDTO f = calculator.movimientos(ym.atDay(1).atStartOfDay(), fin.plusDays(1).atStartOfDay(), sedeId).resumen();
            serie.add(new MesEvolucionDTO(ym.toString(), i == 0, u.ventasNetasReales(), u.devolucionesNetas(),
                    u.comprasNetas(), u.gastosOperativos(), u.utilidadNetaReal(), u.margenNetoPorcentaje(),
                    f.cobrosVentas().total(), f.totalEgresosOperativos().total(), f.flujoOperativo().total(),
                    f.retirosSocios().total(), f.flujoNeto().total()));
        }
        return new EvolucionMensualResponseDTO(sedeId, n, serie);
    }

    // ===================== HELPERS =====================

    /** (actual - anterior) / |anterior| × 100; null si no hay base. */
    static BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) return null;
        return actual.subtract(anterior).multiply(CIEN).divide(anterior.abs(), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal porcentaje(BigDecimal parte, BigDecimal base) {
        return parte.multiply(CIEN).divide(base, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal disponibleDe(List<SaldoCuentaEstimadoDTO> cuentas, UUID sedeId) {
        return cuentas.stream().filter(c -> Objects.equals(sedeId, c.sedeId()) && c.saldoEstimado() != null)
                .map(SaldoCuentaEstimadoDTO::saldoEstimado).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static Map<UUID, BigDecimal> porSede(List<MontoPorSedeProjection> filas) {
        Map<UUID, BigDecimal> mapa = new HashMap<>();
        for (MontoPorSedeProjection f : filas) mapa.merge(f.getSedeId(), escala(cero(f.getTotal())), BigDecimal::add);
        return mapa;
    }

    private static BigDecimal total(Map<UUID, BigDecimal> mapa) {
        return escala(mapa.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static BigDecimal cero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal cero2() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static BigDecimal escala(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Sede> sedesDelReporte(UUID sedeId) {
        if (sedeId != null) {
            return List.of(sedeRepository.findById(sedeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + sedeId)));
        }
        return sedeRepository.findAll().stream()
                .sorted(Comparator.comparing(Sede::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
    }

    private User exigirAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
        if (u.getRol() == null || u.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede consultar los dashboards financieros");
        }
        return u;
    }
}
