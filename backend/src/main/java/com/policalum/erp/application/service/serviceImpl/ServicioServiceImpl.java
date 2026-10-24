package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.ServicioColaboradorRequestDTO;
import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.dto.response.ServicioColaboradorResponseDTO;
import com.policalum.erp.application.dto.response.ServicioResponseDTO;
import com.policalum.erp.application.mapper.ServicioMapper;
import com.policalum.erp.application.service.ServicioService;
import com.policalum.erp.domain.model.Servicio;
import com.policalum.erp.domain.model.ServicioColaborador;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ServicioRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.validation.ValidationException;
import org.springframework.transaction.annotation.Transactional; //
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class ServicioServiceImpl implements ServicioService {

    private static final Set<String> PARTICIPACIONES = Set.of("INSTALO", "AYUDO");

    private final ServicioRepository servicioRepository;
    private final ServicioMapper servicioMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ServicioResponseDTO create(ServicioRequestDTO dto) {
        if (!isNombreDisponible(dto.nombre(), null)) {
            throw new ValidationException("Ya existe un servicio registrado con el nombre: " + dto.nombre());
        }

        Servicio servicio = servicioMapper.toEntity(dto);
        aplicarColaboradores(servicio, dto.colaboradores());
        Servicio guardado = servicioRepository.save(servicio);
        return responder(guardado);
    }

    @Override
    @Transactional
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
        aplicarColaboradores(servicio, dto.colaboradores());

        Servicio actualizado = servicioRepository.save(servicio);
        return responder(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ServicioResponseDTO findById(UUID id) {
        return servicioRepository.findById(id)
                .map(this::responder)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con el ID: " + id));
    }



    @Override
    @Transactional(readOnly = true)
    public Page<ServicioResponseDTO> search(String searchTerm, Boolean activo, BigDecimal precioMin, BigDecimal precioMax, Pageable pageable) {
        String termino = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();
        return servicioRepository.search(termino, activo, precioMin, precioMax, pageable)
                .map(this::responder);
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

    private void aplicarColaboradores(Servicio servicio, List<ServicioColaboradorRequestDTO> pedidos) {
        if (pedidos == null) {
            return;
        }
        if (servicio.getColaboradores() == null) {
            servicio.setColaboradores(new ArrayList<>());
        }
        servicio.getColaboradores().clear();
        Set<UUID> vistos = new HashSet<>();
        for (ServicioColaboradorRequestDTO pedido : pedidos) {
            if (pedido == null || pedido.userId() == null) {
                throw new ValidationException("Cada persona del servicio debe ser un usuario existente");
            }
            if (!vistos.add(pedido.userId())) {
                throw new ValidationException("La misma persona está repetida en el equipo del servicio");
            }
            User usuario = userRepository.findById(pedido.userId())
                    .orElseThrow(() -> new ValidationException("No existe el usuario indicado para instalar o ayudar en el servicio"));
            ServicioColaborador colaborador = ServicioColaborador.builder()
                    .usuario(usuario)
                    .participacion(normalizarParticipacion(pedido.participacion()))
                    .build();
            colaborador.setServicio(servicio);
            servicio.getColaboradores().add(colaborador);
        }
    }

    private String normalizarParticipacion(String participacion) {
        String valor = participacion == null || participacion.isBlank() ? "AYUDO" : participacion.trim().toUpperCase();
        if (!PARTICIPACIONES.contains(valor)) {
            throw new ValidationException("La participación debe ser INSTALO o AYUDO");
        }
        return valor;
    }

    private ServicioResponseDTO responder(Servicio servicio) {
        List<ServicioColaboradorResponseDTO> personas = new ArrayList<>();
        if (servicio.getColaboradores() != null) {
            for (ServicioColaborador colaborador : servicio.getColaboradores()) {
                User usuario = colaborador.getUsuario();
                personas.add(new ServicioColaboradorResponseDTO(
                        usuario != null ? usuario.getId() : null,
                        usuario != null ? usuario.getNombre() : null,
                        usuario != null ? usuario.getApellido() : null,
                        usuario != null ? usuario.getEmail() : null,
                        colaborador.getParticipacion()));
            }
        }
        ServicioResponseDTO base = servicioMapper.toResponseDto(servicio);
        return new ServicioResponseDTO(
                base.id(),
                base.nombre(),
                base.descripcion(),
                base.precio(),
                base.activo(),
                base.createdAt(),
                personas);
    }
}
