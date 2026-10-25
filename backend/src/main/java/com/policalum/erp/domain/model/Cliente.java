package com.policalum.erp.domain.model;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clientes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class
Cliente {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;
    @OneToMany(mappedBy = "cliente", fetch = FetchType.LAZY)
    private List<Venta> ventas = new ArrayList<>();
    @Column(name = "tipo_documento", length = 10)
    private String tipoDocumento = "DNI"; // DNI, RUC, CE, PASAPORTE

    @Column(name = "numero_documento", unique = true, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 150)
    private String email;

    @Column(length = 15)
    private String celular;

    @Column(length = 255)
    private String direccion;

    /** Null se trata como activo. La baja con historial deja el registro en false. */
    @Column(columnDefinition = "boolean default true")
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist(){
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.tipoDocumento == null) this.tipoDocumento = "DNI";
        if (this.activo == null) this.activo = true;
        this.createdAt = LocalDateTime.now();
    }
}