package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.dto.response.SedeResponseDTO;
import com.policalum.erp.application.mapper.SedeMapper;
import com.policalum.erp.application.service.SedeService;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SedeServiceImpl implements SedeService {

    private final SedeRepository sedeRepository;
    private final SedeMapper sedeMapper;

    @Override
    @Transactional
    public SedeResponseDTO crearSede(SedeRequestDTO request) {
        Sede sede = sedeMapper.toEntity(request);

        // Valores por defecto preventivos si vienen nulos en el DTO
        if (sede.getPais() == null) sede.setPais("Perú");
        if (sede.getMoneda() == null) sede.setMoneda("PEN");
        if (sede.getActiva() == null) sede.setActiva(true);

        Sede guardada = sedeRepository.save(sede);
        return sedeMapper.toResponseDto(guardada);
    }

    @Override
    public Page<SedeResponseDTO> listarTodas(Pageable pageable) {
        return sedeRepository.findAll(pageable)
                .map(sedeMapper::toResponseDto);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "sedes", key = "#activa + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    public Page<SedeResponseDTO> listarPorEstado(Boolean activa, Pageable pageable) {
        return sedeRepository.findByActiva(activa, pageable)
                .map(sedeMapper::toResponseDto);
    }

    @Override
    public SedeResponseDTO buscarPorId(UUID id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        return sedeMapper.toResponseDto(sede);
    }

    @Override
    @Transactional
    public SedeResponseDTO actualizarSede(UUID id, SedeRequestDTO request) {
        Sede sedeExistente = sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se puede actualizar. Sede no encontrada con ID: " + id));

        // Actualización in-place usando el Mapper con @MappingTarget
        sedeMapper.updateEntityFromDto(request, sedeExistente);

        Sede actualizada = sedeRepository.save(sedeExistente);
        return sedeMapper.toResponseDto(actualizada);
    }

    @Override
    @Transactional
    public SedeResponseDTO eliminarSede(UUID id) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se puede eliminar. Sede no encontrada con ID: " + id));
        if (sedeRepository.tieneHistorial(id)) {
            if (Boolean.FALSE.equals(sede.getActiva())) {
                throw new ValidationException("La sede tiene historial y permanece inactiva");
            }
            sede.setActiva(false);
            return sedeMapper.toResponseDto(sedeRepository.save(sede));
        }
        sedeRepository.delete(sede);
        return null;
    }
}
