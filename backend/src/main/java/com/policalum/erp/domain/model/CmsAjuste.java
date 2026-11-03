package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una sola fila. El sitio lee estos valores al cargar. */
@Entity
@Table(name = "cms_ajustes")
@Getter
@Setter
@NoArgsConstructor
public class CmsAjuste {

    public static final short ID = 1;

    @Id
    private Short id = ID;

    @Column(name = "color_primario", nullable = false, length = 7)
    private String colorPrimario = "#1FA2D6";

    @Column(name = "color_secundario", nullable = false, length = 7)
    private String colorSecundario = "#FFFFFF";

    @Column(name = "tamano_base", nullable = false)
    private Integer tamanoBase = 16;

    @Column(nullable = false, length = 20)
    private String modo = "aether";
}
