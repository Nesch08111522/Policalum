package com.policalum.erp.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.policalum.erp.application.dto.request.StockRequestDTO;
import com.policalum.erp.application.dto.response.StockResponseDTO;
import com.policalum.erp.domain.model.Stock;

@Mapper(componentModel = "spring")
public interface StockMapper {
    StockResponseDTO toResponseDto(Stock stock);

    Stock toEntity(StockRequestDTO stockRequestDTO);

    void updateEntityFromDto(StockRequestDTO dto, @MappingTarget Stock entity);
}
