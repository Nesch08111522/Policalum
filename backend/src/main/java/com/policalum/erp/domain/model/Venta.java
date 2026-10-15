package com.policalum.erp.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoSunat;
import com.policalum.erp.domain.enums.EstadoVenta;
import com.policalum.erp.domain.enums.TipoComprobante;
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
@Table(name = "ventas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Venta {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // FK circular con cotizaciones — se gestiona en CasoDeUso
    @Column(name = "cotizacion_id")
    private UUID cotizacionId;

    // FK circular resuelta: ventas → preformas_venta
    @Column(name = "preforma_id")
    private UUID preformaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_caja_id")
    private TurnoCaja turnoCaja;

    @Column(name = "tipo_comprobante", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TipoComprobante tipoComprobante = TipoComprobante.BOLETA;

    @Column(name = "serie_correlativo", unique = true, length = 50)
    private String serieCorrelativo;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @Column(name = "subtotal_neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalNeto = BigDecimal.ZERO;

    @Column(name = "total_igv", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIgv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "canal_venta", length = 10)
    @Enumerated(EnumType.STRING)
    private CanalVenta canalVenta = CanalVenta.LOCAL;

    @Column(name = "estado_sunat", length = 30)
    @Enumerated(EnumType.STRING)
    private EstadoSunat estadoSunat = EstadoSunat.PENDIENTE;

    @Column(name = "hash_cpe", length = 255)
    private String hashCpe;

    @Column(name = "mensaje_sunat", columnDefinition = "TEXT")
    private String mensajeSunat;

    @Column(name = "referencia_pedido_online", unique = true, length = 100)
    private String referenciaPedidoOnline;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * FIX Módulo 5: @Builder ignora el inicializador de createdAt, por lo que las
     * ventas creadas con Venta.builder() (VentaServiceImpl.registrar) se guardaban
     * con created_at = NULL y quedaban fuera de cualquier filtro por fecha.
     */
    @PrePersist
    void prePersist() {
        // FIX deuda técnica: @Builder (y MapStruct, que usa el builder) ignora los inicializadores de campo
        if (this.tipoComprobante == null) this.tipoComprobante = TipoComprobante.BOLETA;
        if (this.estado == null) this.estado = EstadoVenta.COMPLETADA;
        if (this.subtotalNeto == null) this.subtotalNeto = BigDecimal.ZERO;
        if (this.totalIgv == null) this.totalIgv = BigDecimal.ZERO;
        if (this.canalVenta == null) this.canalVenta = CanalVenta.LOCAL;
        if (this.estadoSunat == null) this.estadoSunat = EstadoSunat.PENDIENTE;
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }

}