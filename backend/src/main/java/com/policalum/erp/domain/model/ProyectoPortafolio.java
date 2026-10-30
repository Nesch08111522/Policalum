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

/** Trabajo publicado en la galería del portal. Lo carga el equipo desde el ERP. */
@Entity
@Table(name = "proyectos_portafolio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProyectoPortafolio {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 800)
    private String descripcion;

    @Column(length = 500)
    private String imagenUrl;

    @Column(nullable = false)
    private Integer orden = 0;

    @Column(nullable = false)
    private Boolean publicado = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (orden == null) orden = 0;
        if (publicado == null) publicado = true;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
