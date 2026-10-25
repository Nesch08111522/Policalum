package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.ConceptoCajaChica;
import com.policalum.erp.domain.enums.TipoCajaChica;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Movimiento menor del turno abierto: transporte, suministros u otro ingreso/egreso de caja. */
@Entity
@Table(name = "movimientos_caja_chica")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MovimientoCajaChica {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false)
    private TurnoCaja turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoCajaChica tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConceptoCajaChica concepto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(length = 255)
    private String detalle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
