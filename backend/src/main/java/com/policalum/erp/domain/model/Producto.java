package com.policalum.erp.domain.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.TipoProducto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "productos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "codigo_sku", length = 50)
    private String codigoSku;

    @Column(nullable = false, length = 255)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoProducto tipo = TipoProducto.FISICO;

    @Column(name = "precio_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioCompra;

    @Column(name = "precio_venta_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioVentaBase;

    @Column(length = 20)
    private String unidad = "UNID"; // UNID, CAJ, KG, etc.

    @Column(name = "es_fraccionable", nullable = false)
    private Boolean esFraccionable = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "incluye_igv")
    private Boolean incluyeIgv = true;

    @Column(name = "tipo_afectacion", length = 10)
    private String tipoAfectacion = "10";


    @Builder.Default
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CatalogoFraccion> fracciones = new ArrayList<>();


    public void addFraccion(CatalogoFraccion fraccion){
        fracciones.add(fraccion);
        fraccion.setProducto(this);
    }

    public void clearFracciones(){
        fracciones.clear();
    }

    /**
     * FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores
     * de campo; sin esto se guardaban NULL o fallaba el NOT NULL.
     */
    @PrePersist
    void prePersist() {
        if (this.tipo == null) this.tipo = TipoProducto.FISICO;
        if (this.unidad == null) this.unidad = "UNID";
        if (this.esFraccionable == null) this.esFraccionable = false;
        if (this.activo == null) this.activo = true;
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.incluyeIgv == null) this.incluyeIgv = true;
        if (this.tipoAfectacion == null) this.tipoAfectacion = "10";
    }
}
