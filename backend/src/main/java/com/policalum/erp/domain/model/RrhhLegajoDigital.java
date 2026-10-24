package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import com.policalum.erp.domain.enums.SistemaPension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rrhh_legajo_digital")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RrhhLegajoDigital {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "tipo_contrato", length = 50)
    private String tipoContrato; // PLANILLA, RECIBO_HONORARIOS, CAS

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "sueldo_base", nullable = false, precision = 12, scale = 2)
    private BigDecimal sueldoBase = BigDecimal.ZERO;

    /** Día del mes en que se cobra, del 1 al 31. Null si aún no está definido. */
    @Column(name = "dia_cobro")
    private Integer diaCobro;

    @Column(name = "porcentaje_comision", precision = 5, scale = 2)
    private BigDecimal porcentajeComision = BigDecimal.ZERO;

    /**
     * LEGACY / DEPRECADO (Módulo 7): los documentos ahora viven en RrhhLegajoDocumento
     * (metadata en BD + archivo en disco). No usar en código nuevo; se conserva solo
     * para no perder datos existentes.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "documentos_json", columnDefinition = "jsonb")
    private Map<String, Object> documentosJson;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ============ NUEVOS CAMPOS (Módulo 9) ============

    /** ONP / AFP / NINGUNO. Null = no registrado (la nómina lo advierte y no descuenta). */
    @Column(name = "sistema_pension", length = 20)
    @Enumerated(EnumType.STRING)
    private SistemaPension sistemaPension;

    /**
     * Tasa de descuento en % (ej. 13.00). Para ONP, si es null se usa 13.00.
     * Para AFP es obligatoria (aporte + comisión + prima de seguro de su AFP).
     */
    @Column(name = "tasa_pension", precision = 5, scale = 2)
    private BigDecimal tasaPension;

    // FIX: @Builder ignora los inicializadores de arriba
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
        if (this.sueldoBase == null) this.sueldoBase = BigDecimal.ZERO;
        if (this.porcentajeComision == null) this.porcentajeComision = BigDecimal.ZERO;
    }
}
