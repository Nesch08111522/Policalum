package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoPreforma;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pre-venta de mostrador sin validez comercial (no reserva stock ni toca caja).
 * Flujo: ABIERTA → CONFIRMADA (crea la Venta) | CANCELADA | EXPIRADA.
 */
@Entity
@Table(name = "preformas_venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PreformaVenta {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    /** Vendedor que la armó. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    /** Turno en el que se cobró (se llena al confirmar). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_caja_id")
    private TurnoCaja turnoCaja;

    @Column(name = "subtotal_neto_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalNetoEstimado;

    @Column(name = "total_igv_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIgvEstimado;

    @Column(name = "total_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEstimado;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoPreforma estado;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /** Se llena al confirmar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============ NUEVOS CAMPOS (Módulo 18) — nullable para ddl-auto=update ============

    /** PRE-2026-000123 */
    @Column(unique = true, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmado_por")
    private User confirmadoPor;

    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelado_por")
    private User canceladoPor;

    @Column(name = "fecha_cancelacion")
    private LocalDateTime fechaCancelacion;

    @Column(name = "motivo_cancelacion", columnDefinition = "TEXT")
    private String motivoCancelacion;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // FIX: @Builder ignoraba los inicializadores de estado, totales y createdAt
    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        this.updatedAt = ahora;
        if (this.estado == null) this.estado = EstadoPreforma.ABIERTA;
        if (this.subtotalNetoEstimado == null) this.subtotalNetoEstimado = BigDecimal.ZERO;
        if (this.totalIgvEstimado == null) this.totalIgvEstimado = BigDecimal.ZERO;
        if (this.totalEstimado == null) this.totalEstimado = BigDecimal.ZERO;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** ABIERTA con fecha de expiración ya pasada (aún no marcada como EXPIRADA). */
    public boolean estaVencida(LocalDateTime ahora) {
        return estado == EstadoPreforma.ABIERTA && fechaExpiracion != null && fechaExpiracion.isBefore(ahora);
    }
}
