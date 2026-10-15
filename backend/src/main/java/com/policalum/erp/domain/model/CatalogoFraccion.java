package com.policalum.erp.domain.model;
import java.math.BigDecimal;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "catalogo_fracciones",
       uniqueConstraints = @UniqueConstraint(name = "uq_producto_fraccion",
               columnNames = {"producto_id", "factor_descuento_kardex"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CatalogoFraccion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(name = "nombre_fraccion", nullable = false, length = 50)
    private String nombreFraccion; // 'Plancha Completa', 'Media Plancha', etc.

    @Column(name = "factor_descuento_kardex", nullable = false, precision = 5, scale = 4)
    private BigDecimal factorDescuentoKardex; // 1.0, 0.75, 0.5, 0.25

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVenta;

    @Column(nullable = false)
    private Boolean activo = true;

    /**
     * FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores
     * de campo; sin esto se guardaban NULL o fallaba el NOT NULL.
     */
    @PrePersist
    void prePersist() {
        if (this.activo == null) this.activo = true;
    }
}
