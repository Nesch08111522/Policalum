package com.policalum.erp.application.service.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.ProveedorRequestDTO;
import com.policalum.erp.application.dto.response.ProveedorResponseDTO;
import com.policalum.erp.application.mapper.ProveedorMapper;
import com.policalum.erp.application.service.ProveedorService;
import com.policalum.erp.domain.model.Proveedor;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ProveedorRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProveedorServiceImpl implements ProveedorService {

    private static final String ENTITY_NAME = "Proveedor";
    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    @Override
    public ProveedorResponseDTO create(ProveedorRequestDTO requestDTO) {
        validateDocumentAvailability(requestDTO.tipoDocumento(), requestDTO.numeroDocumento(), null);
        Proveedor proveedor = proveedorRepository.save(proveedorMapper.toEntity(requestDTO));
        log.info("Proveedor creado con ID: {}", proveedor.getId());
        return proveedorMapper.toResponseDto(proveedor);
    }

    @Override
    public ProveedorResponseDTO update(UUID id, ProveedorRequestDTO requestDTO) {
        Proveedor proveedor = findEntityById(id);
        validateDocumentAvailability(requestDTO.tipoDocumento(), requestDTO.numeroDocumento(), id);
        proveedorMapper.updateEntityFromDto(requestDTO, proveedor);
        return proveedorMapper.toResponseDto(proveedorRepository.save(proveedor));
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponseDTO findById(UUID id) {
        return proveedorMapper.toResponseDto(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ProveedorResponseDTO findByDocumento(String tipoDocumento, String numeroDocumento) {
        Proveedor proveedor = proveedorRepository.findByTipoDocumentoAndNumeroDocumento(
                tipoDocumento, numeroDocumento)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(
                        "%s no encontrado con documento %s %s", ENTITY_NAME, tipoDocumento, numeroDocumento)));
        return proveedorMapper.toResponseDto(proveedor);
    }

    @Override
    public void delete(UUID id) {
        proveedorRepository.delete(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> search(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll(pageable);
        }
        return proveedorRepository.searchByNombreOrDocumento(searchTerm.trim(), pageable)
                .map(proveedorMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> findAll(Pageable pageable) {
        return proveedorRepository.findAll(pageable).map(proveedorMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProveedorResponseDTO> findByActivo(Boolean activo, Pageable pageable) {
        return proveedorRepository.findByActivo(activo, pageable).map(proveedorMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProveedorResponseDTO> findWithIncompleteContactData() {
        return proveedorRepository.findWithIncompleteContactData().stream()
                .map(proveedorMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalProveedores", proveedorRepository.count());
        statistics.put("proveedoresActivos", proveedorRepository.countByActivo(true));
        statistics.put("proveedoresInactivos", proveedorRepository.countByActivo(false));
        statistics.put("contactosIncompletos", proveedorRepository.findWithIncompleteContactData().size());
        statistics.put("distribucionPorTipoDocumento", getStatisticsByDocumentType());
        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getStatisticsByDocumentType() {
        Map<String, Long> statistics = new HashMap<>();
        for (Object[] result : proveedorRepository.countGroupByTipoDocumento()) {
            statistics.put(result[0] == null ? "SIN_TIPO" : (String) result[0],
                    ((Number) result[1]).longValue());
        }
        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDocumentoDisponible(String tipoDocumento, String numeroDocumento, UUID excludeId) {
        return proveedorRepository.findByTipoDocumentoAndNumeroDocumento(tipoDocumento, numeroDocumento)
                .map(proveedor -> excludeId != null && proveedor.getId().equals(excludeId))
                .orElse(true);
    }

    private Proveedor findEntityById(UUID id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrado con ID: %s", ENTITY_NAME, id)));
    }

    private void validateDocumentAvailability(String tipoDocumento, String numeroDocumento, UUID excludeId) {
        if (!isDocumentoDisponible(tipoDocumento, numeroDocumento, excludeId)) {
            throw new ValidationException("Ya existe un proveedor con el tipo y número de documento indicados");
        }
    }
}