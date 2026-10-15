package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.NominaListadoDTO;
import com.policalum.erp.application.dto.response.NominaResponseDTO;
import com.policalum.erp.domain.model.RrhhNomina;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo mapeos de salida: la nómina no se construye desde un request, sino desde
 * el legajo y la comisión aprobada (ver NominaServiceImpl.generar), por eso no
 * hay toEntity/updateEntityFromDto.
 */
@Mapper(componentModel = "spring")
public interface NominaMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "empleadoNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "comisionId", source = "comision.id")
    @Mapping(target = "generadoPorNombre", expression = "java(nombreCompleto(entity.getGeneradoPor()))")
    @Mapping(target = "aprobadoPorNombre", expression = "java(nombreCompleto(entity.getAprobadoPor()))")
    @Mapping(target = "pagadoPorNombre", expression = "java(nombreCompleto(entity.getPagadoPor()))")
    NominaResponseDTO toResponseDto(RrhhNomina entity);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "empleadoNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    NominaListadoDTO toListadoDto(RrhhNomina entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }
}
