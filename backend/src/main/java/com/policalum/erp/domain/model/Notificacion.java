package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/** Aviso persistente. Se conserva en la vista hasta el purgado permanente a los 30 días. */
@Entity
@Table(name = "notificaciones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 160)
    private String clave;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 400)
    private String detalle;

    @Column(nullable = false, length = 240)
    private String ruta;

    @Column(nullable = false)
    private boolean leida;

    /** Oculta la campana. La página de notificaciones sigue mostrando el aviso como pasado. */
    @Column(nullable = false)
    private boolean eliminada;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "leida_at")
    private LocalDateTime leidaAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
