package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.TurnoCajaRequestDTO;
import com.policalum.erp.application.dto.response.TurnoCajaResponseDTO;
import com.policalum.erp.domain.model.TurnoCaja;

@Mapper(componentModel = "spring")
public interface TurnoCajaMapper {
    TurnoCajaResponseDTO toResponseDto(TurnoCaja turnoCaja);

    TurnoCaja toEntity(TurnoCajaRequestDTO turnoCajaRequestDTO);

    void updateEntityFromDto(TurnoCajaRequestDTO dto, @MappingTarget TurnoCaja entity);
}
