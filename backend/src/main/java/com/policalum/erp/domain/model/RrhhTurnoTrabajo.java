package com.policalum.erp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Turno de trabajo de UN empleado. Un empleado puede tener varios turnos activos
 * (rotativos) siempre que no se crucen. Si horaFin < horaInicio el turno es
 * nocturno y termina al día siguiente.
 */
@Entity
@Table(name = "rrhh_turnos_trabajo")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RrhhTurnoTrabajo {

    /** Tolerancia de tardanza si el turno no define una. */
    public static final int TOLERANCIA_DEFAULT_MINUTOS = 10;

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @Column(name = "nombre_turno", nullable = false, length = 50)
    private String nombreTurno; // MAÑANA, TARDE, NOCHE

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dias_semana", nullable = false, columnDefinition = "jsonb")
    private List<String> diasSemana; // ["LUNES","MARTES",...]

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // ============ NUEVO (Módulo 8) ============

    /** Minutos de gracia antes de marcar TARDANZA. Nullable por ddl-auto=update; usar getToleranciaSegura(). */
    @Column(name = "tolerancia_minutos")
    private Integer toleranciaMinutos;

    public int getToleranciaSegura() {
        return toleranciaMinutos != null ? toleranciaMinutos : TOLERANCIA_DEFAULT_MINUTOS;
    }

    // FIX: @Builder ignora los inicializadores de activo y createdAt
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
        if (this.toleranciaMinutos == null) this.toleranciaMinutos = TOLERANCIA_DEFAULT_MINUTOS;
    }
}
