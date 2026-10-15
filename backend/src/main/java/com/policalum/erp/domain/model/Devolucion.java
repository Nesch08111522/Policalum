package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoDevolucion;

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

/**
 * Devolución (parcial o total) de una venta. Flujo: PENDIENTE → APROBADA | RECHAZADA.
 * Al aprobar se mueve el Kardex; el dinero se devuelve aparte con un Reembolso.
 */
@Entity
@Table(name = "devoluciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Devolucion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por", nullable = false)
    private User solicitadoPor;

    /** Admin que aprobó o rechazó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private User aprobadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoDevolucion estado;

    @Column(name = "fecha_solicitud")
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    /** Nota del admin al aprobar, o motivo del rechazo. */
    @Column(name = "observacion_admin", columnDefinition = "TEXT")
    private String observacionAdmin;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============ NUEVOS CAMPOS (Módulo 17) — nullable para ddl-auto=update ============

    /** DEV-2026-000123 */
    @Column(unique = true, length = 20)
    private String codigo;

    /** Suma de precio con IGV × cantidad devuelta de todas las líneas (tope del reembolso). */
    @Column(name = "monto_total", precision = 12, scale = 2)
    private BigDecimal montoTotal;

    // FIX: @Builder ignoraba los inicializadores de estado, fechaSolicitud y createdAt
    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        if (this.fechaSolicitud == null) this.fechaSolicitud = ahora;
        if (this.estado == null) this.estado = EstadoDevolucion.PENDIENTE;
    }
}
