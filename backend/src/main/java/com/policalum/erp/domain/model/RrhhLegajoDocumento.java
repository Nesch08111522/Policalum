package com.policalum.erp.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.TipoDocumentoLegajo;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadata de un documento del legajo. El archivo vive en disco
 * (./uploads/legajos/YYYY/MM/uuid.ext) y aquí solo se guarda su ruta relativa.
 */
@Entity
@Table(name = "rrhh_legajo_documentos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RrhhLegajoDocumento {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legajo_id", nullable = false)
    private RrhhLegajoDigital legajo;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private TipoDocumentoLegajo tipo;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    /** Ruta relativa en disco. Interna: nunca se expone en el API. */
    @Column(name = "ruta_archivo", nullable = false, length = 500)
    private String rutaArchivo;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(length = 255)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subido_por", nullable = false)
    private User subidoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
