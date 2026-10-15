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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "detalle_cotizacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleCotizacion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cotizacion_id", nullable = false)
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraccion_id")
    private CatalogoFraccion fraccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "precio_unitario_con_igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioConIgv;

    @Column(name = "valor_unitario_neto", nullable = false, precision = 12, scale = 4)
    private BigDecimal valorUnitarioNeto;

    @Column(name = "igv_linea", nullable = false, precision = 12, scale = 2)
    private BigDecimal igvLinea;

    @Column(name = "porcentaje_igv", precision = 5, scale = 2)
    private BigDecimal porcentajeIgv = new BigDecimal("18.00");

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /**
     * FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores
     * de campo; sin esto se guardaban NULL o fallaba el NOT NULL.
     */
    @PrePersist
    void prePersist() {
        if (this.porcentajeIgv == null) this.porcentajeIgv = new BigDecimal("18.00");
    }
}
