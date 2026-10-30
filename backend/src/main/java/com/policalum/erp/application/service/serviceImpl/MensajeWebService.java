package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.ContactoWebRequestDTO;
import com.policalum.erp.application.dto.request.GestionarMensajeRequestDTO;
import com.policalum.erp.application.dto.response.MensajeContactoResponseDTO;
import com.policalum.erp.domain.enums.EstadoMensajeWeb;
import com.policalum.erp.domain.model.MensajeContacto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.MensajeContactoRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MensajeWebService {

    private final MensajeContactoRepository repository;
    private final CanalWebNotificador notificador;

    @Transactional
    public MensajeContactoResponseDTO recibir(ContactoWebRequestDTO request) {
        MensajeContacto mensaje = MensajeContacto.builder()
                .nombre(request.nombre().trim())
                .correo(request.correo().trim().toLowerCase())
                .telefono(vacio(request.telefono()))
                .empresa(vacio(request.empresa()))
                .asunto(request.asunto().trim())
                .mensaje(request.mensaje().trim())
                .estado(EstadoMensajeWeb.NUEVO)
                .build();
        MensajeContacto guardado = repository.save(mensaje);
        notificador.mensaje(guardado);
        return dto(guardado);
    }

    @Transactional(readOnly = true)
    public Page<MensajeContactoResponseDTO> listar(EstadoMensajeWeb estado, Pageable pageable) {
        Page<MensajeContacto> pagina = estado == null
                ? repository.findAllByOrderByCreatedAtDesc(pageable)
                : repository.findByEstadoOrderByCreatedAtDesc(estado, pageable);
        return pagina.map(this::dto);
    }

    @Transactional(readOnly = true)
    public long nuevos() {
        return repository.countByEstado(EstadoMensajeWeb.NUEVO);
    }

    @Transactional
    public MensajeContactoResponseDTO gestionar(UUID id, GestionarMensajeRequestDTO request) {
        MensajeContacto mensaje = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
        mensaje.setEstado(EstadoMensajeWeb.GESTIONADO);
        mensaje.setNotaGestion(request == null || request.nota() == null || request.nota().isBlank() ? null : request.nota().trim());
        mensaje.setGestionadoAt(LocalDateTime.now());
        return dto(mensaje);
    }

    private MensajeContactoResponseDTO dto(MensajeContacto mensaje) {
        return new MensajeContactoResponseDTO(mensaje.getId(), mensaje.getNombre(), mensaje.getCorreo(), mensaje.getTelefono(),
                mensaje.getEmpresa(), mensaje.getAsunto(), mensaje.getMensaje(), mensaje.getEstado(), mensaje.getNotaGestion(),
                mensaje.getCreatedAt(), mensaje.getGestionadoAt());
    }

    private static String vacio(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
