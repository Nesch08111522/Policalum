package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.GastoEmpresarialRequestDTO;
import com.policalum.erp.application.dto.response.GastoEmpresarialResponseDTO;
import com.policalum.erp.domain.model.GastoEmpresarial;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.GastoEmpresarialRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GastoEmpresarialServiceImpl {

    private final GastoEmpresarialRepository repository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;

    @Transactional
    public GastoEmpresarialResponseDTO registrar(GastoEmpresarialRequestDTO request) {
        Sede sede = null;
        if (request.sedeId() != null) {
            sede = sedeRepository.findById(request.sedeId()).orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        }
        GastoEmpresarial gasto = GastoEmpresarial.builder()
                .sede(sede)
                .tipo(request.tipo())
                .monto(request.monto())
                .fecha(request.fecha())
                .descripcion(request.descripcion() == null ? null : request.descripcion().trim())
                .anulado(false)
                .registradoPor(usuario())
                .build();
        return dto(repository.save(gasto));
    }

    @Transactional(readOnly = true)
    public Page<GastoEmpresarialResponseDTO> listar(UUID sedeId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        LocalDate inicio = desde == null ? LocalDate.now().minusYears(2) : desde;
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        return repository.buscar(sedeId, inicio, fin, pageable).map(this::dto);
    }

    @Transactional
    public void anular(UUID id) {
        GastoEmpresarial gasto = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Gasto no encontrado"));
        gasto.setAnulado(true);
    }

    private GastoEmpresarialResponseDTO dto(GastoEmpresarial gasto) {
        User quien = gasto.getRegistradoPor();
        String nombre = quien == null ? "" : ((quien.getNombre() == null ? "" : quien.getNombre()) + " " + (quien.getApellido() == null ? "" : quien.getApellido())).trim();
        return new GastoEmpresarialResponseDTO(
                gasto.getId(),
                gasto.getSede() == null ? null : gasto.getSede().getId(),
                gasto.getSede() == null ? null : gasto.getSede().getNombre(),
                gasto.getTipo(),
                gasto.getMonto(),
                gasto.getFecha(),
                gasto.getDescripcion(),
                nombre,
                gasto.getCreatedAt());
    }

    private User usuario() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email).orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
