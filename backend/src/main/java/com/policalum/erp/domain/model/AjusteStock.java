package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Ajuste manual de inventario (Módulo 11): sobrante (ENTRADA) o faltante/merma (SALIDA).
 * Guarda el costo del momento para que el valor auditado no cambie si luego cambia el costo
 * promedio. El movimiento de Kardex correspondiente apunta aquí con referenciaId.
 */
@Entity
@Table(name = "stock_ajustes", indexes = {
        @Index(name = "idx_stock_ajustes_fecha", columnList = "created_at"),
        @Index(name = "idx_stock_ajustes_producto", columnList = "producto_id")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AjusteStock {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** AJ-AAAA-NNNNNN */
    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_id", nullable = false)
    private Almacen almacen;

    @Column(name = "tipo_movimiento", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipoMovimiento;

    /** Con signo: positivo = sobrante, negativo = faltante. */
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "stock_anterior", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockAnterior;

    @Column(name = "stock_resultante", nullable = false, precision = 12, scale = 4)
    private BigDecimal stockResultante;

    /** Costo promedio del producto al momento del ajuste. */
    @Column(name = "costo_unitario", nullable = false, precision = 12, scale = 4)
    private BigDecimal costoUnitario;

    /** |cantidad| × costoUnitario. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false, length = 255)
    private String motivo;

    /** Obligatoria (mín. 30 caracteres) cuando el valor supera el umbral. */
    @Column(columnDefinition = "TEXT")
    private String justificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kardex_movimiento_id")
    private KardexMovimiento kardexMovimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
