package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.RegistrarSaldoCuentaRequestDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaResponseDTO;
import com.policalum.erp.domain.model.SaldoCuentaFinanciera;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Los cortes no se editan; updateEntityFromDto existe por convención y no se expone. */
@Mapper(componentModel = "spring")
public interface SaldoCuentaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    @Mapping(target = "registradoPor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    SaldoCuentaFinanciera toEntity(RegistrarSaldoCuentaRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    @Mapping(target = "registradoPor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(RegistrarSaldoCuentaRequestDTO dto, @MappingTarget SaldoCuentaFinanciera entity);

    @Mapping(target = "cuentaId", source = "cuenta.id")
    @Mapping(target = "registradoPorNombre", expression = "java(nombreCompleto(entity.getRegistradoPor()))")
    SaldoCuentaResponseDTO toResponseDto(SaldoCuentaFinanciera entity);

    default String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
