package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.DetalleDevolucionResponseDTO;
import com.policalum.erp.application.dto.response.DevolucionListadoDTO;
import com.policalum.erp.application.dto.response.DevolucionResponseDTO;
import com.policalum.erp.domain.model.Devolucion;
import com.policalum.erp.domain.model.Reembolso;
import com.policalum.erp.domain.model.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida: la devolución se arma en DevolucionServiceImpl a partir de las líneas de la
 * venta (precio, fracción, producto), así que no hay toEntity/updateEntityFromDto.
 */
@Mapper(componentModel = "spring", uses = DetalleDevolucionMapper.class)
public interface DevolucionMapper {

    @Mapping(target = "id", source = "entity.id")
    @Mapping(target = "codigo", source = "entity.codigo")
    @Mapping(target = "ventaId", source = "entity.venta.id")
    @Mapping(target = "ventaSerie", source = "entity.venta.serieCorrelativo")
    @Mapping(target = "sedeId", source = "entity.sede.id")
    @Mapping(target = "sedeNombre", source = "entity.sede.nombre")
    @Mapping(target = "clienteId", source = "entity.cliente.id")
    @Mapping(target = "clienteNombre", source = "entity.cliente.nombre")
    @Mapping(target = "motivo", source = "entity.motivo")
    @Mapping(target = "estado", source = "entity.estado")
    @Mapping(target = "montoTotal", source = "entity.montoTotal")
    @Mapping(target = "solicitadoPorNombre", expression = "java(nombreCompleto(entity.getSolicitadoPor()))")
    @Mapping(target = "fechaSolicitud", source = "entity.fechaSolicitud")
    @Mapping(target = "resueltoPorNombre", expression = "java(nombreCompleto(entity.getAprobadoPor()))")
    @Mapping(target = "fechaResolucion", source = "entity.fechaResolucion")
    @Mapping(target = "observacionAdmin", source = "entity.observacionAdmin")
    @Mapping(target = "detalles", source = "detalles")
    @Mapping(target = "reembolsoId", expression = "java(reembolso == null ? null : reembolso.getId())")
    @Mapping(target = "reembolsoEstado", expression = "java(reembolso == null ? null : reembolso.getEstado())")
    DevolucionResponseDTO toResponseDto(Devolucion entity, List<DetalleDevolucionResponseDTO> detalles, Reembolso reembolso);

    @Mapping(target = "ventaId", source = "venta.id")
    @Mapping(target = "ventaSerie", source = "venta.serieCorrelativo")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    DevolucionListadoDTO toListadoDto(Devolucion entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }
}
