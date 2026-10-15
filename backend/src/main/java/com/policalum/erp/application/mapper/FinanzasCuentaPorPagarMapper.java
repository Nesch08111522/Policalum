package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.FinanzasCuentaPorPagarRequestDTO;
import com.policalum.erp.application.dto.response.FinanzasCuentaPorPagarResponseDTO;
import com.policalum.erp.domain.model.FinanzasCuentaPorPagar;

@Mapper(componentModel = "spring")
public interface FinanzasCuentaPorPagarMapper {
    FinanzasCuentaPorPagarResponseDTO toResponseDto(FinanzasCuentaPorPagar entity);

    FinanzasCuentaPorPagar toEntity(FinanzasCuentaPorPagarRequestDTO requestDTO);

    void updateEntityFromDto(FinanzasCuentaPorPagarRequestDTO dto, @MappingTarget FinanzasCuentaPorPagar entity);
}
