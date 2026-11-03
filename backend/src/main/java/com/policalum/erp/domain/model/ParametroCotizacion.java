package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una sola fila: el porcentaje de utilidad que el portal suma al subtotal de materiales. */
@Entity
@Table(name = "parametro_cotizacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParametroCotizacion {

    public static final Long ID_UNICO = 1L;

    @Id
    private Long id;

    @Column(name = "utilidad_porcentaje", nullable = false, precision = 6, scale = 2)
    private BigDecimal utilidadPorcentaje;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = ID_UNICO;
        if (this.utilidadPorcentaje == null) this.utilidadPorcentaje = BigDecimal.ZERO;
    }
}
