package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.LogAuditoriaResponseDTO;
import com.policalum.erp.domain.model.AuditLog;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida: el log de auditoría es inmutable y solo lo escribe AuditService.registrar,
 * por eso no hay toEntity/updateEntityFromDto.
 */
@Mapper(componentModel = "spring")
public interface LogAuditoriaMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "userEmail", source = "user.email")
    LogAuditoriaResponseDTO toResponseDto(AuditLog entity);

    default String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
