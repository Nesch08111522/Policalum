package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.CrearDespachoRequestDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.domain.enums.EstadoDespacho;
import com.policalum.erp.domain.model.OrdenDespacho;
import com.policalum.erp.domain.model.User;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Relaciones, estado, código y datos de entrega los asigna OrdenDespachoServiceImpl.
 * No hay updateEntityFromDto: la orden no se edita libremente, cambia por transiciones de estado.
 */
@Mapper(componentModel = "spring")
public interface OrdenDespachoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "venta", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "responsable", ignore = true)
    @Mapping(target = "movilidad", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaEntrega", ignore = true)
    @Mapping(target = "urlEvidenciaEntrega", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "codigoSeguimiento", ignore = true)
    @Mapping(target = "fechaAsignacion", ignore = true)
    @Mapping(target = "fechaSalida", ignore = true)
    @Mapping(target = "recibidoPorNombre", ignore = true)
    @Mapping(target = "recibidoPorDocumento", ignore = true)
    @Mapping(target = "latitudEntrega", ignore = true)
    @Mapping(target = "longitudEntrega", ignore = true)
    @Mapping(target = "evidenciaContentType", ignore = true)
    @Mapping(target = "evidenciaNombreOriginal", ignore = true)
    @Mapping(target = "motivoFallo", ignore = true)
    @Mapping(target = "intentosFallidos", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrdenDespacho toEntity(CrearDespachoRequestDTO dto);

    @Mapping(target = "ventaId", source = "venta.id")
    @Mapping(target = "ventaSerie", source = "venta.serieCorrelativo")
    @Mapping(target = "clienteNombre", source = "venta.cliente.nombre")
    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "atrasado", expression = "java(estaAtrasado(entity))")
    @Mapping(target = "responsableId", source = "responsable.id")
    @Mapping(target = "responsableNombre", expression = "java(nombreCompleto(entity.getResponsable()))")
    @Mapping(target = "movilidadId", source = "movilidad.id")
    @Mapping(target = "movilidadPlaca", source = "movilidad.placa")
    @Mapping(target = "tieneEvidencia", expression = "java(entity.getUrlEvidenciaEntrega() != null)")
    OrdenDespachoResponseDTO toResponseDto(OrdenDespacho entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }

    /** PENDIENTE o EN_CAMINO con fecha programada ya pasada. */
    default boolean estaAtrasado(OrdenDespacho o) {
        return o.getFechaProgramada() != null && o.getFechaProgramada().isBefore(LocalDate.now())
                && (o.getEstado() == EstadoDespacho.PENDIENTE || o.getEstado() == EstadoDespacho.EN_CAMINO);
    }
}
