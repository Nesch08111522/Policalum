package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aviso operativo que el sitio muestra en la portada. */
@Entity
@Table(name = "horario_operativo")
@Getter
@Setter
@NoArgsConstructor
public class HorarioOperativo {

    public static final short ID = 1;

    @Id
    private Short id = ID;

    @Column(nullable = false, length = 180)
    private String mensaje = "";

    @Column(nullable = false)
    private Boolean activo = false;
}
