package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoCxP;
import com.policalum.erp.domain.enums.TipoReferenciaCxP;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "finanzas_cuentas_por_pagar")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinanzasCuentaPorPagar {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    // ============ NUEVAS FKs ============

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compra_id")
    private Compra compra;

    // ====================================

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "tipo_referencia", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoReferenciaCxP tipoReferencia;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_pagado", precision = 12, scale = 2)
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoCxP estado = EstadoCxP.PENDIENTE;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagado_por")
    private User pagadoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.montoPagado == null) this.montoPagado = BigDecimal.ZERO;
        if (this.estado == null) this.estado = EstadoCxP.PENDIENTE;
        this.createdAt = LocalDateTime.now();
    }
}