package com.policalum.erp.application.dto.response;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un monto separado en efectivo y medios digitales (Yape, Plin, tarjeta, transferencia...). */
public record MontoPorMedioDTO(BigDecimal efectivo, BigDecimal digital, BigDecimal total) {

    public static final MontoPorMedioDTO CERO = de(BigDecimal.ZERO, BigDecimal.ZERO);

    public static MontoPorMedioDTO de(BigDecimal efectivo, BigDecimal digital) {
        BigDecimal e = efectivo.setScale(2, RoundingMode.HALF_UP);
        BigDecimal d = digital.setScale(2, RoundingMode.HALF_UP);
        return new MontoPorMedioDTO(e, d, e.add(d));
    }

    public MontoPorMedioDTO mas(MontoPorMedioDTO otro) {
        return de(efectivo.add(otro.efectivo()), digital.add(otro.digital()));
    }

    public MontoPorMedioDTO menos(MontoPorMedioDTO otro) {
        return de(efectivo.subtract(otro.efectivo()), digital.subtract(otro.digital()));
    }
}
