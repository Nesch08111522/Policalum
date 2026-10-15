package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.ComisionResponseDTO;
import com.policalum.erp.domain.model.RrhhComision;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Solo salida: la comisión se calcula a partir de las ventas (ComisionServiceImpl.calcular). */
@Mapper(componentModel = "spring")
public interface ComisionMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "empleadoNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "aprobadoPorNombre", expression = "java(nombreCompleto(entity.getAprobadoPor()))")
    ComisionResponseDTO toResponseDto(RrhhComision entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }
}
