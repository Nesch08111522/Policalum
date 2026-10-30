package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.EstadoMensajeWeb;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Prospecto que llega del formulario de contacto del portal. */
@Entity
@Table(name = "mensajes_web")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MensajeContacto {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String empresa;

    @Column(nullable = false, length = 120)
    private String asunto;

    @Column(nullable = false, length = 2000)
    private String mensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMensajeWeb estado = EstadoMensajeWeb.NUEVO;

    @Column(length = 500)
    private String notaGestion;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "gestionado_at")
    private LocalDateTime gestionadoAt;

    @PrePersist
    void prePersist() {
        if (estado == null) estado = EstadoMensajeWeb.NUEVO;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
