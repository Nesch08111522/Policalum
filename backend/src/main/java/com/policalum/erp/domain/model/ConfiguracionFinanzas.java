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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parámetros de finanzas (Módulo 14): proyección de flujo de caja (M6). */
@Entity
@Table(name = "configuracion_finanzas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionFinanzas {

    /** Tabla de una sola fila (empresa única): el id siempre es 1. */
    public static final Long ID_UNICO = 1L;

    public static final int MESES_HISTORICOS_DEFAULT = 3;
    public static final int MESES_PROYECCION_DEFAULT = 3;
    public static final int MESES_PROYECCION_MAX_DEFAULT = 12;

    @Id
    private Long id;

    /** Meses completos anteriores que se promedian para la tendencia. */
    @Column(name = "meses_historicos_proyeccion", nullable = false)
    private Integer mesesHistoricosProyeccion;

    @Column(name = "meses_proyeccion_default", nullable = false)
    private Integer mesesProyeccionDefault;

    @Column(name = "meses_proyeccion_max", nullable = false)
    private Integer mesesProyeccionMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User actualizadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = ID_UNICO;
        if (this.mesesHistoricosProyeccion == null) this.mesesHistoricosProyeccion = MESES_HISTORICOS_DEFAULT;
        if (this.mesesProyeccionDefault == null) this.mesesProyeccionDefault = MESES_PROYECCION_DEFAULT;
        if (this.mesesProyeccionMax == null) this.mesesProyeccionMax = MESES_PROYECCION_MAX_DEFAULT;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
