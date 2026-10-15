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

/** Parámetros de reportes (Módulo 14): límites de exportación (M10 Kardex, M13 Auditoría). */
@Entity
@Table(name = "configuracion_reportes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionReportes {

    /** Tabla de una sola fila (empresa única): el id siempre es 1. */
    public static final Long ID_UNICO = 1L;

    public static final int MAX_FILAS_EXPORTACION_DEFAULT = 50_000;

    @Id
    private Long id;

    @Column(name = "max_filas_exportacion", nullable = false)
    private Integer maxFilasExportacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User actualizadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = ID_UNICO;
        if (this.maxFilasExportacion == null) this.maxFilasExportacion = MAX_FILAS_EXPORTACION_DEFAULT;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
