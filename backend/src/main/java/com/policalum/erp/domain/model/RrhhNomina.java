package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.policalum.erp.domain.enums.EstadoNomina;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.enums.SistemaPension;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nómina mensual de UN empleado. Flujo: PENDIENTE → APROBADA → PAGADA.
 * Los campos de cálculo son una "foto" del legajo al generar, para que cambios
 * posteriores del legajo no alteren nóminas ya emitidas.
 */
@Entity
@Table(name = "rrhh_nominas",
        uniqueConstraints = @UniqueConstraint(name = "uk_nomina_user_periodo", columnNames = {"user_id", "periodo"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RrhhNomina {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String periodo; // Ej: "2026-05"

    @Column(name = "sueldo_base_calculado", nullable = false, precision = 12, scale = 2)
    private BigDecimal sueldoBaseCalculado;

    @Column(name = "total_comisiones", precision = 12, scale = 2)
    private BigDecimal totalComisiones;

    /** Total de descuentos = descuentoPension + otrosDescuentos. */
    @Column(precision = 12, scale = 2)
    private BigDecimal descuentos;

    @Column(name = "total_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalNeto;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoNomina estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    private User aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ============ NUEVOS CAMPOS (Módulo 9) — nullable para ddl-auto=update ============

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sede_id")
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comision_id")
    private RrhhComision comision;

    @Column(name = "tipo_contrato", length = 50)
    private String tipoContrato;

    @Column(name = "dias_computados")
    private Integer diasComputados;

    @Column(name = "ventas_base_comision", precision = 12, scale = 2)
    private BigDecimal ventasBaseComision;

    @Column(name = "porcentaje_comision", precision = 5, scale = 2)
    private BigDecimal porcentajeComision;

    @Column(name = "sistema_pension", length = 20)
    @Enumerated(EnumType.STRING)
    private SistemaPension sistemaPension;

    @Column(name = "tasa_pension", precision = 5, scale = 2)
    private BigDecimal tasaPension;

    @Column(name = "descuento_pension", precision = 12, scale = 2)
    private BigDecimal descuentoPension;

    @Column(name = "otros_descuentos", precision = 12, scale = 2)
    private BigDecimal otrosDescuentos;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generado_por")
    private User generadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagado_por")
    private User pagadoPor;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "metodo_pago", length = 30)
    @Enumerated(EnumType.STRING)
    private MetodoPago metodoPago;

    @Column(name = "referencia_pago", length = 100)
    private String referenciaPago;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    // FIX: antes createdAt/estado/totalComisiones/descuentos tenían inicializadores que @Builder ignora
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoNomina.PENDIENTE;
        if (this.totalComisiones == null) this.totalComisiones = BigDecimal.ZERO;
        if (this.descuentoPension == null) this.descuentoPension = BigDecimal.ZERO;
        if (this.otrosDescuentos == null) this.otrosDescuentos = BigDecimal.ZERO;
        if (this.descuentos == null) this.descuentos = this.descuentoPension.add(this.otrosDescuentos);
    }
}
