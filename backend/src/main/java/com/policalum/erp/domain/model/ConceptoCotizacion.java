package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.TipoCargoCotizacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Cargo extra del formato (monto fijo o porcentaje del subtotal de materiales). */
@Entity
@Table(name = "concepto_cotizacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConceptoCotizacion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCargoCotizacion tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false)
    private Integer orden;

    @PrePersist
    void prePersist() {
        if (this.activo == null) this.activo = true;
        if (this.orden == null) this.orden = 0;
        if (this.valor == null) this.valor = BigDecimal.ZERO;
        if (this.tipo == null) this.tipo = TipoCargoCotizacion.MONTO;
    }
}
