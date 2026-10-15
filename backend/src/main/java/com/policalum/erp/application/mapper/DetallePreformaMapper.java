package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.DetallePreformaResponseDTO;
import com.policalum.erp.domain.model.DetallePreforma;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Solo salida: las líneas se calculan en PreformaServiceImpl con los precios del catálogo. */
@Mapper(componentModel = "spring")
public interface DetallePreformaMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "fraccionId", source = "fraccion.id")
    @Mapping(target = "fraccionNombre", source = "fraccion.nombreFraccion")
    @Mapping(target = "servicioId", source = "servicio.id")
    @Mapping(target = "servicioNombre", source = "servicio.nombre")
    DetallePreformaResponseDTO toResponseDto(DetallePreforma entity);
}
