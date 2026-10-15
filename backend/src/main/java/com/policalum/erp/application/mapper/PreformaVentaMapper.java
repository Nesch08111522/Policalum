package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.DetallePreformaResponseDTO;
import com.policalum.erp.application.dto.response.PreformaListadoDTO;
import com.policalum.erp.application.dto.response.PreformaVentaResponseDTO;
import com.policalum.erp.domain.enums.EstadoPreforma;
import com.policalum.erp.domain.model.PreformaVenta;
import com.policalum.erp.domain.model.User;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida: precios, estado, código y vigencia los asigna PreformaServiceImpl, por eso
 * no hay toEntity/updateEntityFromDto.
 */
@Mapper(componentModel = "spring")
public interface PreformaVentaMapper {

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "codigo", source = "entity.codigo")
    @Mapping(target = "sedeId", source = "entity.sede.id")
    @Mapping(target = "sedeNombre", source = "entity.sede.nombre")
    @Mapping(target = "vendedorId", source = "entity.user.id")
    @Mapping(target = "vendedorNombre", expression = "java(nombreCompleto(entity.getUser()))")
    @Mapping(target = "clienteId", source = "entity.cliente.id")
    @Mapping(target = "clienteNombre", source = "entity.cliente.nombre")
    @Mapping(target = "estado", source = "entity.estado")
    @Mapping(target = "subtotalNetoEstimado", source = "entity.subtotalNetoEstimado")
    @Mapping(target = "totalIgvEstimado", source = "entity.totalIgvEstimado")
    @Mapping(target = "totalEstimado", source = "entity.totalEstimado")
    @Mapping(target = "fechaExpiracion", source = "entity.fechaExpiracion")
    @Mapping(target = "minutosRestantes", expression = "java(minutosRestantes(entity))")
    @Mapping(target = "observaciones", source = "entity.observaciones")
    @Mapping(target = "ventaId", source = "entity.venta.id")
    @Mapping(target = "ventaSerie", source = "entity.venta.serieCorrelativo")
    @Mapping(target = "confirmadoPorNombre", expression = "java(nombreCompleto(entity.getConfirmadoPor()))")
    @Mapping(target = "fechaConfirmacion", source = "entity.fechaConfirmacion")
    @Mapping(target = "canceladoPorNombre", expression = "java(nombreCompleto(entity.getCanceladoPor()))")
    @Mapping(target = "fechaCancelacion", source = "entity.fechaCancelacion")
    @Mapping(target = "motivoCancelacion", source = "entity.motivoCancelacion")
    @Mapping(target = "createdAt", source = "entity.createdAt")
    @Mapping(target = "lineas", source = "lineas")
    PreformaVentaResponseDTO toResponseDto(PreformaVenta entity, List<DetallePreformaResponseDTO> lineas);

    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "vendedorNombre", expression = "java(nombreCompleto(entity.getUser()))")
    PreformaListadoDTO toListadoDto(PreformaVenta entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }

    default long minutosRestantes(PreformaVenta p) {
        if (p.getEstado() != EstadoPreforma.ABIERTA || p.getFechaExpiracion() == null) return 0;
        return Math.max(0, Duration.between(LocalDateTime.now(), p.getFechaExpiracion()).toMinutes());
    }
}
