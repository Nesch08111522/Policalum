package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.AuditLogRequestDTO;
import com.policalum.erp.application.dto.response.AuditLogResponseDTO;
import com.policalum.erp.domain.model.AuditLog;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    AuditLogResponseDTO toResponseDto(AuditLog entity);

    AuditLog toEntity(AuditLogRequestDTO requestDTO);

    void updateEntityFromDto(AuditLogRequestDTO dto, @MappingTarget AuditLog entity);
}
