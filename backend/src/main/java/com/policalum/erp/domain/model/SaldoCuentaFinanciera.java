package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Saldo real de una cuenta a una fecha (corte), registrado por el admin
 * (estado de cuenta del banco, arqueo de la caja fuerte). Es un historial:
 * nunca se edita, se registra un corte nuevo.
 */
@Entity
@Table(name = "finanzas_saldos_cuenta",
        indexes = @Index(name = "idx_saldos_cuenta_fecha", columnList = "cuenta_id, fecha_corte"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SaldoCuentaFinanciera {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaFinanciera cuenta;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;

    /** Momento al que corresponde el saldo (no cuando se digitó). */
    @Column(name = "fecha_corte", nullable = false)
    private LocalDateTime fechaCorte;

    @Column(length = 500)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private User registradoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        if (this.fechaCorte == null) this.fechaCorte = ahora;
    }
}
