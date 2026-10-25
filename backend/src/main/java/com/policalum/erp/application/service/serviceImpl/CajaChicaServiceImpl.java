package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.CajaChicaRequestDTO;
import com.policalum.erp.application.dto.response.CajaChicaResponseDTO;
import com.policalum.erp.domain.enums.EstadoTurno;
import com.policalum.erp.domain.model.MovimientoCajaChica;
import com.policalum.erp.domain.model.TurnoCaja;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.MovimientoCajaChicaRepository;
import com.policalum.erp.infrastructure.repository.TurnoCajaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CajaChicaServiceImpl {

    private final MovimientoCajaChicaRepository repository;
    private final TurnoCajaRepository turnoRepository;
    private final UserRepository userRepository;

    @Transactional
    public CajaChicaResponseDTO registrar(UUID turnoId, CajaChicaRequestDTO request) {
        TurnoCaja turno = turnoRepository.findById(turnoId).orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado"));
        User user = usuario();
        if (turno.getEstado() != EstadoTurno.ABIERTO) throw new IllegalStateException("La caja chica solo se mueve con el turno abierto");
        if (!turno.getUser().getId().equals(user.getId())) throw new IllegalStateException("Solo el responsable del turno registra caja chica");
        MovimientoCajaChica movimiento = MovimientoCajaChica.builder()
                .turno(turno).user(user).tipo(request.tipo()).concepto(request.concepto())
                .monto(request.monto()).detalle(request.detalle() == null ? null : request.detalle().trim())
                .build();
        return dto(repository.save(movimiento));
    }

    @Transactional(readOnly = true)
    public List<CajaChicaResponseDTO> listar(UUID turnoId) {
        if (!turnoRepository.existsById(turnoId)) throw new ResourceNotFoundException("Turno no encontrado");
        return repository.findByTurno_IdOrderByCreatedAtDesc(turnoId).stream().map(this::dto).toList();
    }

    private CajaChicaResponseDTO dto(MovimientoCajaChica movimiento) {
        return new CajaChicaResponseDTO(movimiento.getId(), movimiento.getTurno().getId(), movimiento.getTipo(),
                movimiento.getConcepto(), movimiento.getMonto(), movimiento.getDetalle(), movimiento.getCreatedAt());
    }

    private User usuario() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email).orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
