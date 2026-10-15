package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.LegajoRequestDTO;
import com.policalum.erp.application.dto.response.LegajoDocumentoResponseDTO;
import com.policalum.erp.application.dto.response.LegajoListadoDTO;
import com.policalum.erp.application.dto.response.LegajoResponseDTO;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.RrhhLegajoDocumento;
import com.policalum.erp.domain.model.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * user, tipoContrato (normalizado), tasaPension (resuelta según el sistema) y activo
 * los asigna LegajoServiceImpl; aquí se ignoran.
 */
@Mapper(componentModel = "spring")
public interface LegajoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tipoContrato", ignore = true)
    @Mapping(target = "tasaPension", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "documentosJson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RrhhLegajoDigital toEntity(LegajoRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "tipoContrato", ignore = true)
    @Mapping(target = "tasaPension", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "documentosJson", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(LegajoRequestDTO dto, @MappingTarget RrhhLegajoDigital entity);

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "empleadoId", source = "entity.user.id")
    @Mapping(target = "empleadoNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "empleadoEmail", source = "entity.user.email")
    @Mapping(target = "sedeId", source = "entity.user.sede.id")
    @Mapping(target = "sedeNombre", source = "entity.user.sede.nombre")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "documentos", source = "documentos")
    LegajoResponseDTO toResponseDto(RrhhLegajoDigital entity, List<LegajoDocumentoResponseDTO> documentos);

    @Mapping(target = "empleadoId", source = "user.id")
    @Mapping(target = "empleadoNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "sedeNombre", source = "user.sede.nombre")
    LegajoListadoDTO toListadoDto(RrhhLegajoDigital entity);

    @Mapping(target = "legajoId", source = "legajo.id")
    @Mapping(target = "subidoPorNombre", expression = "java(nombreCompleto(entity.getSubidoPor()))")
    LegajoDocumentoResponseDTO toDocumentoDto(RrhhLegajoDocumento entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }
}
