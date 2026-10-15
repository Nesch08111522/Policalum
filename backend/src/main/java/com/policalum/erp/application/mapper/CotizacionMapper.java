package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;

import com.policalum.erp.application.dto.request.CotizacionRequestDTO;
import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.domain.model.Cotizacion;

@Mapper(componentModel = "spring")
public interface CotizacionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "venta", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Cotizacion toEntity(CotizacionRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "venta", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(CotizacionRequestDTO dto, @MappingTarget Cotizacion entity);
}
