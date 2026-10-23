package com.policalum.erp.domain.model;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.UUID;
 
@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
 
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Rol rol;
 
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;
 
    @Column(name = "nombre_usuario", unique = true, nullable = false, length = 50)
    private String nombreUsuario;
 
    @Column(nullable = false, length = 100)
    private String nombre;
 
    @Column(nullable = false, length = 100)
    private String apellido;
 
    @Column(unique = true, nullable = false, length = 150)
    private String email;
 
    // FIX Módulo 10 (seguridad): varias respuestas devuelven la entidad User completa
    // (p. ej. KardexMovimientoResponseDTO); estos campos nunca deben salir en el JSON.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
 
    @Column(length = 15)
    private String celular;

    @Column(length = 15)
    private String documento;

    @Column(length = 500)
    private String biografia;

    @Column(columnDefinition = "text")
    private String foto;

    @Column(name = "ultimo_ip", length = 45)
    private String ultimoIp;

    @Column(name = "sesion_version")
    private Integer sesionVersion = 0;
 
    @Column(nullable = false)
    private Boolean activo = true;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "totp_secret", length = 100)
    private String totpSecret;  // Secreto TOTP del empleado

    @Column(name = "totp_enabled")
    private Boolean totpEnabled = false;  // Si ya configuró TOTP


    @Column(name = "ultimo_login")
    private LocalDateTime ultimoLogin;
 
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist(){
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.activo == null) this.activo = true;
        if (this.totpEnabled == null) this.totpEnabled = false;
        if (this.sesionVersion == null) this.sesionVersion = 0;
        this.createdAt = LocalDateTime.now();
    }

}