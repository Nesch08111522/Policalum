package com.policalum.erp.domain.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.policalum.erp.domain.enums.MetodoPago;
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
@Table(name = "pagos_venta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PagoVenta {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @Column(name = "metodo_pago", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 100)
    private String referencia; // Código de operación Yape/Plin/Niubiz

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // MetodoPago moved to com.policalum.erp.domain.enums.MetodoPago

    /**
     * FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores
     * de campo; sin esto se guardaban NULL o fallaba el NOT NULL.
     */
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
