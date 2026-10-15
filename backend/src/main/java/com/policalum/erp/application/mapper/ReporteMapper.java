package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.VentaReporteDTO;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.domain.model.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Reportes (Módulo 12). Solo de salida: los reportes leen entidades y proyecciones y nunca crean ni editan
 * registros, por eso no hay toEntity ni updateEntityFromDto. Los reportes agregados (rentabilidad,
 * rankings, arqueos) se arman en ReporteServiceImpl porque son cálculos, no copias de campos.
 */
@Mapper(componentModel = "spring")
public interface ReporteMapper {

    @Mapping(target = "id", source = "venta.id")
    @Mapping(target = "serieCorrelativo", source = "venta.serieCorrelativo")
    @Mapping(target = "fecha", source = "venta.createdAt")
    @Mapping(target = "sede", source = "venta.sede.nombre")
    @Mapping(target = "vendedor", expression = "java(nombreCompleto(venta.getUser()))")
    @Mapping(target = "cliente", source = "venta.cliente.nombre")
    @Mapping(target = "clienteDocumento", source = "venta.cliente.numeroDocumento")
    @Mapping(target = "canal", source = "venta.canalVenta")
    @Mapping(target = "tipoComprobante", source = "venta.tipoComprobante")
    @Mapping(target = "estado", source = "venta.estado")
    @Mapping(target = "neto", source = "venta.subtotalNeto")
    @Mapping(target = "igv", source = "venta.totalIgv")
    @Mapping(target = "total", source = "venta.total")
    @Mapping(target = "metodosPago", source = "metodosPago")
    VentaReporteDTO toResponseDto(Venta venta, String metodosPago);

    default String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
