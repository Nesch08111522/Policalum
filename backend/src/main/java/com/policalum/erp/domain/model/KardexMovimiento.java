package com.policalum.erp.domain.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoReferencia;
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
@Table(name = "kardex_movimientos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KardexMovimiento {

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

    @Column(name = "referencia_id")
    private UUID referenciaId; // ID de Venta, Compra, Devolución, Ajuste

    @Column(name = "tipo_referencia", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoReferencia tipoReferencia;

    @Column(name = "tipo_movimiento", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipoMovimiento;

    @Column(name = "cantidad_movida", nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidadMovida;

    @Column(name = "stock_resultante", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockResultante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String observacion;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * FIX Módulo 10: @Builder ignoraba el inicializador "= LocalDateTime.now()" y
     * KardexServiceImpl crea los movimientos con builder: se guardaban con created_at NULL.
     */
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

}