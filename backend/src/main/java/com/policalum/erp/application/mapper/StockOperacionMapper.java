package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.AjusteStockResponseDTO;
import com.policalum.erp.application.dto.response.AlmacenResumenDTO;
import com.policalum.erp.application.dto.response.StockConsolidadoDTO;
import com.policalum.erp.application.dto.response.TransferenciaStockResponseDTO;
import com.policalum.erp.domain.model.AjusteStock;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.TransferenciaStock;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.projection.StockConsolidadoProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida (Módulo 11): ajustes y transferencias los crea StockOperacionServiceImpl y no
 * se editan (son registros de auditoría), por eso no hay toEntity/updateEntityFromDto.
 */
@Mapper(componentModel = "spring", uses = KardexConsultaMapper.class)
public interface StockOperacionMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "codigoSku", source = "producto.codigoSku")
    @Mapping(target = "unidad", source = "producto.unidad")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "sedeId", source = "almacen.sede.id")
    @Mapping(target = "sedeNombre", source = "almacen.sede.nombre")
    @Mapping(target = "kardexMovimientoId", source = "kardexMovimiento.id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userNombre", expression = "java(nombreCompleto(entity.getUser()))")
    AjusteStockResponseDTO toAjusteDto(AjusteStock entity);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "codigoSku", source = "producto.codigoSku")
    @Mapping(target = "unidad", source = "producto.unidad")
    @Mapping(target = "origen", expression = "java(almacen(entity.getAlmacenOrigen()))")
    @Mapping(target = "destino", expression = "java(almacen(entity.getAlmacenDestino()))")
    @Mapping(target = "entreSedes", expression = "java(Boolean.TRUE.equals(entity.getEntreSedes()))")
    @Mapping(target = "salida", source = "movimientoSalida")
    @Mapping(target = "entrada", source = "movimientoEntrada")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userNombre", expression = "java(nombreCompleto(entity.getUser()))")
    TransferenciaStockResponseDTO toTransferenciaDto(TransferenciaStock entity);

    StockConsolidadoDTO toConsolidadoDto(StockConsolidadoProjection p);

    default AlmacenResumenDTO almacen(Almacen a) {
        if (a == null) return null;
        return new AlmacenResumenDTO(a.getId(), a.getNombre(),
                a.getSede() != null ? a.getSede().getId() : null, a.getSede() != null ? a.getSede().getNombre() : null);
    }

    default long nulo(Long v) {
        return v != null ? v : 0L;
    }

    default String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
