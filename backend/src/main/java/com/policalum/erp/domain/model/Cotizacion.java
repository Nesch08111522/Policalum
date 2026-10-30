package com.policalum.erp.domain.model;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.policalum.erp.domain.enums.CanalCotizacion;
import com.policalum.erp.domain.enums.EstadoCotizacion;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cotizaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cotizacion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoCotizacion estado = EstadoCotizacion.EMITIDA;

    /** Null en filas anteriores a la web: se leen como canal interno. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CanalCotizacion canal = CanalCotizacion.INTERNO;

    @Column(name = "contacto_ruc", length = 11)
    private String contactoRuc;

    @Column(name = "contacto_razon_social", length = 200)
    private String contactoRazonSocial;

    @Column(name = "contacto_correo", length = 150)
    private String contactoCorreo;

    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;

    @Column(precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "fecha_vigencia", nullable = false)
    private LocalDate fechaVigencia;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // FK circular resuelta — se llena al convertir en venta
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.estado == null) this.estado = EstadoCotizacion.EMITIDA;
        if (this.canal == null) this.canal = CanalCotizacion.INTERNO;
        if (this.total == null) this.total = BigDecimal.ZERO;
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // EstadoCotizacion moved to com.policalum.erp.domain.enums.EstadoCotizacion
}
