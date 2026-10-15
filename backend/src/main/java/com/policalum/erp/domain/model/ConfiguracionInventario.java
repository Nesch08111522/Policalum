package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parámetros de inventario (Módulo 14): reglas de ajustes de stock (M11). */
@Entity
@Table(name = "configuracion_inventario")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionInventario {

    /** Tabla de una sola fila (empresa única): el id siempre es 1. */
    public static final Long ID_UNICO = 1L;

    public static final BigDecimal UMBRAL_JUSTIFICACION_DEFAULT = new BigDecimal("500.00");
    public static final int MIN_CARACTERES_JUSTIFICACION_DEFAULT = 30;

    @Id
    private Long id;

    /** Ajustes por encima de este valor (S/) exigen justificación ampliada. */
    @Column(name = "umbral_justificacion_ajuste", nullable = false, precision = 12, scale = 2)
    private BigDecimal umbralJustificacionAjuste;

    @Column(name = "min_caracteres_justificacion", nullable = false)
    private Integer minCaracteresJustificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User actualizadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = ID_UNICO;
        if (this.umbralJustificacionAjuste == null) this.umbralJustificacionAjuste = UMBRAL_JUSTIFICACION_DEFAULT;
        if (this.minCaracteresJustificacion == null) this.minCaracteresJustificacion = MIN_CARACTERES_JUSTIFICACION_DEFAULT;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
