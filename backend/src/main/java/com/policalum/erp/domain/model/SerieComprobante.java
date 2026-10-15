package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.TipoComprobante;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Serie de comprobante por sede (establecimiento) y tipo (Módulo 14). La usará SUNAT (M2):
 * BOLETA = Bxxx, FACTURA = Fxxx. Una serie es única en toda la empresa (mismo RUC).
 */
@Entity
@Table(name = "series_comprobante", uniqueConstraints = {
        @UniqueConstraint(name = "uk_serie_sede_tipo", columnNames = {"sede_id", "tipo_comprobante"}),
        @UniqueConstraint(name = "uk_serie_codigo", columnNames = {"serie"})})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SerieComprobante {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoComprobante tipoComprobante;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private Boolean activa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User actualizadoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        this.updatedAt = ahora;
        if (this.activa == null) this.activa = true;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
