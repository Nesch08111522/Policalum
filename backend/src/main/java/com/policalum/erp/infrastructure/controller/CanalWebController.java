package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.GestionarMensajeRequestDTO;
import com.policalum.erp.application.dto.response.MensajeContactoResponseDTO;
import com.policalum.erp.application.service.serviceImpl.CanalWebNotificador;
import com.policalum.erp.application.service.serviceImpl.MensajeWebService;
import com.policalum.erp.domain.enums.EstadoMensajeWeb;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Canal web")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class CanalWebController {

    private final MensajeWebService mensajes;
    private final CanalWebNotificador notificador;

    @GetMapping("/mensajes-web")
    public ResponseEntity<Page<MensajeContactoResponseDTO>> listar(
            @RequestParam(required = false) EstadoMensajeWeb estado,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(mensajes.listar(estado, pageable));
    }

    @GetMapping("/mensajes-web/nuevos")
    public ResponseEntity<Map<String, Long>> nuevos() {
        return ResponseEntity.ok(Map.of("nuevos", mensajes.nuevos()));
    }

    @PatchMapping("/mensajes-web/{id}/gestionar")
    public ResponseEntity<MensajeContactoResponseDTO> gestionar(@PathVariable UUID id, @Valid @RequestBody(required = false) GestionarMensajeRequestDTO request) {
        return ResponseEntity.ok(mensajes.gestionar(id, request));
    }

    /** El ERP abre este flujo con access_token porque EventSource no manda Authorization. */
    @GetMapping(value = "/canal-web/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos() {
        return notificador.suscribir();
    }
}
