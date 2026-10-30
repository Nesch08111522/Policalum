package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.ProyectoPortafolioRequestDTO;
import com.policalum.erp.application.dto.response.ProyectoPortafolioResponseDTO;
import com.policalum.erp.domain.model.ProyectoPortafolio;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ProyectoPortafolioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortafolioService {

    private final ProyectoPortafolioRepository repository;

    @Transactional(readOnly = true)
    public List<ProyectoPortafolioResponseDTO> listar() {
        return repository.findAllByOrderByOrdenAscTituloAsc().stream().map(this::dto).toList();
    }

    @Transactional
    public ProyectoPortafolioResponseDTO crear(ProyectoPortafolioRequestDTO request) {
        ProyectoPortafolio proyecto = ProyectoPortafolio.builder()
                .titulo(request.titulo().trim())
                .descripcion(request.descripcion().trim())
                .imagenUrl(url(request.imagenUrl()))
                .orden(request.orden())
                .publicado(request.publicado())
                .build();
        return dto(repository.save(proyecto));
    }

    @Transactional
    public ProyectoPortafolioResponseDTO actualizar(UUID id, ProyectoPortafolioRequestDTO request) {
        ProyectoPortafolio proyecto = buscar(id);
        proyecto.setTitulo(request.titulo().trim());
        proyecto.setDescripcion(request.descripcion().trim());
        proyecto.setImagenUrl(url(request.imagenUrl()));
        proyecto.setOrden(request.orden());
        proyecto.setPublicado(request.publicado());
        return dto(proyecto);
    }

    @Transactional
    public void eliminar(UUID id) {
        repository.delete(buscar(id));
    }

    private ProyectoPortafolio buscar(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private ProyectoPortafolioResponseDTO dto(ProyectoPortafolio proyecto) {
        return new ProyectoPortafolioResponseDTO(proyecto.getId(), proyecto.getTitulo(), proyecto.getDescripcion(),
                proyecto.getImagenUrl(), proyecto.getOrden(), proyecto.getPublicado(), proyecto.getCreatedAt());
    }

    private static String url(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim();
        if (!limpio.startsWith("https://") && !limpio.startsWith("http://")) {
            throw new IllegalArgumentException("La imagen del proyecto debe ser una URL http(s)");
        }
        return limpio;
    }
}
