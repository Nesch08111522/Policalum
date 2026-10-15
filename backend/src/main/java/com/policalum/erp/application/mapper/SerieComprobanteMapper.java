package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.SerieComprobanteRequestDTO;
import com.policalum.erp.application.dto.response.SerieComprobanteResponseDTO;
import com.policalum.erp.domain.model.SerieComprobante;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** sede, serie normalizada (mayúsculas), activa y actualizadoPor los asigna ConfiguracionServiceImpl. */
@Mapper(componentModel = "spring")
public interface SerieComprobanteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "serie", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SerieComprobante toEntity(SerieComprobanteRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "tipoComprobante", ignore = true)
    @Mapping(target = "serie", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "actualizadoPor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(SerieComprobanteRequestDTO dto, @MappingTarget SerieComprobante entity);

    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "activa", expression = "java(Boolean.TRUE.equals(entity.getActiva()))")
    SerieComprobanteResponseDTO toResponseDto(SerieComprobante entity);
}
