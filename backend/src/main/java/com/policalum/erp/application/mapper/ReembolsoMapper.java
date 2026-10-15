package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.ReembolsoResponseDTO;
import com.policalum.erp.domain.model.Reembolso;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Solo salida: el reembolso se arma en ReembolsoServiceImpl a partir de la devolución aprobada. */
@Mapper(componentModel = "spring")
public interface ReembolsoMapper {

    @Mapping(target = "devolucionId", source = "devolucion.id")
    @Mapping(target = "devolucionCodigo", source = "devolucion.codigo")
    @Mapping(target = "ventaId", source = "venta.id")
    @Mapping(target = "clienteNombre", source = "cliente.nombre")
    @Mapping(target = "creadoPorNombre", expression = "java(nombreCompleto(entity.getCreadoPor()))")
    @Mapping(target = "procesadoPorNombre", expression = "java(nombreCompleto(entity.getProcesadoPor()))")
    @Mapping(target = "turnoCajaId", source = "turnoCaja.id")
    ReembolsoResponseDTO toResponseDto(Reembolso entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }
}
