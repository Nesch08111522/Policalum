package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.DisponibleCalculadoDTO;
import com.policalum.erp.application.dto.response.FlujoCajaRealResponseDTO;
import com.policalum.erp.application.dto.response.FlujoCajaSedeDTO;
import com.policalum.erp.application.dto.response.FlujoDevengadoResponseDTO;
import com.policalum.erp.application.dto.response.MesProyectadoDTO;
import com.policalum.erp.application.dto.response.MontoProyectadoDTO;
import com.policalum.erp.application.dto.response.ParametrosFinanzasDTO;
import com.policalum.erp.application.dto.response.ProyeccionFlujoResponseDTO;
import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import com.policalum.erp.application.dto.response.UtilidadSedeDTO;
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.FlujoCajaService;
import com.policalum.erp.application.service.UtilidadNetaService;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Concepto;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoReferenciaCxP;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CotizacionRepository;
import com.policalum.erp.infrastructure.repository.FinanzasCuentaPorPagarRepository;
import com.policalum.erp.infrastructure.repository.PreformaVentaRepository;
import com.policalum.erp.infrastructure.repository.ReembolsoRepository;
import com.policalum.erp.infrastructure.repository.RrhhNominaRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.projection.CxpPendienteProjection;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
public class FlujoCajaServiceImpl implements FlujoCajaService {

    private final MovimientosCajaCalculator calculator;
    private final ConfiguracionService configuracionService;
    private final UtilidadNetaService utilidadNetaService;
    private final FinanzasCuentaPorPagarRepository cxpRepository;
    private final RrhhNominaRepository nominaRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final CotizacionRepository cotizacionRepository;
    private final PreformaVentaRepository preformaRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;

    @Override
    public FlujoCajaRealResponseDTO flujoReal(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin();
        RangoFechas rango = RangoFechas.de(desde, hasta, LocalDate.now());
        List<Sede> sedes = sedesDelReporte(sedeId);
        MovimientosCaja mov = calculator.movimientos(rango.inicio(), rango.finExclusivo(), sedeId);

        Set<UUID> conMovimiento = mov.sedes();
        List<FlujoCajaSedeDTO> porSede = sedes.stream()
                .filter(s -> sedeId != null || Boolean.TRUE.equals(s.getActiva()) || conMovimiento.contains(s.getId()))
                .map(s -> new FlujoCajaSedeDTO(s.getId(), s.getNombre(), mov.resumenSede(s.getId())))
                .toList();

        List<String> advertencias = new ArrayList<>();
        BigDecimal sinMetodo = mov.egresosSinMetodo();
        if (sinMetodo.signum() > 0) {
            advertencias.add("S/ " + sinMetodo.toPlainString() + " de pagos no tienen método registrado: se cuentan como digitales");
        }
        return new FlujoCajaRealResponseDTO(sedeId, rango.desde(), rango.hasta(), mov.resumen(), porSede, advertencias);
    }

    @Override
    public FlujoDevengadoResponseDTO devengado(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin();
        RangoFechas rango = RangoFechas.de(desde, hasta, LocalDate.now());
        List<Sede> sedes = sedesDelReporte(sedeId);
        UtilidadNetaResponseDTO consolidado = utilidadNetaService.calcular(sedeId, rango.desde(), rango.hasta());

        List<UtilidadSedeDTO> porSede = new ArrayList<>();
        if (sedeId != null) {
            porSede.add(new UtilidadSedeDTO(sedeId, sedes.get(0).getNombre(), consolidado));
        } else {
            for (Sede s : sedes) {
                UtilidadNetaResponseDTO u = utilidadNetaService.calcular(s.getId(), rango.desde(), rango.hasta());
                if (Boolean.TRUE.equals(s.getActiva()) || tieneMovimiento(u)) {
                    porSede.add(new UtilidadSedeDTO(s.getId(), s.getNombre(), u));
                }
            }
        }
        return new FlujoDevengadoResponseDTO(sedeId, rango.desde(), rango.hasta(), consolidado, porSede);
    }

    @Override
    public ProyeccionFlujoResponseDTO proyeccion(UUID sedeId, Integer meses) {
        exigirAdmin();
        ParametrosFinanzasDTO param = configuracionService.finanzas();
        int mesesHistoricos = param.mesesHistoricosProyeccion();
        int n = meses != null ? meses : param.mesesProyeccionDefault();
        if (n < 1 || n > param.mesesProyeccionMax()) {
            throw new IllegalArgumentException("La proyección debe ser de 1 a " + param.mesesProyeccionMax() + " meses");
        }
        if (sedeId != null) sedesDelReporte(sedeId);
        LocalDate hoy = LocalDate.now();
        LocalDateTime ahora = LocalDateTime.now();
        YearMonth mesActual = YearMonth.from(hoy);
        List<String> advertencias = new ArrayList<>();

        // 1) Tendencia: promedio de los últimos meses completos con actividad
        Map<Concepto, BigDecimal> acumulado = new EnumMap<>(Concepto.class);
        int mesesConDatos = 0;
        for (int i = 1; i <= mesesHistoricos; i++) {
            YearMonth ym = mesActual.minusMonths(i);
            MovimientosCaja mov = calculator.movimientos(ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay(), sedeId);
            if (mov.estaVacio()) continue;
            mesesConDatos++;
            for (Concepto c : Concepto.values()) acumulado.merge(c, mov.total(c), BigDecimal::add);
        }
        if (mesesConDatos < mesesHistoricos) {
            advertencias.add("Solo hay " + mesesConDatos + " de " + mesesHistoricos
                    + " meses con movimientos: la tendencia es poco confiable");
        }
        BigDecimal divisor = BigDecimal.valueOf(Math.max(1, mesesConDatos));
        Map<Concepto, BigDecimal> promedio = new EnumMap<>(Concepto.class);
        for (Concepto c : Concepto.values()) {
            promedio.put(c, acumulado.getOrDefault(c, BigDecimal.ZERO).divide(divisor, 2, RoundingMode.HALF_UP));
        }

        // 2) Comprometido: documentos que ya existen
        BigDecimal[] proveedores = ceros(n), otros = ceros(n), nominas = ceros(n), reembolsos = ceros(n);
        BigDecimal fueraDeHorizonte = BigDecimal.ZERO;
        for (CxpPendienteProjection p : cxpRepository.pendientesPorVencimiento(sedeId)) {
            BigDecimal saldo = cero(p.getSaldo());
            if (saldo.signum() <= 0) continue;
            int idx = indiceMes(mesActual, p.getFechaVencimiento());
            if (idx >= n) {
                fueraDeHorizonte = fueraDeHorizonte.add(saldo);
                continue;
            }
            BigDecimal[] destino = p.getTipo() == TipoReferenciaCxP.COMPRA ? proveedores : otros;
            destino[idx] = destino[idx].add(saldo);
        }
        nominas[0] = suma(nominaRepository.porPagarPorSede(List.of(EstadoNomina.PENDIENTE, EstadoNomina.APROBADA), sedeId));
        reembolsos[0] = suma(reembolsoRepository.pendientesPorSede(sedeId));
        if (fueraDeHorizonte.signum() > 0) {
            advertencias.add("S/ " + fueraDeHorizonte.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + " en cuentas por pagar vencen después del horizonte proyectado");
        }

        // 3) Saldo inicial: caja y bancos estimados
        DisponibleCalculadoDTO disponible = calculator.disponible(sedeId, ahora);
        advertencias.addAll(disponible.advertencias());
        BigDecimal saldo = disponible.total();

        // 4) Meses
        List<MesProyectadoDTO> resultado = new ArrayList<>();
        boolean avisoNegativo = false;
        for (int i = 0; i < n; i++) {
            YearMonth ym = mesActual.plusMonths(i);
            BigDecimal fraccion = i == 0
                    ? BigDecimal.valueOf(ym.lengthOfMonth() - hoy.getDayOfMonth() + 1).divide(BigDecimal.valueOf(ym.lengthOfMonth()), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ONE.setScale(4);
            BigDecimal ingresos = prorrateo(promedio.get(Concepto.COBRO_VENTA), fraccion);
            MontoProyectadoDTO prov = proyectado(prorrateo(promedio.get(Concepto.PAGO_PROVEEDOR), fraccion), proveedores[i]);
            MontoProyectadoDTO nom = proyectado(prorrateo(promedio.get(Concepto.PAGO_NOMINA), fraccion), nominas[i]);
            MontoProyectadoDTO reem = proyectado(prorrateo(promedio.get(Concepto.REEMBOLSO), fraccion), reembolsos[i]);
            MontoProyectadoDTO otr = proyectado(prorrateo(promedio.get(Concepto.OTRO_PAGO), fraccion), otros[i]);
            BigDecimal egresos = prov.estimado().add(nom.estimado()).add(reem.estimado()).add(otr.estimado());
            BigDecimal operativo = ingresos.subtract(egresos);
            BigDecimal retiros = prorrateo(promedio.get(Concepto.RETIRO_SOCIO), fraccion);
            BigDecimal neto = operativo.subtract(retiros);
            saldo = saldo.add(neto);
            if (saldo.signum() < 0 && !avisoNegativo) {
                advertencias.add("En " + ym + " el saldo estimado sería negativo (S/ " + saldo.toPlainString() + ")");
                avisoNegativo = true;
            }
            resultado.add(new MesProyectadoDTO(ym.toString(), i == 0, fraccion, ingresos, prov, nom, reem, otr,
                    egresos, operativo, retiros, neto, saldo));
        }

        BigDecimal cotizaciones = cero(cotizacionRepository.sumarVigentes(
                List.of(EstadoCotizacion.EMITIDA, EstadoCotizacion.ACEPTADA), hoy, sedeId));
        BigDecimal preformas = cero(preformaRepository.sumarAbiertasVigentes(sedeId, ahora));
        BigDecimal promedioEgresos = promedio.get(Concepto.PAGO_PROVEEDOR).add(promedio.get(Concepto.PAGO_NOMINA))
                .add(promedio.get(Concepto.REEMBOLSO)).add(promedio.get(Concepto.OTRO_PAGO));

        return new ProyeccionFlujoResponseDTO(sedeId, hoy, mesesConDatos, disponible.total(),
                promedio.get(Concepto.COBRO_VENTA), promedioEgresos, promedio.get(Concepto.RETIRO_SOCIO),
                resultado, cotizaciones.add(preformas).setScale(2, RoundingMode.HALF_UP), advertencias);
    }

    // ============ HELPERS ============

    /** 0 = mes en curso (incluye vencidos y sin fecha de vencimiento). */
    static int indiceMes(YearMonth mesActual, LocalDate vencimiento) {
        if (vencimiento == null) return 0;
        long diff = ChronoUnit.MONTHS.between(mesActual, YearMonth.from(vencimiento));
        return (int) Math.max(0, diff);
    }

    static MontoProyectadoDTO proyectado(BigDecimal tendencia, BigDecimal comprometido) {
        BigDecimal c = comprometido.setScale(2, RoundingMode.HALF_UP);
        return new MontoProyectadoDTO(tendencia, c, tendencia.max(c));
    }

    private static BigDecimal prorrateo(BigDecimal monto, BigDecimal fraccion) {
        return monto.multiply(fraccion).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal[] ceros(int n) {
        BigDecimal[] a = new BigDecimal[n];
        java.util.Arrays.fill(a, BigDecimal.ZERO);
        return a;
    }

    private static BigDecimal suma(List<MontoPorSedeProjection> filas) {
        return filas.stream().map(f -> cero(f.getTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal cero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static boolean tieneMovimiento(UtilidadNetaResponseDTO u) {
        return u.ventasNetas().signum() != 0 || u.comprasNetas().signum() != 0 || u.gastosOperativos().signum() != 0;
    }

    /** Con sedeId: esa sede (404 si no existe). Sin sedeId: todas, ordenadas por nombre. */
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
            throw new AccessDeniedException("Solo un administrador puede consultar el flujo de caja");
        }
        return u;
    }
}
