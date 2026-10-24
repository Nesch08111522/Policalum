package com.policalum.erp.application.service;

import java.util.List;
import java.util.UUID;

import com.policalum.erp.application.dto.request.ProductoImportacionFilaDTO;
import com.policalum.erp.application.dto.request.ProductoImportacionRequestDTO;
import com.policalum.erp.application.dto.response.ProductoImportacionResultadoDTO;
import com.policalum.erp.domain.enums.TipoProducto;

public interface ProductoImportacionService {

    /**
     * Crea o actualiza por SKU. Si una fila falla, la transacción revierte todo el lote.
     */
    ProductoImportacionResultadoDTO importar(ProductoImportacionRequestDTO request);

    List<ProductoImportacionFilaDTO> exportar(String searchTerm, UUID categoriaId, TipoProducto tipo,
                                              Boolean activo, Boolean esFraccionable);
}
