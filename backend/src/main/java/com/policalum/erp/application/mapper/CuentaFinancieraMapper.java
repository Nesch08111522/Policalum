package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.request.CuentaFinancieraRequestDTO;
import com.policalum.erp.application.dto.response.CuentaFinancieraResponseDTO;
import com.policalum.erp.domain.enums.MetodoPago;
import com.policalum.erp.domain.model.CuentaFinanciera;
import com.policalum.erp.domain.model.SaldoCuentaFinanciera;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** sede, métodos, activa y auditoría los asigna CuentaFinancieraServiceImpl. */
@Mapper(componentModel = "spring")
public interface CuentaFinancieraMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "metodosPago", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CuentaFinanciera toEntity(CuentaFinancieraRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sede", ignore = true)
    @Mapping(target = "metodosPago", ignore = true)
    @Mapping(target = "activa", ignore = true)
    @Mapping(target = "creadoPor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(CuentaFinancieraRequestDTO dto, @MappingTarget CuentaFinanciera entity);

    /** ultimo puede ser null (cuenta sin saldo registrado). */
    @Mapping(target = "id", source = "cuenta.id")
    @Mapping(target = "nombre", source = "cuenta.nombre")
    @Mapping(target = "tipo", source = "cuenta.tipo")
    @Mapping(target = "entidad", source = "cuenta.entidad")
    @Mapping(target = "ultimosDigitos", source = "cuenta.ultimosDigitos")
    @Mapping(target = "sedeId", source = "cuenta.sede.id")
    @Mapping(target = "sedeNombre", source = "cuenta.sede.nombre")
    @Mapping(target = "metodosPago", expression = "java(ordenados(cuenta.getMetodosPago()))")
    @Mapping(target = "activa", source = "cuenta.activa")
    @Mapping(target = "ultimoSaldo", source = "ultimo.saldo")
    @Mapping(target = "ultimoCorte", source = "ultimo.fechaCorte")
    @Mapping(target = "createdAt", source = "cuenta.createdAt")
    CuentaFinancieraResponseDTO toResponseDto(CuentaFinanciera cuenta, SaldoCuentaFinanciera ultimo);

    default List<MetodoPago> ordenados(Collection<MetodoPago> metodos) {
        if (metodos == null) return List.of();
        Set<MetodoPago> orden = new TreeSet<>(metodos);
        return List.copyOf(orden);
    }
}
