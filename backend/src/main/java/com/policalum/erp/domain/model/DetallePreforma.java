package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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

@Entity
@Table(name = "detalle_preforma")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetallePreforma {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preforma_id", nullable = false)
    private PreformaVenta preforma;

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
    private BigDecimal porcentajeIgv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "nota_cambio", columnDefinition = "TEXT")
    private String notaCambio;

    @PrePersist
    void prePersist() {
        if (this.porcentajeIgv == null) this.porcentajeIgv = new BigDecimal("18.00");
    }
}
