package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoCompra;
import com.policalum.erp.domain.enums.EstadoPago;
import com.policalum.erp.domain.enums.EstadoRecepcion;
import com.policalum.erp.domain.enums.Moneda;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "compras")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Compra {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ============ NUEVOS CAMPOS ============

    @Column(name = "numero_orden_compra", unique = true, length = 20)
    private String numeroOrdenCompra;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento_pago")
    private LocalDate fechaVencimientoPago;

    @Column(nullable = false, length = 5)
    @Enumerated(EnumType.STRING)
    private Moneda moneda = Moneda.PEN;

    @Column(name = "estado_compra", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoCompra estadoCompra = EstadoCompra.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por")
    private User anuladoPor;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<DetalleCompra> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CompraAdjunto> adjuntos = new ArrayList<>();

    // ======================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "numero_factura_proveedor", length = 50)
    private String numeroFacturaProveedor;

    @Column(name = "subtotal_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalNeto = BigDecimal.ZERO;

    @Column(name = "total_igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIgv = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "estado_recepcion", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoRecepcion estadoRecepcion = EstadoRecepcion.PENDIENTE;

    @Column(name = "estado_pago", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============ HELPERS ============

    public void addDetalle(DetalleCompra detalle) {
        detalles.add(detalle);
        detalle.setCompra(this);
    }

    public void clearDetalles() {
        detalles.clear();
    }

    public void addAdjunto(CompraAdjunto adjunto) {
        adjuntos.add(adjunto);
        adjunto.setCompra(this);
    }

    @PrePersist
    void prePersist() {
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.subtotalNeto == null) this.subtotalNeto = BigDecimal.ZERO;
        if (this.totalIgv == null) this.totalIgv = BigDecimal.ZERO;
        if (this.total == null) this.total = BigDecimal.ZERO;
        if (this.estadoRecepcion == null) this.estadoRecepcion = EstadoRecepcion.PENDIENTE;
        if (this.estadoPago == null) this.estadoPago = EstadoPago.PENDIENTE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.estadoCompra == null) this.estadoCompra = EstadoCompra.BORRADOR;
        if (this.moneda == null) this.moneda = Moneda.PEN;
    }
}