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

/** Hilo de chat entre un visitante del sitio y el equipo del ERP. */
@Entity
@Table(name = "conversaciones_web")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversacionWeb {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 36)
    private String clave;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String empresa;

    /** ABIERTA, EN_ESPERA o CERRADA. */
    @Column(length = 20)
    private String estado = "ABIERTA";

    @Column(name = "cliente_id")
    private UUID clienteId;

    /** Identificador del navegador. No es una cuenta. */
    @Column(length = 40)
    private String dispositivo;

    @Column(length = 180)
    private String ultimoTexto;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ultimo_mensaje_at")
    private LocalDateTime ultimoMensajeAt;

    @Column(nullable = false)
    private int noLeidosEquipo;

    @Column(nullable = false)
    private int noLeidosCliente;

    private LocalDateTime clienteEscribeHasta;

    private LocalDateTime equipoEscribeHasta;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (estado == null || estado.isBlank()) estado = "ABIERTA";
    }
}
