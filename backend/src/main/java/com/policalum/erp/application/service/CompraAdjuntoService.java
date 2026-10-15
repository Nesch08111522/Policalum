package com.policalum.erp.application.service;

import com.policalum.erp.application.dto.response.CompraAdjuntoResponseDTO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CompraAdjuntoService {

    CompraAdjuntoResponseDTO subir(UUID compraId, MultipartFile archivo, String tipo);

    List<CompraAdjuntoResponseDTO> listar(UUID compraId);

    Resource descargar(UUID compraId, UUID adjuntoId);

    void eliminar(UUID compraId, UUID adjuntoId);
}