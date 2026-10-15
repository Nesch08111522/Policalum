package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoDespacho;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Orden de despacho (delivery) de una venta. Una venta puede tener varias órdenes.
 * Flujo: PENDIENTE → EN_CAMINO → ENTREGADO | FALLIDO; FALLIDO → PENDIENTE (reprogramación).
 */
@Entity
@Table(name = "ordenes_despacho")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrdenDespacho {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    /** Repartidor asignado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private User responsable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movilidad_id")
    private Movilidad movilidad;

    @Column(name = "direccion_entrega", nullable = false, length = 255)
    private String direccionEntrega;

    @Column(name = "referencia_entrega", length = 255)
    private String referenciaEntrega;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoDespacho estado;

    @Column(name = "fecha_programada")
    private LocalDate fechaProgramada;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /** Ruta relativa de la foto de entrega en disco. Interna: no se expone en el API. */
    @Column(name = "url_evidencia_entrega", length = 255)
    private String urlEvidenciaEntrega;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============ NUEVOS CAMPOS (Módulo 4) — nullable para ddl-auto=update ============

    /** Código corto para seguimiento: DESP-2026-000123. */
    @Column(name = "codigo_seguimiento", unique = true, length = 20)
    private String codigoSeguimiento;

    @Column(name = "contacto_nombre", length = 150)
    private String contactoNombre;

    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion;

    @Column(name = "fecha_salida")
    private LocalDateTime fechaSalida;

    @Column(name = "recibido_por_nombre", length = 150)
    private String recibidoPorNombre;

    @Column(name = "recibido_por_documento", length = 15)
    private String recibidoPorDocumento;

    @Column(name = "latitud_entrega", precision = 9, scale = 6)
    private BigDecimal latitudEntrega;

    @Column(name = "longitud_entrega", precision = 9, scale = 6)
    private BigDecimal longitudEntrega;

    @Column(name = "evidencia_content_type", length = 100)
    private String evidenciaContentType;

    @Column(name = "evidencia_nombre_original", length = 255)
    private String evidenciaNombreOriginal;

    @Column(name = "motivo_fallo", columnDefinition = "TEXT")
    private String motivoFallo;

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private User creadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // FIX: @Builder ignoraba los inicializadores de estado y createdAt
    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        this.updatedAt = ahora;
        if (this.estado == null) this.estado = EstadoDespacho.PENDIENTE;
        if (this.intentosFallidos == null) this.intentosFallidos = 0;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
