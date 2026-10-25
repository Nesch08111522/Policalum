package com.policalum.erp.application.service.serviceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.CategoriaRequestDTO;
import com.policalum.erp.application.dto.response.CategoriaResponseDTO;
import com.policalum.erp.application.mapper.CategoriaMapper;
import com.policalum.erp.application.service.CategoriaService;
import com.policalum.erp.domain.model.Categoria;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaServiceImpl implements CategoriaService {

    private static final String ENTITY_NAME = "Categoría";

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "categorias", allEntries = true)
    public CategoriaResponseDTO create(CategoriaRequestDTO requestDTO) {
        validateSlugAvailability(requestDTO.slug(), null);
        Categoria categoria = categoriaMapper.toEntity(requestDTO);
        resolverParent(categoria, requestDTO);
        normalize(categoria);
        return categoriaMapper.toResponseDto(categoriaRepository.save(categoria));
    }

    @Override
    public CategoriaResponseDTO update(UUID id, CategoriaRequestDTO requestDTO) {
        Categoria categoria = findEntityById(id);
        validateSlugAvailability(requestDTO.slug(), id);
        validateParent(id, requestDTO.parent());
        categoriaMapper.updateEntityFromDto(requestDTO, categoria);
        resolverParent(categoria, requestDTO);
        normalize(categoria);
        return categoriaMapper.toResponseDto(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponseDTO findById(UUID id) {
        return categoriaMapper.toResponseDto(findEntityById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoriaResponseDTO> search(String searchTerm, Boolean activa, Pageable pageable) {
        String normalizedSearchTerm = searchTerm == null || searchTerm.isBlank()
                ? null
                : searchTerm.trim();
        return categoriaRepository.search(normalizedSearchTerm, activa, pageable)
                .map(categoriaMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable("categorias")
    public List<CategoriaResponseDTO> findRoots() {
        return categoriaRepository.findByParentIsNullOrderByNombreAsc().stream()
                .map(categoriaMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> findChildren(UUID parentId) {
        findEntityById(parentId);
        return categoriaRepository.findByParent_IdOrderByNombreAsc(parentId).stream()
                .map(categoriaMapper::toResponseDto)
                .toList();
    }

    @Override
    public CategoriaResponseDTO delete(UUID id) {
        Categoria categoria = findEntityById(id);
        if (categoriaRepository.existsByParent_Id(id) || categoriaRepository.tieneProductos(id)) {
            if (Boolean.FALSE.equals(categoria.getActiva())) {
                throw new ValidationException("La categoría tiene productos o subcategorías y permanece inactiva");
            }
            categoria.setActiva(false);
            return categoriaMapper.toResponseDto(categoriaRepository.save(categoria));
        }
        categoriaRepository.delete(categoria);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSlugDisponible(String slug, UUID excludeId) {
        return categoriaRepository.findBySlugIgnoreCase(slug.trim())
                .map(categoria -> excludeId != null && categoria.getId().equals(excludeId))
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCategorias", categoriaRepository.count());
        statistics.put("categoriasActivas", categoriaRepository.countByActiva(true));
        statistics.put("categoriasInactivas", categoriaRepository.countByActiva(false));
        return statistics;
    }

    private Categoria findEntityById(UUID id) {
        return categoriaRepository.findWithParentById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrada con ID: %s", ENTITY_NAME, id)));
    }

    private void validateSlugAvailability(String slug, UUID excludeId) {
        if (!isSlugDisponible(slug, excludeId)) {
            throw new ValidationException("Ya existe una categoría con el slug indicado");
        }
    }

    private void validateParent(UUID categoryId, Categoria parent) {
        if (parent == null || parent.getId() == null) {
            return;
        }
        if (parent.getId().equals(categoryId)) {
            throw new ValidationException("Una categoría no puede ser su propio padre");
        }
        UUID currentId = parent.getId();
        while (currentId != null) {
            Categoria current = categoriaRepository.findById(currentId)
                    .orElseThrow(() -> new ResourceNotFoundException("La categoría padre no existe"));
            currentId = current.getParent() == null ? null : current.getParent().getId();
            if (categoryId.equals(currentId)) {
                throw new ValidationException("La categoría no puede quedar dentro de su propia descendencia");
            }
        }
    }

    private void resolverParent(Categoria categoria, CategoriaRequestDTO request) {
        Categoria pedido = request.parent();
        if (pedido == null || pedido.getId() == null) {
            categoria.setParent(null);
            return;
        }
        categoria.setParent(categoriaRepository.findById(pedido.getId())
                .orElseThrow(() -> new ResourceNotFoundException("La categoría padre no existe")));
    }

    private void normalize(Categoria categoria) {
        categoria.setNombre(categoria.getNombre().trim());
        categoria.setSlug(categoria.getSlug().trim().toLowerCase());
    }
}
