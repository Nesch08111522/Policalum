package com.policalum.erp.application.service;
import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.dto.response.SedeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SedeService {

    SedeResponseDTO crearSede(SedeRequestDTO request);

    Page<SedeResponseDTO> listarTodas(Pageable pageable);

    Page<SedeResponseDTO> listarPorEstado(Boolean activa, Pageable pageable);

    SedeResponseDTO buscarPorId(UUID id);

    SedeResponseDTO actualizarSede(UUID id, SedeRequestDTO request);

    void eliminarSede(UUID id);
}
