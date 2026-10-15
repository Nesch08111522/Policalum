package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoProducto;

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
@Table(name = "detalle_devolucion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DetalleDevolucion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "devolucion_id", nullable = false)
    private Devolucion devolucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "detalle_venta_id", nullable = false)
    private DetalleVenta detalleVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fraccion_id")
    private CatalogoFraccion fraccion;

    /** En las mismas unidades de la línea de venta (ej. 2 medias planchas = 2). */
    @Column(name = "cantidad_devuelta", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadDevuelta;

    /** Precio unitario CON IGV de la venta original. */
    @Column(name = "precio_unitario_ref", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitarioRef;

    @Column(name = "estado_producto", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoProducto estadoProducto;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    // ============ NUEVOS CAMPOS (Módulo 17) ============

    /** precioUnitarioRef × cantidadDevuelta. */
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** Cantidad en unidades de stock (cantidad × factor de la fracción): lo que mueve el Kardex. */
    @Column(name = "cantidad_stock", precision = 12, scale = 4)
    private BigDecimal cantidadStock;

    /** Almacén al que reingresa (el mismo de la salida original). Se fija al aprobar. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "almacen_id")
    private Almacen almacen;

    @PrePersist
    void prePersist() {
        if (this.estadoProducto == null) this.estadoProducto = EstadoProducto.BUENO;
    }
}
