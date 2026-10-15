package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoReembolso;
import com.policalum.erp.domain.enums.MetodoReembolso;

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
 * Devolución de dinero de una devolución APROBADA (uno por devolución).
 * Flujo: PENDIENTE → PROCESADO. Si es EFECTIVO, al procesar sale del turno de caja.
 */
@Entity
@Table(name = "reembolsos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reembolso {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "monto_total_reembolso", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotalReembolso;

    @Column(name = "metodo_reembolso", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MetodoReembolso metodoReembolso;

    @Column(length = 100)
    private String referencia;

    /**
     * Admin que lo procesó. Nullable: el reembolso se crea PENDIENTE y se procesa después.
     * OJO: si la tabla ya existía con NOT NULL, ddl-auto=update no lo quita (ver SQL en el resumen).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procesado_por")
    private User procesadoPor;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoReembolso estado;

    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============ NUEVOS CAMPOS (Módulo 17) ============

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private User creadoPor;

    /** Tope = monto total de la devolución. */
    @Column(name = "monto_maximo", precision = 12, scale = 2)
    private BigDecimal montoMaximo;

    /** Obligatorio si se reembolsa menos que el tope (ej. producto USADO). */
    @Column(name = "motivo_ajuste", columnDefinition = "TEXT")
    private String motivoAjuste;

    /** Turno del que salió el efectivo (solo EFECTIVO). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_caja_id")
    private TurnoCaja turnoCaja;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoReembolso.PENDIENTE;
    }
}
