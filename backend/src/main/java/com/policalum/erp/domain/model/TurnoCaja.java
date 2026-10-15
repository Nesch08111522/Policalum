package com.policalum.erp.domain.model;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.policalum.erp.domain.enums.EstadoTurno;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "turnos_caja")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TurnoCaja {

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

    @Column(name = "monto_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoInicial;

    @Column(name = "monto_final_declarado", precision = 12, scale = 2)
    private BigDecimal montoFinalDeclarado;

    @Column(name = "fecha_apertura")
    private LocalDateTime fechaApertura = LocalDateTime.now();

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EstadoTurno estado = EstadoTurno.ABIERTO;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /**
     * Efectivo retirado por socios desde este turno (Módulo 5).
     * Se descuenta del efectivo esperado al cerrar caja.
     * Nullable en BD para que ddl-auto=update funcione con turnos existentes;
     * usar getTotalRetirosSociosSeguro() al leerlo.
     */
    @Column(name = "total_retiros_socios", precision = 12, scale = 2)
    private BigDecimal totalRetirosSocios;

    /** Devuelve totalRetirosSocios o cero si es null (turnos anteriores al Módulo 5). */
    public BigDecimal getTotalRetirosSociosSeguro() {
        return totalRetirosSocios != null ? totalRetirosSocios : BigDecimal.ZERO;
    }

    /** Reembolsos de devoluciones pagados en efectivo desde este turno (Módulo 17). */
    @Column(name = "total_reembolsos", precision = 12, scale = 2)
    private BigDecimal totalReembolsos;

    public BigDecimal getTotalReembolsosSeguro() {
        return totalReembolsos != null ? totalReembolsos : BigDecimal.ZERO;
    }

    /** Todo el efectivo que salió del turno: retiros de socios + reembolsos. */
    public BigDecimal getEgresosEfectivoSeguro() {
        return getTotalRetirosSociosSeguro().add(getTotalReembolsosSeguro());
    }

    @PrePersist
    void prePersist() {
        if (this.fechaApertura == null) this.fechaApertura = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoTurno.ABIERTO;
        if (this.totalRetirosSocios == null) this.totalRetirosSocios = BigDecimal.ZERO;
        if (this.totalReembolsos == null) this.totalReembolsos = BigDecimal.ZERO;
    }

}