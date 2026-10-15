package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.TipoCuentaFinanciera;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * Cuenta donde la empresa guarda dinero (Módulo 6). Montos siempre en PEN.
 *
 * metodosPago indica qué cobros y pagos pasan por esta cuenta (p. ej. BCP recibe
 * TRANSFERENCIA y TARJETA). Con eso el sistema estima el saldo actual:
 * último saldo registrado por el admin + movimientos de esos métodos desde esa fecha.
 *
 * Un método solo puede estar en una cuenta activa por sede. Una cuenta sin sede
 * (corporativa) cubre todas las sedes, así que no puede convivir con otra cuenta
 * del mismo método en una sede concreta.
 */
@Entity
@Table(name = "finanzas_cuentas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CuentaFinanciera {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoCuentaFinanciera tipo;

    /** Banco o proveedor (BCP, Interbank, Niubiz...). */
    @Column(length = 100)
    private String entidad;

    /** Solo los últimos dígitos del número de cuenta, para identificarla. */
    @Column(name = "ultimos_digitos", length = 6)
    private String ultimosDigitos;

    /** null = cuenta corporativa (todas las sedes). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "finanzas_cuenta_metodos", joinColumns = @JoinColumn(name = "cuenta_id"))
    @Column(name = "metodo_pago", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<MetodoPago> metodosPago;

    @Column(nullable = false)
    private Boolean activa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private User creadoPor;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.createdAt == null) this.createdAt = ahora;
        this.updatedAt = ahora;
        if (this.activa == null) this.activa = true;
        if (this.metodosPago == null) this.metodosPago = new HashSet<>();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /** true si ambas cuentas podrían recibir movimientos de la misma sede. */
    public boolean compartePosibleSede(CuentaFinanciera otra) {
        if (this.sede == null || otra.getSede() == null) return true;
        return this.sede.getId().equals(otra.getSede().getId());
    }
}
