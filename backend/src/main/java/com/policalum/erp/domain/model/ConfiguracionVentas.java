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

/** Parámetros de ventas (Módulo 14): vigencias de cotizaciones (M3) y preformas (M18). */
@Entity
@Table(name = "configuracion_ventas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionVentas {

    /** Tabla de una sola fila (empresa única): el id siempre es 1. */
    public static final Long ID_UNICO = 1L;

    public static final int VIGENCIA_COTIZACION_DIAS_DEFAULT = 15;
    public static final int PREFORMA_MINUTOS_DEFAULT = 120;
    public static final int PREFORMA_MINUTOS_MIN_DEFAULT = 5;
    public static final int PREFORMA_MINUTOS_MAX_DEFAULT = 4320;

    @Id
    private Long id;

    @Column(name = "vigencia_cotizacion_dias", nullable = false)
    private Integer vigenciaCotizacionDias;

    @Column(name = "preforma_minutos_default", nullable = false)
    private Integer preformaMinutosDefault;

    @Column(name = "preforma_minutos_min", nullable = false)
    private Integer preformaMinutosMin;

    @Column(name = "preforma_minutos_max", nullable = false)
    private Integer preformaMinutosMax;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User actualizadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = ID_UNICO;
        if (this.vigenciaCotizacionDias == null) this.vigenciaCotizacionDias = VIGENCIA_COTIZACION_DIAS_DEFAULT;
        if (this.preformaMinutosDefault == null) this.preformaMinutosDefault = PREFORMA_MINUTOS_DEFAULT;
        if (this.preformaMinutosMin == null) this.preformaMinutosMin = PREFORMA_MINUTOS_MIN_DEFAULT;
        if (this.preformaMinutosMax == null) this.preformaMinutosMax = PREFORMA_MINUTOS_MAX_DEFAULT;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
