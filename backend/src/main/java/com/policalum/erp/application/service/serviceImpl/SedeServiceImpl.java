package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.dto.response.SedeMapaDTO;
import com.policalum.erp.application.dto.response.SedeResponseDTO;
import com.policalum.erp.application.mapper.SedeMapper;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.SedeService;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.geo.PeruGeocoder;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SedeServiceImpl implements SedeService {

    private final SedeRepository sedeRepository;
    private final SedeMapper sedeMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    @CacheEvict(value = "sedes", allEntries = true)
    public SedeResponseDTO crearSede(SedeRequestDTO request) {
        Sede sede = sedeMapper.toEntity(request);

        if (sede.getPais() == null) sede.setPais("Perú");
        if (sede.getMoneda() == null) sede.setMoneda("PEN");
        if (sede.getActiva() == null) sede.setActiva(true);
        if (request.imagen() != null && !request.imagen().isBlank()) sede.setImagen(request.imagen().trim());
        completarUbicacion(sede);

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
    @CacheEvict(value = "sedes", allEntries = true)
    public SedeResponseDTO actualizarSede(UUID id, SedeRequestDTO request) {
        Sede sedeExistente = sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No se puede actualizar. Sede no encontrada con ID: " + id));

        sedeMapper.updateEntityFromDto(request, sedeExistente);
        if (request.imagen() != null && !request.imagen().isBlank()) sedeExistente.setImagen(request.imagen().trim());
        completarUbicacion(sedeExistente);

        Sede actualizada = sedeRepository.save(sedeExistente);
        return sedeMapper.toResponseDto(actualizada);
    }

    @Override
    @Transactional
    public List<SedeMapaDTO> mapa() {
        return sedeRepository.findByActiva(true, PageRequest.of(0, 500, Sort.by("nombre"))).getContent().stream()
                .map(this::aMapa)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "sedes", allEntries = true)
    public SedeResponseDTO guardarImagen(UUID id, MultipartFile archivo) {
        Sede sede = sedeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + id));
        String nombre = archivo == null || archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!(nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") || nombre.endsWith(".png") || nombre.endsWith(".webp"))) {
            throw new ValidationException("La imagen de la sede debe ser JPG, PNG o WEBP");
        }
        String anterior = sede.getImagen();
        String ruta = fileStorageService.guardar(archivo, "sedes");
        sede.setImagen(ruta);
        if (anterior != null && anterior.startsWith("sedes/") && !anterior.contains("..")) {
            fileStorageService.eliminar(anterior);
        }
        return sedeMapper.toResponseDto(sedeRepository.save(sede));
    }

    @Override
    @Transactional
    @CacheEvict(value = "sedes", allEntries = true)
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

    private void completarUbicacion(Sede sede) {
        if (sede.getLatitud() != null && sede.getLongitud() != null) return;
        double[] punto = PeruGeocoder.buscar(sede.getCiudad(), sede.getDireccion(), sede.getNombre());
        if (punto == null) return;
        if (sede.getLatitud() == null) sede.setLatitud(punto[0]);
        if (sede.getLongitud() == null) sede.setLongitud(punto[1]);
    }

    private SedeMapaDTO aMapa(Sede sede) {
        Double latitud = sede.getLatitud();
        Double longitud = sede.getLongitud();
        if (latitud == null || longitud == null) {
            double[] punto = PeruGeocoder.buscar(sede.getCiudad(), sede.getDireccion(), sede.getNombre());
            if (punto != null) {
                if (latitud == null) latitud = punto[0];
                if (longitud == null) longitud = punto[1];
            }
        }
        return new SedeMapaDTO(sede.getId(), sede.getNombre(), sede.getCiudad(), sede.getDireccion(),
                latitud, longitud, sede.getImagen(), latitud != null && longitud != null);
    }
}
