package com.policalum.erp.infrastructure.repository;

import com.policalum.erp.infrastructure.repository.projection.DevueltoPorProductoProjection;
import com.policalum.erp.domain.enums.EstadoDevolucion;
import com.policalum.erp.domain.enums.EstadoProducto;
import com.policalum.erp.domain.model.DetalleDevolucion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetalleDevolucionRepository extends JpaRepository<DetalleDevolucion, UUID> {

    @EntityGraph(attributePaths = {"detalleVenta", "producto", "fraccion", "almacen"})
    List<DetalleDevolucion> findByDevolucion_Id(UUID devolucionId);

    /** Cantidad ya comprometida de una línea de venta en devoluciones PENDIENTES o APROBADAS. */
    @Query("SELECT COALESCE(SUM(d.cantidadDevuelta), 0) FROM DetalleDevolucion d " +
            "WHERE d.detalleVenta.id = :detalleVentaId AND d.devolucion.estado IN :estados")
    BigDecimal sumarCantidadDevuelta(@Param("detalleVentaId") UUID detalleVentaId,
                                     @Param("estados") Collection<EstadoDevolucion> estados);

    @Query("SELECT COALESCE(SUM(d.cantidadStock), 0) FROM DetalleDevolucion d WHERE " +
            "d.devolucion.estado = com.policalum.erp.domain.enums.EstadoDevolucion.APROBADA AND " +
            "d.estadoProducto IN :estados AND (:sedeId IS NULL OR d.devolucion.sede.id = :sedeId) AND " +
            "d.devolucion.fechaResolucion >= :desde AND d.devolucion.fechaResolucion < :hasta")
    BigDecimal sumarUnidadesAprobadas(@Param("estados") Collection<EstadoProducto> estados,
                                      @Param("sedeId") UUID sedeId,
                                      @Param("desde") LocalDateTime desde,
                                      @Param("hasta") LocalDateTime hasta);

    // ============ Módulo 12: reportes ============

    /**
     * Devoluciones APROBADAS resueltas en [desde, hasta) por producto (mismo criterio de fecha que la
     * Utilidad Neta). Unidades base y monto neto: el subtotal devuelto (con IGV) se lleva a neto con la
     * proporción neto/total de la línea de venta original.
     */
    @Query("SELECT p.id AS productoId, " +
            "COALESCE(SUM(dd.cantidadDevuelta * COALESCE(f.factorDescuentoKardex, 1)), 0) AS cantidad, " +
            "COALESCE(SUM(dd.subtotal * (dv.subtotal - dv.igvLinea) / NULLIF(dv.subtotal, 0)), 0) AS montoNeto " +
            "FROM DetalleDevolucion dd JOIN dd.devolucion d JOIN dd.detalleVenta dv JOIN dd.producto p " +
            "LEFT JOIN dd.fraccion f " +
            "WHERE d.estado = com.policalum.erp.domain.enums.EstadoDevolucion.APROBADA AND " +
            "d.fechaResolucion >= :desde AND d.fechaResolucion < :hasta AND (:sedeId IS NULL OR d.sede.id = :sedeId) AND " +
            "(:categoriaId IS NULL OR p.categoria.id = :categoriaId) " +
            "GROUP BY p.id")
    List<DevueltoPorProductoProjection> devueltoPorProducto(@Param("desde") LocalDateTime desde,
                                                            @Param("hasta") LocalDateTime hasta,
                                                            @Param("sedeId") UUID sedeId,
                                                            @Param("categoriaId") UUID categoriaId);
}
