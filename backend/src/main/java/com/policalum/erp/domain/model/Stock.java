package com.policalum.erp.domain.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "stock",
       uniqueConstraints = @UniqueConstraint(columnNames = {"producto_id", "almacen_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Stock {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(precision = 12, scale = 4)
    private BigDecimal cantidad = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 12, scale = 4)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores
     * de campo; sin esto se guardaban NULL o fallaba el NOT NULL.
     */
    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (this.cantidad == null) this.cantidad = BigDecimal.ZERO;
        if (this.stockMinimo == null) this.stockMinimo = BigDecimal.ZERO;
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }
}
