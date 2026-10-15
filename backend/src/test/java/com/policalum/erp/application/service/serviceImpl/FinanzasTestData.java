package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.UtilidadNetaResponseDTO;
import com.policalum.erp.infrastructure.repository.projection.MontoPorSedeProjection;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Datos de prueba compartidos por los tests del Módulo 6. */
final class FinanzasTestData {

    private FinanzasTestData() { }

    static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    /** Utilidad con lo esencial; el resto en cero. */
    static UtilidadNetaResponseDTO utilidad(String ventasNetas, String devolucionesNetas, String compras, String nominas,
                                            String utilidad, String margen, String retiros, long cantVentas, String ventasTotales) {
        BigDecimal reales = bd(ventasNetas).subtract(bd(devolucionesNetas));
        return new UtilidadNetaResponseDTO(null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                bd(ventasNetas), BigDecimal.ZERO, bd(ventasTotales), cantVentas,
                bd(devolucionesNetas), bd(devolucionesNetas), 0, reales,
                bd(compras), bd(compras), 0, 0,
                reales.subtract(bd(compras)), bd(nominas), 0, bd(utilidad), bd(utilidad), bd(margen),
                bd(retiros), bd(utilidad).subtract(bd(retiros)), BigDecimal.ZERO);
    }

    static UtilidadNetaResponseDTO utilidadCero() {
        return utilidad("0", "0", "0", "0", "0", "0", "0", 0, "0");
    }

    static MontoPorSedeProjection monto(UUID sede, String total) {
        return new MontoPorSedeProjection() {
            public UUID getSedeId() { return sede; }
            public BigDecimal getTotal() { return new BigDecimal(total); }
        };
    }
}
