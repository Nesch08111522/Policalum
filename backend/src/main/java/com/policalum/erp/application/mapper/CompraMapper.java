package com.policalum.erp.application.mapper;

import com.policalum.erp.application.dto.response.CompraAdjuntoResponseDTO;
import com.policalum.erp.application.dto.response.CompraListadoDTO;
import com.policalum.erp.application.dto.response.CompraResponseDTO;
import com.policalum.erp.application.dto.response.DetalleCompraResponseDTO;
import com.policalum.erp.domain.model.Compra;
import com.policalum.erp.domain.model.CompraAdjunto;
import com.policalum.erp.domain.model.DetalleCompra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompraMapper {

    @Mapping(target = "proveedorId", source = "proveedor.id")
    @Mapping(target = "proveedorNombre", source = "proveedor.nombreRazon")
    @Mapping(target = "proveedorDocumento", source = "proveedor.numeroDocumento")
    @Mapping(target = "sedeId", source = "sede.id")
    @Mapping(target = "sedeNombre", source = "sede.nombre")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userNombre", expression = "java(compra.getUser().getNombre() + \" \" + compra.getUser().getApellido())")
    @Mapping(target = "detalles", source = "detalles")
    @Mapping(target = "adjuntos", source = "adjuntos")
    CompraResponseDTO toResponseDto(Compra compra);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoSku", source = "producto.codigoSku")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "fraccionId", source = "fraccion.id")
    @Mapping(target = "fraccionNombre", source = "fraccion.nombreFraccion")
    @Mapping(target = "factorFraccion", source = "fraccion.factorDescuentoKardex")
    @Mapping(target = "almacenId", source = "almacen.id")
    @Mapping(target = "almacenNombre", source = "almacen.nombre")
    DetalleCompraResponseDTO toDetalleResponseDto(DetalleCompra detalle);

    List<DetalleCompraResponseDTO> toDetalleResponseList(List<DetalleCompra> detalles);

    @Mapping(target = "subidoPorId", source = "subidoPor.id")
    @Mapping(target = "subidoPorNombre", expression = "java(adjunto.getSubidoPor().getNombre() + \" \" + adjunto.getSubidoPor().getApellido())")
    CompraAdjuntoResponseDTO toAdjuntoResponseDto(CompraAdjunto adjunto);

    List<CompraAdjuntoResponseDTO> toAdjuntoResponseList(List<CompraAdjunto> adjuntos);

    @Mapping(target = "proveedorNombre", source = "proveedor.nombreRazon")
    CompraListadoDTO toListadoDto(Compra compra);
}