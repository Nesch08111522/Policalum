package com.policalum.erp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sedes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sede {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String pais = "Perú";

    @Column(length = 100)
    private String ciudad;

    @Column(length = 255)
    private String direccion;

    @Column(name = "celular_sede", length = 20)
    private String celularSede;

    @Column(length = 10)
    private String moneda = "PEN";

    @Column(nullable = false)
    private Boolean activa = true;

    /** Grados decimales. El geocodificador local los completa si la ciudad es conocida. */
    private Double latitud;

    private Double longitud;

    /** Ruta relativa en el almacén de archivos o una URL. */
    @Column(length = 500)
    private String imagen;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.pais == null) this.pais = "Perú";
        if (this.moneda == null) this.moneda = "PEN";
        if (this.activa == null) this.activa = true;
        this.createdAt = LocalDateTime.now();
    }

}