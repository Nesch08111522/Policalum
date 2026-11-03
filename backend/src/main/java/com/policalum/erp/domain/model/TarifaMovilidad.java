package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Precio de movilidad de un distrito. El base cubre hasta las unidades incluidas; el resto se cobra por pieza. */
@Entity
@Table(name = "tarifa_movilidad")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TarifaMovilidad {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 80)
    private String distrito;

    @Column(name = "precio_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBase;

    @Column(name = "unidades_incluidas", nullable = false)
    private Integer unidadesIncluidas;

    @Column(name = "precio_unidad_extra", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnidadExtra;

    @Column(nullable = false)
    private Boolean activo;

    @PrePersist
    void prePersist() {
        if (this.activo == null) this.activo = true;
        if (this.unidadesIncluidas == null) this.unidadesIncluidas = 0;
        if (this.precioBase == null) this.precioBase = BigDecimal.ZERO;
        if (this.precioUnidadExtra == null) this.precioUnidadExtra = BigDecimal.ZERO;
    }
}
