package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import com.policalum.erp.application.service.UtilidadNetaService;
import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.Moneda;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.CompraRepository;
import com.policalum.erp.infrastructure.repository.DevolucionRepository;
import com.policalum.erp.infrastructure.repository.FinanzasRetiroSocioRepository;
import com.policalum.erp.infrastructure.repository.KardexRepository;
import com.policalum.erp.infrastructure.repository.RrhhNominaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.repository.VentaRepository;
import com.policalum.erp.infrastructure.repository.projection.MontoCantidadProjection;
import com.policalum.erp.infrastructure.repository.projection.TotalesFinancierosProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
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
public class UtilidadNetaServiceImpl implements UtilidadNetaService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    /** 1 + IGV: las devoluciones se guardan con IGV (precio de venta). */
    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");

    private final VentaRepository ventaRepository;
    private final CompraRepository compraRepository;
    private final RrhhNominaRepository nominaRepository;
    private final FinanzasRetiroSocioRepository retiroRepository;
    private final UserRepository userRepository;
    private final DevolucionRepository devolucionRepository;
    private final KardexRepository kardexRepository;

    @Override
    public UtilidadNetaResponseDTO calcular(UUID sedeId, LocalDate desde, LocalDate hasta) {
        exigirAdmin(usuarioActual());
        LocalDate d = desde != null ? desde : LocalDate.now().withDayOfMonth(1);
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        if (d.isAfter(h)) throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");

        LocalDateTime ini = d.atStartOfDay();
        LocalDateTime finExclusivo = h.plusDays(1).atStartOfDay();

        // Ventas COMPLETADAS (las ANULADAS no son ingreso)
        TotalesFinancierosProjection ventas = ventaRepository.totalesPorPeriodo(EstadoVenta.COMPLETADA, ini, finExclusivo, sedeId);
        BigDecimal ventasNetas = neto(ventas);
        BigDecimal igvVentas = ventas != null ? cero(ventas.getIgv()) : BigDecimal.ZERO;
        BigDecimal ventasTotales = ventas != null ? cero(ventas.getTotal()) : BigDecimal.ZERO;
        long cantVentas = cantidad(ventas);

        // Módulo 6: devoluciones APROBADAS en el periodo (fecha de resolución) restan a las ventas
        MontoCantidadProjection devueltas = devolucionRepository.totalDevueltoPorPeriodo(sedeId, ini, finExclusivo);
        BigDecimal devolucionesTotales = devueltas != null ? cero(devueltas.getTotal()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
        BigDecimal devolucionesNetas = devolucionesTotales.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        long cantDevoluciones = devueltas != null && devueltas.getCantidad() != null ? devueltas.getCantidad() : 0L;
        BigDecimal ventasNetasReales = ventasNetas.subtract(devolucionesNetas);

        // Compras RECIBIDAS en PEN (las de otra moneda se excluyen e informan)
        TotalesFinancierosProjection compras = compraRepository.totalesPorPeriodo(EstadoCompra.RECIBIDA, Moneda.PEN, d, h, sedeId);
        BigDecimal comprasNetas = neto(compras);
        BigDecimal comprasTotales = compras != null ? cero(compras.getTotal()) : BigDecimal.ZERO;
        long cantCompras = cantidad(compras);
        long comprasOtraMoneda = compraRepository.contarEnOtraMoneda(EstadoCompra.RECIBIDA, Moneda.PEN, d, h, sedeId);

        // Nóminas PAGADAS: el periodo es "YYYY-MM", se toman meses completos
        String periodoDesde = YearMonth.from(d).toString();
        String periodoHasta = YearMonth.from(h).toString();
        BigDecimal nominas = cero(nominaRepository.sumarTotalNeto(EstadoNomina.PAGADA, periodoDesde, periodoHasta, sedeId));
        long cantNominas = nominaRepository.contar(EstadoNomina.PAGADA, periodoDesde, periodoHasta, sedeId);

        // Retiros: informativo, no resta a la utilidad
        BigDecimal retiros = cero(retiroRepository.sumarMonto(EstadoRetiro.REGISTRADO, sedeId, ini, finExclusivo));

        // Mermas: informativo. Con el método "compras como costo" la mercadería ya se restó al comprarla
        BigDecimal mermas = cero(kardexRepository.valorMermas(sedeId, ini, finExclusivo)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal utilidadBruta = ventasNetasReales.subtract(comprasNetas);
        BigDecimal utilidadOperativa = utilidadBruta.subtract(nominas);
        BigDecimal utilidadNeta = utilidadOperativa;
        BigDecimal margen = ventasNetasReales.signum() > 0
                ? utilidadNeta.multiply(CIEN).divide(ventasNetasReales, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2);

        if (comprasOtraMoneda > 0) {
            log.warn("Utilidad neta {} a {}: {} compras en moneda distinta a PEN excluidas", d, h, comprasOtraMoneda);
        }

        return new UtilidadNetaResponseDTO(sedeId, d, h,
                ventasNetas, igvVentas, ventasTotales, cantVentas,
                devolucionesNetas, devolucionesTotales, cantDevoluciones, ventasNetasReales,
                comprasNetas, comprasTotales, cantCompras, comprasOtraMoneda,
                utilidadBruta, nominas, cantNominas, utilidadOperativa, utilidadNeta, margen,
                retiros, utilidadNeta.subtract(retiros), mermas);
    }

    @Override
    public UtilidadNetaResponseDTO calcularPorPeriodo(UUID sedeId, String periodo) {
        YearMonth mes;
        try {
            mes = YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Periodo inválido: '" + periodo + "'. Formato esperado: YYYY-MM");
        }
        return calcular(sedeId, mes.atDay(1), mes.atEndOfMonth());
    }

    // ============ HELPERS ============

    private BigDecimal neto(TotalesFinancierosProjection p) {
        return p != null ? cero(p.getNeto()) : BigDecimal.ZERO;
    }

    private long cantidad(TotalesFinancierosProjection p) {
        return p != null && p.getCantidad() != null ? p.getCantidad() : 0L;
    }

    private BigDecimal cero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private void exigirAdmin(User user) {
        if (user.getRol() == null || user.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede consultar la utilidad neta");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
