package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.RetiroSocioRequestDTO;
import com.policalum.erp.application.dto.response.RetiroSocioListadoDTO;
import com.policalum.erp.application.dto.response.RetiroSocioResponseDTO;
import com.policalum.erp.domain.model.FinanzasRetiroSocio;
import com.policalum.erp.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface RetiroSocioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "turnoCaja", ignore = true)
    @Mapping(target = "fechaRetiro", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "motivoAnulacion", ignore = true)
    @Mapping(target = "anuladoPor", ignore = true)
    @Mapping(target = "fechaAnulacion", ignore = true)
    @Mapping(target = "impactoCajaRevertido", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FinanzasRetiroSocio toEntity(RetiroSocioRequestDTO dto);

    @Mapping(target = "adminId", source = "admin.id")
    @Mapping(target = "adminNombre", expression = "java(nombreCompleto(entity.getAdmin()))")
    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "turnoCajaId", source = "turnoCaja.id")
    @Mapping(target = "anuladoPorId", source = "anuladoPor.id")
    @Mapping(target = "anuladoPorNombre", expression = "java(nombreCompleto(entity.getAnuladoPor()))")
    RetiroSocioResponseDTO toResponseDto(FinanzasRetiroSocio entity);

    @Mapping(target = "adminNombre", expression = "java(nombreCompleto(entity.getAdmin()))")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    RetiroSocioListadoDTO toListadoDto(FinanzasRetiroSocio entity);

    /**
     * Por convención del proyecto. Un retiro no se edita (se anula y se registra otro),
     * por eso el servicio no expone update; solo copia la descripción.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "admin", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "turnoCaja", ignore = true)
    @Mapping(target = "montoRetirado", ignore = true)
    @Mapping(target = "origenFondos", ignore = true)
    @Mapping(target = "fechaRetiro", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "motivoAnulacion", ignore = true)
    @Mapping(target = "anuladoPor", ignore = true)
    @Mapping(target = "fechaAnulacion", ignore = true)
    @Mapping(target = "impactoCajaRevertido", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(RetiroSocioRequestDTO dto, @MappingTarget FinanzasRetiroSocio entity);

    default String nombreCompleto(User user) {
        if (user == null) return null;
        return user.getNombre() + " " + user.getApellido();
    }
}
