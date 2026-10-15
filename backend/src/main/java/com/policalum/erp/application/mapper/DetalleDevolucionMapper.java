package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.DetalleDevolucionResponseDTO;
import com.policalum.erp.domain.model.DetalleDevolucion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Solo salida (las líneas se construyen en DevolucionServiceImpl desde DetalleVenta). */
@Mapper(componentModel = "spring")
public interface DetalleDevolucionMapper {

    @Mapping(target = "detalleVentaId", source = "detalleVenta.id")
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "fraccionNombre", source = "fraccion.nombreFraccion")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    DetalleDevolucionResponseDTO toResponseDto(DetalleDevolucion entity);
}
