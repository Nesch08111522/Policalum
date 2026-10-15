package com.policalum.erp.domain.model;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.policalum.erp.domain.enums.AccionAudit;
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
@Table(name = "audit_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String tabla;

    @Column(name = "registro_id", nullable = false)
    private UUID registroId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccionAudit accion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "datos_json", columnDefinition = "jsonb")
    private Map<String, Object> datosJson;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * FIX Módulo 13: @Builder ignoraba el inicializador "= LocalDateTime.now()" y
     * AuditServiceImpl crea los logs con builder, así que se guardaban con created_at NULL.
     */
    @PrePersist
    void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

    // AccionAudit enum moved to com.policalum.erp.domain.enums.AccionAudit
}