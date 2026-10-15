package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.DetalleCotizacionCommandDTO;
import com.policalum.erp.domain.model.DetalleCotizacion;

@Mapper(componentModel = "spring")
public interface DetalleCotizacionMapper {
    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "cotizacion", ignore = true)
    @org.mapstruct.Mapping(target = "producto", ignore = true)
    @org.mapstruct.Mapping(target = "fraccion", ignore = true)
    @org.mapstruct.Mapping(target = "servicio", ignore = true)
    @org.mapstruct.Mapping(target = "precioUnitarioConIgv", ignore = true)
    @org.mapstruct.Mapping(target = "valorUnitarioNeto", ignore = true)
    @org.mapstruct.Mapping(target = "igvLinea", ignore = true)
    @org.mapstruct.Mapping(target = "porcentajeIgv", ignore = true)
    @org.mapstruct.Mapping(target = "subtotal", ignore = true)
    DetalleCotizacion toEntity(DetalleCotizacionCommandDTO requestDTO);

    void updateEntityFromDto(DetalleCotizacionCommandDTO dto, @MappingTarget DetalleCotizacion entity);
}
