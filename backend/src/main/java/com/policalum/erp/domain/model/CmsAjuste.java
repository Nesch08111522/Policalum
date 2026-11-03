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

    @Column(length = 180)
    private String banner;

    @Column(length = 500)
    private String resumen;

    @Column(name = "horario_semana", length = 80)
    private String horarioSemana;

    @Column(name = "horario_sabado", length = 80)
    private String horarioSabado;

    @Column(length = 200)
    private String facebook;

    @Column(length = 200)
    private String instagram;

    @Column(length = 200)
    private String linkedin;

    @Column(length = 200)
    private String youtube;

    @Column(name = "correo_soporte", length = 150)
    private String correoSoporte;

    @Column(name = "texto_privacidad", columnDefinition = "TEXT")
    private String textoPrivacidad;

    @Column(name = "texto_terminos", columnDefinition = "TEXT")
    private String textoTerminos;

    @Column(name = "texto_garantia", columnDefinition = "TEXT")
    private String textoGarantia;

    @Column(name = "texto_faq", columnDefinition = "TEXT")
    private String textoFaq;

    @Column(name = "hero_kicker", length = 80)
    private String heroKicker;

    @Column(name = "hero_titulo", length = 180)
    private String heroTitulo;

    @Column(name = "hero_texto", length = 400)
    private String heroTexto;
}
