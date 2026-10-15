package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.FinanzasRetiroSocioRequestDTO;
import com.policalum.erp.application.dto.response.FinanzasRetiroSocioResponseDTO;
import com.policalum.erp.domain.model.FinanzasRetiroSocio;

@Mapper(componentModel = "spring")
public interface FinanzasRetiroSocioMapper {
    FinanzasRetiroSocioResponseDTO toResponseDto(FinanzasRetiroSocio entity);

    FinanzasRetiroSocio toEntity(FinanzasRetiroSocioRequestDTO requestDTO);

    void updateEntityFromDto(FinanzasRetiroSocioRequestDTO dto, @MappingTarget FinanzasRetiroSocio entity);
}
