package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoComision;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comisión mensual de un vendedor: % de su legajo sobre sus ventas COMPLETADAS
 * sin IGV del periodo (todas las sedes). Una por vendedor y periodo.
 */
@Entity
@Table(name = "rrhh_comisiones",
        uniqueConstraints = @UniqueConstraint(name = "uk_comision_user_periodo", columnNames = {"user_id", "periodo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RrhhComision {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Sede del vendedor (para filtrar); las ventas se suman de todas las sedes. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(nullable = false, length = 7)
    private String periodo; // "YYYY-MM"

    @Column(name = "total_ventas_netas", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalVentasNetas;

    @Column(name = "cantidad_ventas", nullable = false)
    private Long cantidadVentas;

    @Column(name = "porcentaje_comision", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComision;

    @Column(name = "monto_comision", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoComision;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoComision estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private User aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        if (this.estado == null) this.estado = EstadoComision.CALCULADA;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
