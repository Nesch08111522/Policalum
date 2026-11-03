package com.policalum.erp.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Par de claves del servidor para enviar notificaciones al navegador. Una sola fila. */
@Entity
@Table(name = "vapid_claves")
@Getter
@Setter
@NoArgsConstructor
public class VapidClave {

    public static final short ID = 1;

    @Id
    private Short id = ID;

    @Column(nullable = false, length = 200)
    private String publica;

    @Column(nullable = false, length = 200)
    private String privada;
}
