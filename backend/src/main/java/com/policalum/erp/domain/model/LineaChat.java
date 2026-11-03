package com.policalum.erp.domain.model;

import com.policalum.erp.domain.enums.AutorChat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "chat_lineas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LineaChat {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversacion_id", nullable = false)
    private UUID conversacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AutorChat autor;

    @Column(length = 120)
    private String autorNombre;

    @Column(nullable = false, length = 2000)
    private String texto;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
