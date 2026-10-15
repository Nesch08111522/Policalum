package com.policalum.erp.application.service.serviceImpl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.response.CatalogoFraccionResponseDTO;
import com.policalum.erp.application.mapper.CatalogoFraccionMapper;
import com.policalum.erp.application.service.CatalogoFraccionService;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CatalogoFraccionRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogoFraccionServiceImpl implements CatalogoFraccionService {

    private static final String ENTITY_NAME = "Fracción de catálogo";

    private final CatalogoFraccionRepository catalogoFraccionRepository;
    private final CatalogoFraccionMapper catalogoFraccionMapper;
    private final ProductoRepository productoRepository;

    @Override
    public CatalogoFraccionResponseDTO create(CatalogoFraccionRequestDTO requestDTO) {
        Producto producto = validateRequest(requestDTO, null);
        CatalogoFraccion fraccion = catalogoFraccionMapper.toEntity(requestDTO);
        fraccion.setProducto(producto);
        normalize(fraccion);
        return catalogoFraccionMapper.toResponseDto(catalogoFraccionRepository.save(fraccion));
    }

    @Override
    public CatalogoFraccionResponseDTO update(UUID id, CatalogoFraccionRequestDTO requestDTO) {
        findEntityById(id);
        Producto producto = validateRequest(requestDTO, id);
        CatalogoFraccion fraccion = catalogoFraccionRepository.findWithProductoById(id).orElseThrow();
        catalogoFraccionMapper.updateEntityFromDto(requestDTO, fraccion);
        fraccion.setProducto(producto);
        normalize(fraccion);
        return catalogoFraccionMapper.toResponseDto(catalogoFraccionRepository.save(fraccion));
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoFraccionResponseDTO findById(UUID id) {
        return catalogoFraccionMapper.toResponseDto(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CatalogoFraccionResponseDTO> search(UUID productoId, Boolean activo,
            String searchTerm, Pageable pageable) {
        String normalizedSearchTerm = searchTerm == null || searchTerm.isBlank()
                ? null
                : searchTerm.trim();
        return catalogoFraccionRepository.search(productoId, activo, normalizedSearchTerm, pageable)
                .map(catalogoFraccionMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoFraccionResponseDTO> findAvailableByProducto(UUID productoId) {
        return catalogoFraccionRepository.findByProducto_IdAndActivoTrueOrderByFactorDescuentoKardexDesc(productoId)
                .stream()
                .map(catalogoFraccionMapper::toResponseDto)
                .toList();
    }

    @Override
    public void delete(UUID id) {
        catalogoFraccionRepository.delete(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFactorDisponible(UUID productoId, BigDecimal factor, UUID excludeId) {
        return catalogoFraccionRepository.findByProducto_IdAndFactorDescuentoKardex(productoId, factor)
                .map(fraccion -> excludeId != null && fraccion.getId().equals(excludeId))
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalFracciones", catalogoFraccionRepository.count());
        statistics.put("fraccionesActivas", catalogoFraccionRepository.countByActivo(true));
        statistics.put("fraccionesInactivas", catalogoFraccionRepository.countByActivo(false));
        return statistics;
    }

    private CatalogoFraccion findEntityById(UUID id) {
        return catalogoFraccionRepository.findWithProductoById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrada con ID: %s", ENTITY_NAME, id)));
    }

    private Producto validateRequest(CatalogoFraccionRequestDTO requestDTO, UUID excludeId) {
        Producto producto = productoRepository.findById(requestDTO.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("El producto indicado no existe"));
        if (!productoRepository.existsByIdAndEsFraccionableTrue(producto.getId())) {
            throw new ValidationException("Solo se pueden registrar fracciones para productos fraccionables");
        }
        BigDecimal factor = requestDTO.factorDescuentoKardex();
        if (factor.compareTo(BigDecimal.ZERO) <= 0 || factor.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("El factor de la fracción debe ser mayor que 0 y menor o igual que 1");
        }
        if (!isFactorDisponible(producto.getId(), factor, excludeId)) {
            throw new ValidationException("Ya existe una fracción con ese factor para el producto indicado");
        }
        return producto;
    }

    private void normalize(CatalogoFraccion fraccion) {
        fraccion.setNombreFraccion(fraccion.getNombreFraccion().trim());
        fraccion.setFactorDescuentoKardex(fraccion.getFactorDescuentoKardex().stripTrailingZeros());
    }
}
