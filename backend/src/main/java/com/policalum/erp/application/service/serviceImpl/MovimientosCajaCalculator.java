package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.DisponibleCalculadoDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaEstimadoDTO;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Concepto;
import com.policalum.erp.application.service.serviceImpl.MovimientosCaja.Movimiento;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.OrigenFondos;
import com.policalum.erp.domain.enums.TipoReferenciaCxP;
import com.policalum.erp.domain.model.CuentaFinanciera;
import com.policalum.erp.domain.model.SaldoCuentaFinanciera;
import com.policalum.erp.infrastructure.repository.CuentaFinancieraRepository;
import com.policalum.erp.infrastructure.repository.FinanzasCuentaPorPagarRepository;
import com.policalum.erp.infrastructure.repository.FinanzasRetiroSocioRepository;
import com.policalum.erp.infrastructure.repository.PagoCuentaPorPagarRepository;
import com.policalum.erp.infrastructure.repository.PagoVentaRepository;
import com.policalum.erp.infrastructure.repository.ReembolsoRepository;
import com.policalum.erp.infrastructure.repository.RrhhNominaRepository;
import com.policalum.erp.infrastructure.repository.SaldoCuentaFinancieraRepository;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeMetodoProjection;
import com.policalum.erp.infrastructure.repository.projection.PagoCxpHistoricoProjection;
import com.policalum.erp.infrastructure.repository.projection.PagoCxpPorSedeProjection;
import com.policalum.erp.infrastructure.repository.projection.ReembolsoPorSedeMetodoProjection;
import com.policalum.erp.infrastructure.repository.projection.RetiroPorSedeOrigenProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee de la BD los movimientos de dinero real (Módulo 6) y estima el saldo de las cuentas.
 *
 * Fuentes:
 *   cobros de ventas COMPLETADAS (fecha de la venta)
 *   abonos a cuentas por pagar (COMPRA = proveedores; PLANILLA/SERVICIO/OTRO = otros pagos);
 *     las CxP pagadas antes del detalle de abonos se toman en la fecha del pago final
 *   nóminas PAGADAS (fecha de pago)
 *   reembolsos PROCESADOS (fecha de proceso)
 *   retiros de socios REGISTRADOS: CAJA_FISICA se trata como EFECTIVO y CUENTA_BANCARIA como
 *     TRANSFERENCIA (el retiro no guarda la cuenta exacta)
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovimientosCajaCalculator {

    static final int DIAS_CORTE_ANTIGUO = 30;

    private final PagoVentaRepository pagoVentaRepository;
    private final PagoCuentaPorPagarRepository pagoCxpRepository;
    private final FinanzasCuentaPorPagarRepository cxpRepository;
    private final RrhhNominaRepository nominaRepository;
    private final ReembolsoRepository reembolsoRepository;
    private final FinanzasRetiroSocioRepository retiroRepository;
    private final CuentaFinancieraRepository cuentaRepository;
    private final SaldoCuentaFinancieraRepository saldoRepository;

    /** Movimientos en [desde, hasta). sedeId null = todas las sedes. */
    public MovimientosCaja movimientos(LocalDateTime desde, LocalDateTime hasta, UUID sedeId) {
        List<Movimiento> items = new ArrayList<>();
        for (MontoPorSedeMetodoProjection p : pagoVentaRepository.cobrosPorSedeYMetodo(EstadoVenta.COMPLETADA, desde, hasta, sedeId)) {
            agregar(items, p.getSedeId(), Concepto.COBRO_VENTA, p.getMetodo(), p.getTotal());
        }
        for (PagoCxpPorSedeProjection p : pagoCxpRepository.pagosPorSede(desde, hasta, sedeId)) {
            agregar(items, p.getSedeId(), conceptoCxp(p.getTipo()), p.getMetodo(), p.getTotal());
        }
        for (PagoCxpHistoricoProjection p : cxpRepository.pagosSinDetallePorSede(desde, hasta, sedeId)) {
            agregar(items, p.getSedeId(), conceptoCxp(p.getTipo()), metodoDesdeTexto(p.getMetodo()), p.getTotal());
        }
        for (MontoPorSedeMetodoProjection p : nominaRepository.pagadasPorSedeYMetodo(desde, hasta, sedeId)) {
            agregar(items, p.getSedeId(), Concepto.PAGO_NOMINA, p.getMetodo(), p.getTotal());
        }
        for (ReembolsoPorSedeMetodoProjection p : reembolsoRepository.procesadosPorSedeYMetodo(desde, hasta, sedeId)) {
            agregar(items, p.getSedeId(), Concepto.REEMBOLSO,
                    p.getMetodo() != null ? metodoDesdeTexto(p.getMetodo().name()) : null, p.getTotal());
        }
        for (RetiroPorSedeOrigenProjection p : retiroRepository.totalesPorSedeYOrigen(EstadoRetiro.REGISTRADO, desde, hasta, sedeId)) {
            MetodoPago metodo = p.getOrigen() == OrigenFondos.CAJA_FISICA ? MetodoPago.EFECTIVO : MetodoPago.TRANSFERENCIA;
            agregar(items, p.getSedeId(), Concepto.RETIRO_SOCIO, metodo, p.getTotal());
        }
        return new MovimientosCaja(items);
    }

    /**
     * Caja y bancos estimados a 'ahora'. sedeId null = todas las cuentas activas; con sedeId,
     * solo las cuentas de esa sede (las corporativas quedan fuera).
     */
    public DisponibleCalculadoDTO disponible(UUID sedeId, LocalDateTime ahora) {
        List<CuentaFinanciera> activas = cuentaRepository.findByActivaTrueOrderByNombreAsc();
        List<SaldoCuentaEstimadoDTO> cuentas = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        boolean hayCorporativas = false;

        for (CuentaFinanciera c : activas) {
            UUID sedeCuenta = c.getSede() != null ? c.getSede().getId() : null;
            if (sedeId != null && !sedeId.equals(sedeCuenta)) {
                hayCorporativas |= sedeCuenta == null;
                continue;
            }
            Set<MetodoPago> metodos = c.getMetodosPago() != null ? c.getMetodosPago() : Set.of();
            List<MetodoPago> metodosOrdenados = List.copyOf(new TreeSet<>(metodos));
            SaldoCuentaFinanciera corte = saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(c.getId()).orElse(null);
            if (corte == null) {
                advertencias.add("La cuenta '" + c.getNombre() + "' no tiene saldo registrado: no suma al disponible");
                cuentas.add(new SaldoCuentaEstimadoDTO(c.getId(), c.getNombre(), c.getTipo(), sedeCuenta,
                        c.getSede() != null ? c.getSede().getNombre() : null, metodosOrdenados, null, null, null, null, null));
                continue;
            }
            BigDecimal posteriores = metodos.isEmpty() ? BigDecimal.ZERO.setScale(2)
                    : movimientos(corte.getFechaCorte(), ahora, sedeCuenta).neto(m -> m.metodo() != null && metodos.contains(m.metodo()));
            BigDecimal estimado = corte.getSaldo().add(posteriores).setScale(2, RoundingMode.HALF_UP);
            long dias = ChronoUnit.DAYS.between(corte.getFechaCorte(), ahora);
            if (dias > DIAS_CORTE_ANTIGUO) {
                advertencias.add("El último saldo de '" + c.getNombre() + "' tiene " + dias
                        + " días: registre uno nuevo para mejorar la estimación");
            }
            total = total.add(estimado);
            cuentas.add(new SaldoCuentaEstimadoDTO(c.getId(), c.getNombre(), c.getTipo(), sedeCuenta,
                    c.getSede() != null ? c.getSede().getNombre() : null, metodosOrdenados,
                    corte.getSaldo(), corte.getFechaCorte(), posteriores, estimado, dias));
        }

        if (sedeId == null) {
            Set<MetodoPago> sinCuenta = EnumSet.allOf(MetodoPago.class);
            activas.forEach(c -> { if (c.getMetodosPago() != null) sinCuenta.removeAll(c.getMetodosPago()); });
            if (!sinCuenta.isEmpty()) {
                advertencias.add("Los métodos " + sinCuenta + " no están asignados a ninguna cuenta: "
                        + "esos cobros y pagos no se reflejan en el disponible");
            }
        } else if (hayCorporativas) {
            advertencias.add("Las cuentas corporativas (sin sede) no se incluyen al filtrar por sede");
        }
        return new DisponibleCalculadoDTO(total.setScale(2, RoundingMode.HALF_UP), cuentas, advertencias);
    }

    // ============ HELPERS ============

    static Concepto conceptoCxp(TipoReferenciaCxP tipo) {
        return tipo == TipoReferenciaCxP.COMPRA ? Concepto.PAGO_PROVEEDOR : Concepto.OTRO_PAGO;
    }

    /** CxP antiguas guardan el método como texto; MetodoReembolso comparte nombres con MetodoPago. */
    static MetodoPago metodoDesdeTexto(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return MetodoPago.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void agregar(List<Movimiento> items, UUID sedeId, Concepto concepto, MetodoPago metodo, BigDecimal monto) {
        if (monto == null || monto.signum() == 0) return;
        items.add(new Movimiento(sedeId, concepto, metodo, monto));
    }
}
