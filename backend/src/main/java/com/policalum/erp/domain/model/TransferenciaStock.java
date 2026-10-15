package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Transferencia entre almacenes (Módulo 11). Agrupa la SALIDA del origen y la ENTRADA del
 * destino del Kardex; ambos movimientos llevan referenciaId = id de esta transferencia.
 */
@Entity
@Table(name = "stock_transferencias", indexes = @Index(name = "idx_stock_transferencias_fecha", columnList = "created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferenciaStock {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** TRF-AAAA-NNNNNN */
    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_origen_id", nullable = false)
    private Almacen almacenOrigen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "almacen_destino_id", nullable = false)
    private Almacen almacenDestino;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal cantidad;

    @Column(name = "entre_sedes", nullable = false)
    private Boolean entreSedes;

    @Column(length = 255)
    private String motivo;

    @Column(name = "referencia_documento", length = 100)
    private String referenciaDocumento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_salida_id")
    private KardexMovimiento movimientoSalida;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_entrada_id")
    private KardexMovimiento movimientoEntrada;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.entreSedes == null) this.entreSedes = false;
    }
}
