package com.policalum.erp.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proveedores",
        uniqueConstraints = @UniqueConstraint(name = "uk_documento_proveedor",
                columnNames = {"tipo_documento", "numero_documento"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Proveedor {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nombre_razon", nullable = false, length = 255)
    private String nombreRazon;

    @Column(name = "tipo_documento", nullable = false, length = 10)
    private String tipoDocumento = "RUC";

    @Column(name = "numero_documento", nullable = false, length = 15)
    private String numeroDocumento;

    @Column(length = 15)
    private String celular;

    @Column(length = 150)
    private String email;

    @Column(length = 255)
    private String direccion;



    @Column(name = "condiciones_pago", length = 20)
    private String condicionesPago = "CONTADO"; // CONTADO, CREDITO_30, CREDITO_60

    @Column(name = "dias_credito")
    private Integer diasCredito = 0;

    @Column(length = 100)
    private String banco;

    @Column(name = "cuenta_bancaria", length = 50)
    private String cuentaBancaria;

    // ======================================

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.tipoDocumento == null) this.tipoDocumento = "RUC";
        if (this.activo == null) this.activo = true;
        this.createdAt = LocalDateTime.now();
        if (this.condicionesPago == null) this.condicionesPago = "CONTADO";
        if (this.diasCredito == null) this.diasCredito = 0;
    }
}