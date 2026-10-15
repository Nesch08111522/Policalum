package com.policalum.erp.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import com.policalum.erp.domain.enums.RolNombre;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
 
@Entity
@Table(name = "roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rol {
 
    @Id
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    @GeneratedValue
    private UUID id;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private RolNombre nombre;
 
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permisos_json", columnDefinition = "jsonb")
    private Map<String, Object> permisosJson;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
}
 