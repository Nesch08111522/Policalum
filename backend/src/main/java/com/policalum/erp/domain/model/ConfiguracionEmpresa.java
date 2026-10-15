package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de la empresa emisora (Módulo 14). RUC y dirección los usará SUNAT (Módulo 2).
 * Mientras ruc sea null la empresa se considera "sin configurar".
 */
@Entity
@Table(name = "configuracion_empresa")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfiguracionEmpresa {

    /** Tabla de una sola fila (empresa única): el id siempre es 1. */
    public static final Long ID_UNICO = 1L;

    public static final String NOMBRE_COMERCIAL_DEFAULT = "POLICALUM";

    @Id
    private Long id;

    @Column(length = 11)
    private String ruc;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", nullable = false, length = 150)
    private String nombreComercial;

    @Column(name = "direccion_fiscal", length = 300)
    private String direccionFiscal;

    /** Código INEI de 6 dígitos (departamento + provincia + distrito). */
    @Column(length = 6)
    private String ubigeo;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String email;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por")
    private User actualizadoPor;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = ID_UNICO;
        if (this.nombreComercial == null) this.nombreComercial = NOMBRE_COMERCIAL_DEFAULT;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
