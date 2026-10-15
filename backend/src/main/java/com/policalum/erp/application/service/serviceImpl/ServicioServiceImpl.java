package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.dto.response.ServicioResponseDTO;
import com.policalum.erp.application.mapper.ServicioMapper;
import com.policalum.erp.application.service.ServicioService;
import com.policalum.erp.domain.model.Servicio;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ServicioRepository;
import jakarta.validation.ValidationException;
import org.springframework.transaction.annotation.Transactional; //
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository servicioRepository;
    private final ServicioMapper servicioMapper;

    @Override
    public ServicioResponseDTO create(ServicioRequestDTO dto) {
        if (!isNombreDisponible(dto.nombre(), null)) {
            throw new ValidationException("Ya existe un servicio registrado con el nombre: " + dto.nombre());
        }

        Servicio servicio = servicioMapper.toEntity(dto);
        Servicio guardado = servicioRepository.save(servicio);
        return servicioMapper.toResponseDto(guardado);
    }

    @Override
    public ServicioResponseDTO update(UUID id, ServicioRequestDTO dto) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con el ID: " + id));

        if (!isNombreDisponible(dto.nombre(), id)) {
            throw new ValidationException("Ya existe otro servicio con el nombre: " + dto.nombre());
        }

        servicio.setNombre(dto.nombre());
        servicio.setDescripcion(dto.descripcion());
        servicio.setPrecio(dto.precio());
        servicio.setActivo(dto.activo());

        Servicio actualizado = servicioRepository.save(servicio);
        return servicioMapper.toResponseDto(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicioResponseDTO findById(UUID id) {
        return servicioRepository.findById(id)
                .map(servicioMapper::toResponseDto)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con el ID: " + id));
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ServicioResponseDTO> search(String searchTerm, Boolean activo, BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        return servicioRepository.search(searchTerm, activo, precioMin, precioMax, pageable)
                .map(servicioMapper::toResponseDto);
    }

    @Override
    public void delete(UUID id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con el ID: " + id));

        // Estrategia: Borrado lógico por defecto para no romper historial de ventas pasadas
        servicio.setActivo(false);
        servicioRepository.save(servicio);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNombreDisponible(String nombre, UUID excludeId) {
        if (nombre == null || nombre.isBlank()) return true;

        if (excludeId == null) {
            return !servicioRepository.existsByNombreIgnoreCase(nombre.trim());
        }
        return !servicioRepository.existsByNombreIgnoreCaseAndIdNot(nombre.trim(), excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        long totalServicios = servicioRepository.count();
        long serviciosActivos = servicioRepository.countByActivoTrue();

        return Map.of(
                "totalServicios", totalServicios,
                "serviciosActivos", serviciosActivos,
                "serviciosInactivos", totalServicios - serviciosActivos
        );
    }
}
