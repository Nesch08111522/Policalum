package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoRetiro;
import com.policalum.erp.domain.enums.OrigenFondos;

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
 * Retiro de dinero realizado por un socio (dueño) de la empresa.
 * No es un gasto: es una salida patrimonial (separación dueño/empresa).
 */
@Entity
@Table(name = "finanzas_retiros_socio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinanzasRetiroSocio {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    /** Solo se llena cuando origenFondos = CAJA_FISICA. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_caja_id")
    private TurnoCaja turnoCaja;

    @Column(name = "monto_retirado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoRetirado;

    @Column(name = "origen_fondos", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OrigenFondos origenFondos;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_retiro", nullable = false)
    private LocalDateTime fechaRetiro;

    // ============ NUEVOS CAMPOS (Módulo 5) ============

    // Sin nullable=false a nivel BD para que ddl-auto=update pueda agregar la
    // columna aunque la tabla ya tenga filas. El @PrePersist garantiza el valor.
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoRetiro estado;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por")
    private User anuladoPor;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    /** true si al anular se devolvió el monto al turno de caja (turno aún ABIERTO). */
    @Column(name = "impacto_caja_revertido")
    private Boolean impactoCajaRevertido;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        this.createdAt = ahora;
        if (this.fechaRetiro == null) this.fechaRetiro = ahora;
        if (this.estado == null) this.estado = EstadoRetiro.REGISTRADO;
        if (this.impactoCajaRevertido == null) this.impactoCajaRevertido = false;
    }
}
