package com.policalum.erp.application.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.RolRequestDTO;
import com.policalum.erp.application.dto.response.RolResponseDTO;
import com.policalum.erp.domain.model.Rol;

@Mapper(componentModel = "spring")
public interface RolMapper {


    Rol toEntity(RolRequestDTO rolRequestDTO);


    RolResponseDTO toResponseDto(Rol rol);

    void updateEntityFromDto(RolRequestDTO dto, @MappingTarget Rol entity);
}
