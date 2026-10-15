package com.policalum.erp.domain.enums;

/** Dónde está el dinero de la empresa (Módulo 6). */
public enum TipoCuentaFinanciera {
    /** Efectivo: cajas de las sedes + caja fuerte. */
    EFECTIVO,
    /** Cuenta corriente o de ahorros en un banco. */
    BANCO,
    /** Billetera o pasarela que retiene saldo (Yape/Plin empresa, Niubiz, etc.). */
    BILLETERA_DIGITAL
}
