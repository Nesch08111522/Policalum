package com.policalum.erp.application.service.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.AlmacenRequestDTO;
import com.policalum.erp.application.dto.response.AlmacenResponseDTO;
import com.policalum.erp.application.mapper.AlmacenMapper;
import com.policalum.erp.application.service.AlmacenService;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AlmacenServiceImpl implements AlmacenService {

    private static final String ENTITY_NAME = "Almacén";

    private final AlmacenRepository almacenRepository;
    private final AlmacenMapper almacenMapper;

    @Override
    public AlmacenResponseDTO create(AlmacenRequestDTO requestDTO) {
        validateNombreAvailability(requestDTO.sede().getId(), requestDTO.nombre(), null);
        Almacen almacen = almacenMapper.toEntity(requestDTO);
        almacen.setNombre(requestDTO.nombre().trim());
        return almacenMapper.toResponseDto(almacenRepository.save(almacen));
    }

    @Override
    public AlmacenResponseDTO update(UUID id, AlmacenRequestDTO requestDTO) {
        Almacen almacen = findEntityById(id);
        validateNombreAvailability(requestDTO.sede().getId(), requestDTO.nombre(), id);
        almacenMapper.updateEntityFromDto(requestDTO, almacen);
        almacen.setNombre(requestDTO.nombre().trim());
        return almacenMapper.toResponseDto(almacenRepository.save(almacen));
    }

    @Override
    @Transactional(readOnly = true)
    public AlmacenResponseDTO findById(UUID id) {
        return almacenMapper.toResponseDto(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlmacenResponseDTO> search(String searchTerm, UUID sedeId, Boolean activo, Pageable pageable) {
        String normalizedSearchTerm = searchTerm == null || searchTerm.isBlank()
                ? null
                : searchTerm.trim();
        return almacenRepository.search(normalizedSearchTerm, sedeId, activo, pageable)
                .map(almacenMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlmacenResponseDTO> findBySede(UUID sedeId) {
        return almacenRepository.findBySede_IdOrderByNombreAsc(sedeId).stream()
                .map(almacenMapper::toResponseDto)
                .toList();
    }

    @Override
    public AlmacenResponseDTO delete(UUID id) {
        Almacen almacen = findEntityById(id);
        if (almacenRepository.tieneHistorial(id)) {
            if (Boolean.FALSE.equals(almacen.getActivo())) {
                throw new ValidationException("El almacén tiene historial de inventario y permanece inactivo");
            }
            almacen.setActivo(false);
            return almacenMapper.toResponseDto(almacenRepository.save(almacen));
        }
        almacenRepository.delete(almacen);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNombreDisponible(UUID sedeId, String nombre, UUID excludeId) {
        return almacenRepository.findBySede_IdAndNombreIgnoreCase(sedeId, nombre.trim())
                .map(almacen -> excludeId != null && almacen.getId().equals(excludeId))
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAlmacenes", almacenRepository.count());
        statistics.put("almacenesActivos", almacenRepository.countByActivo(true));
        statistics.put("almacenesInactivos", almacenRepository.countByActivo(false));
        return statistics;
    }

    private Almacen findEntityById(UUID id) {
        return almacenRepository.findWithSedeById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrado con ID: %s", ENTITY_NAME, id)));
    }

    private void validateNombreAvailability(UUID sedeId, String nombre, UUID excludeId) {
        if (!isNombreDisponible(sedeId, nombre, excludeId)) {
            throw new ValidationException("Ya existe un almacén con ese nombre en la sede indicada");
        }
    }
}
