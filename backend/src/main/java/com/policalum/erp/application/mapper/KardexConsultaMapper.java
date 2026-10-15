package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.KardexMovimientoDetalleDTO;
import com.policalum.erp.application.dto.response.MermaKardexDTO;
import com.policalum.erp.application.dto.response.SaldoKardexDTO;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.model.KardexMovimiento;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.projection.SaldoProductoAlmacenProjection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Solo salida (Módulo 10): los movimientos los crea KardexServiceImpl.registrarMovimiento y
 * no se editan, por eso no hay toEntity/updateEntityFromDto.
 */
@Mapper(componentModel = "spring")
public interface KardexConsultaMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "codigoSku", source = "producto.codigoSku")
    @Mapping(target = "unidad", source = "producto.unidad")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "sedeId", source = "almacen.sede.id")
    @Mapping(target = "sedeNombre", source = "almacen.sede.nombre")
    @Mapping(target = "stockAnterior", expression = "java(stockAnterior(entity))")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userNombre", expression = "java(nombreCompleto(entity.getUser()))")
    KardexMovimientoDetalleDTO toDetalleDto(KardexMovimiento entity);

    @Mapping(target = "movimientoId", source = "id")
    @Mapping(target = "fecha", source = "createdAt")
    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "codigoSku", source = "producto.codigoSku")
    @Mapping(target = "unidad", source = "producto.unidad")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    @Mapping(target = "cantidad", source = "cantidadMovida")
    @Mapping(target = "costoUnitario", source = "producto.precioCompra")
    @Mapping(target = "valor", expression = "java(valor(entity))")
    @Mapping(target = "userNombre", expression = "java(nombreCompleto(entity.getUser()))")
    MermaKardexDTO toMermaDto(KardexMovimiento entity);

    @Mapping(target = "stockMinimo", expression = "java(cero(p.getStockMinimo()))")
    @Mapping(target = "cantidad", expression = "java(cero(p.getCantidad()))")
    @Mapping(target = "bajoMinimo", expression = "java(bajoMinimo(p))")
    SaldoKardexDTO toSaldoDto(SaldoProductoAlmacenProjection p);

    default BigDecimal stockAnterior(KardexMovimiento k) {
        if (k.getStockResultante() == null || k.getCantidadMovida() == null) return null;
        return k.getTipoMovimiento() == TipoMovimiento.ENTRADA
                ? k.getStockResultante().subtract(k.getCantidadMovida())
                : k.getStockResultante().add(k.getCantidadMovida());
    }

    default BigDecimal valor(KardexMovimiento k) {
        BigDecimal costo = k.getProducto() != null && k.getProducto().getPrecioCompra() != null
                ? k.getProducto().getPrecioCompra() : BigDecimal.ZERO;
        BigDecimal cantidad = k.getCantidadMovida() != null ? k.getCantidadMovida() : BigDecimal.ZERO;
        return cantidad.multiply(costo).setScale(2, RoundingMode.HALF_UP);
    }

    default boolean bajoMinimo(SaldoProductoAlmacenProjection p) {
        BigDecimal minimo = cero(p.getStockMinimo());
        return minimo.signum() > 0 && cero(p.getCantidad()).compareTo(minimo) <= 0;
    }

    default BigDecimal cero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    default String nombreCompleto(User u) {
        if (u == null) return null;
        return ((u.getNombre() != null ? u.getNombre() : "") + " " + (u.getApellido() != null ? u.getApellido() : "")).trim();
    }
}
