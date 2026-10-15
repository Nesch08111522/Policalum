package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.UserRequestDTO;
import com.policalum.erp.application.dto.response.UserResponseDTO;
import com.policalum.erp.domain.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponseDTO toResponseDto(User user);

    // Mapeos explícitos para asegurar que MapStruct no se pierda con las relaciones y los campos
    @Mapping(target = "rol", source = "rol")
    @Mapping(target = "sede", source = "sede")
    @Mapping(target = "passwordHash", source = "passwordHash")
    User toEntity(UserRequestDTO userRequestDTO);

    @Mapping(target = "rol", source = "rol")
    @Mapping(target = "sede", source = "sede")
    @Mapping(target = "passwordHash", source = "passwordHash")
    void updateEntityFromDto(UserRequestDTO dto, @MappingTarget User user);
}