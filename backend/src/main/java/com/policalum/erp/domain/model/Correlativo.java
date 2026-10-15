package com.policalum.erp.domain.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contador transaccional de numeración (ej. clave "DESP-2026" → último 123).
 * La clave natural reemplaza al UUID a propósito: es la que se bloquea con FOR UPDATE.
 * Reutilizable para series de comprobantes (Módulo 2) y preformas (Módulo 18).
 */
@Entity
@Table(name = "correlativos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Correlativo {

    @Id
    @Column(length = 40, nullable = false, updatable = false)
    private String clave;

    @Column(nullable = false)
    private Long ultimo;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = LocalDateTime.now();
        if (this.ultimo == null) this.ultimo = 0L;
    }
}
