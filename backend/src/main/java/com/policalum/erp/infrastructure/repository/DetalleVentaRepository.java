package com.policalum.erp.infrastructure.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import com.policalum.erp.infrastructure.repository.projection.RentabilidadProjection;
import com.policalum.erp.domain.model.DetalleVenta;
import java.util.UUID;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, UUID> {
    @EntityGraph(attributePaths = {"producto", "fraccion", "servicio"})
    List<DetalleVenta> findByVenta_Id(UUID ventaId);

    // ============ Módulo 12: reportes ============

    /**
     * Ventas COMPLETADAS en [desde, hasta) por producto o servicio. Cantidad en unidades base (fracción ×
     * factor de Kardex). Ingreso neto = subtotal − IGV de la línea (valorUnitarioNeto guarda el neto de la
     * línea, no el unitario). costoUnitario es el costo promedio ponderado ACTUAL del producto.
     */
    @Query("SELECT p.id AS productoId, s.id AS servicioId, COALESCE(p.nombre, s.nombre) AS nombre, " +
            "p.codigoSku AS sku, p.unidad AS unidad, c.nombre AS categoria, p.precioCompra AS costoUnitario, " +
            "COALESCE(SUM(dv.cantidad * COALESCE(f.factorDescuentoKardex, 1)), 0) AS cantidad, " +
            "COALESCE(SUM(dv.subtotal - dv.igvLinea), 0) AS ingresoNeto, COUNT(DISTINCT v.id) AS ventas " +
            "FROM DetalleVenta dv JOIN dv.venta v LEFT JOIN dv.producto p LEFT JOIN p.categoria c " +
            "LEFT JOIN dv.servicio s LEFT JOIN dv.fraccion f " +
            "WHERE v.estado = com.policalum.erp.domain.enums.EstadoVenta.COMPLETADA AND " +
            "v.createdAt >= CAST(:desde AS timestamp) AND v.createdAt < CAST(:hasta AS timestamp) AND (CAST(:sedeId AS string) IS NULL OR v.sede.id = :sedeId) AND " +
            "(CAST(:categoriaId AS string) IS NULL OR c.id = :categoriaId) " +
            "GROUP BY p.id, s.id, p.nombre, s.nombre, p.codigoSku, p.unidad, c.nombre, p.precioCompra")
    List<RentabilidadProjection> rentabilidad(@Param("desde") LocalDateTime desde,
                                              @Param("hasta") LocalDateTime hasta,
                                              @Param("sedeId") UUID sedeId,
                                              @Param("categoriaId") UUID categoriaId);
}
