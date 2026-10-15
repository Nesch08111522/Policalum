package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.request.FiltroVentasReporteDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.ArqueosReporteDTO;
import com.policalum.erp.application.dto.response.ClientesFrecuentesDTO;
import com.policalum.erp.application.dto.response.ProductosEstrellaDTO;
import com.policalum.erp.application.dto.response.ProveedoresRankingDTO;
import com.policalum.erp.application.dto.response.RentabilidadReporteDTO;
import com.policalum.erp.application.dto.response.VentaReporteDTO;
import com.policalum.erp.application.dto.response.VentasResumenDTO;
import com.policalum.erp.domain.enums.AgrupacionReporte;
import com.policalum.erp.domain.enums.CriterioRankingCliente;
import com.policalum.erp.domain.enums.CriterioRankingProducto;
import com.policalum.erp.domain.enums.CriterioRankingProveedor;
import com.policalum.erp.domain.enums.EstadoTurno;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Reportes consolidados y exportables (Módulo 12). Sin fechas = mes en curso; rango inclusivo [desde, hasta].
 * MULTIFUNCIONAL: solo su sede y sin costos (criterios basados en costos → 403). Exportaciones CSV: solo
 * ADMIN, con tope de filas de Configuración > reportes, auditadas.
 */
public interface ReporteService {

    /** Totales, serie por día/semana/mes, cobros por método y devoluciones del periodo. */
    VentasResumenDTO resumenVentas(FiltroVentasReporteDTO filtro, AgrupacionReporte agrupacion);

    /** Detalle paginado de ventas, más recientes primero. */
    Page<VentaReporteDTO> ventas(FiltroVentasReporteDTO filtro, Pageable pageable);

    /** CSV del detalle de ventas. Solo ADMIN. */
    ArchivoDescargaDTO exportarVentas(FiltroVentasReporteDTO filtro);

    /** Cantidad, ingreso, devoluciones, costo estimado y margen por producto/servicio. */
    RentabilidadReporteDTO rentabilidad(LocalDate desde, LocalDate hasta, UUID sedeId, UUID categoriaId);

    /** CSV de rentabilidad (con costos). Solo ADMIN. */
    ArchivoDescargaDTO exportarRentabilidad(LocalDate desde, LocalDate hasta, UUID sedeId, UUID categoriaId);

    /** Top N de productos por cantidad, ingreso o margen (margen solo ADMIN). */
    ProductosEstrellaDTO productosEstrella(LocalDate desde, LocalDate hasta, UUID sedeId, UUID categoriaId,
                                           CriterioRankingProducto criterio, Integer top);

    /** Top N de clientes por cantidad de compras, monto neto o frecuencia. */
    ClientesFrecuentesDTO clientesFrecuentes(LocalDate desde, LocalDate hasta, UUID sedeId,
                                             CriterioRankingCliente criterio, Integer top);

    /** CSV con todos los clientes del periodo en el orden del criterio. Solo ADMIN. */
    ArchivoDescargaDTO exportarClientes(LocalDate desde, LocalDate hasta, UUID sedeId, CriterioRankingCliente criterio);

    /** Top N de proveedores por volumen (solo ADMIN), frecuencia o días de entrega. */
    ProveedoresRankingDTO rankingProveedores(LocalDate desde, LocalDate hasta, UUID sedeId,
                                             CriterioRankingProveedor criterio, Integer top);

    /** CSV con todos los proveedores del periodo. Solo ADMIN. */
    ArchivoDescargaDTO exportarProveedores(LocalDate desde, LocalDate hasta, UUID sedeId, CriterioRankingProveedor criterio);

    /** Arqueo por turno (por fecha de apertura) con resumen; MULTIFUNCIONAL solo ve sus turnos. */
    ArqueosReporteDTO arqueos(LocalDate desde, LocalDate hasta, UUID sedeId, UUID usuarioId, EstadoTurno estado,
                              Pageable pageable);

    /** CSV de arqueos. Solo ADMIN. */
    ArchivoDescargaDTO exportarArqueos(LocalDate desde, LocalDate hasta, UUID sedeId, UUID usuarioId, EstadoTurno estado);
}
