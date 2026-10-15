package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.EstadoAsistencia;
import com.policalum.erp.domain.enums.TipoMarcacion;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "rrhh_asistencia")
@Data
public class RrhhAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User empleado;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_marcacion", nullable = false, length = 20)
    private TipoMarcacion tipoMarcacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoAsistencia estado = EstadoAsistencia.PRESENTE;

    @Column(name = "dispositivo_info", length = 100)
    private String dispositivoInfo;

    @Column(name = "turno_caja_id")
    private UUID turnoCajaId;

    // ============================================
    // ✅ NUEVOS CAMPOS
    // ============================================

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "hora_entrada")
    private LocalTime horaEntrada;

    @Column(name = "hora_salida")
    private LocalTime horaSalida;

    @Column(name = "horas_trabajadas", precision = 5, scale = 2)
    private BigDecimal horasTrabajadas;

    @Column(name = "turno_trabajo_id")
    private UUID turnoTrabajoId;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        // FIX Módulo 8: respetar la fecha de marcaciones manuales de días pasados
        if (this.fecha == null) this.fecha = LocalDate.now();
    }
}