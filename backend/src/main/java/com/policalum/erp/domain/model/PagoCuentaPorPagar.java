package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.MetodoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Cada abono a una cuenta por pagar (Módulo 6). Antes solo se guardaba el acumulado
 * (montoPagado), por lo que el flujo de caja no sabía cuándo salió cada pago parcial.
 */
@Entity
@Table(name = "finanzas_pagos_cxp",
        indexes = @Index(name = "idx_pagos_cxp_fecha", columnList = "fecha_pago"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoCuentaPorPagar {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_por_pagar_id", nullable = false)
    private FinanzasCuentaPorPagar cuentaPorPagar;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Column(length = 100)
    private String referencia;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private User registradoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        if (this.fechaPago == null) this.fechaPago = ahora;
    }
}
