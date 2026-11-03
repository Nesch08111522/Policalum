package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.ChatTextoRequest;
import com.policalum.erp.application.dto.request.GestionarMensajeRequestDTO;
import com.policalum.erp.application.dto.request.PresenciaChatRequest;
import com.policalum.erp.application.dto.response.HiloEquipoDTO;
import com.policalum.erp.application.dto.response.MensajeContactoResponseDTO;
import com.policalum.erp.application.dto.response.ResumenChatDTO;
import com.policalum.erp.application.service.serviceImpl.CanalWebNotificador;
import com.policalum.erp.application.service.serviceImpl.ChatWebService;
import com.policalum.erp.application.service.serviceImpl.MensajeWebService;
import com.policalum.erp.domain.enums.EstadoMensajeWeb;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final ChatWebService chat;
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

    @GetMapping("/chats")
    public ResponseEntity<List<ResumenChatDTO>> chats() {
        return ResponseEntity.ok(chat.listar());
    }

    @GetMapping("/chats/{id}")
    public ResponseEntity<HiloEquipoDTO> chat(@PathVariable UUID id) {
        return ResponseEntity.ok(chat.leerEquipo(id));
    }

    @PostMapping("/chats/{id}/mensajes")
    public ResponseEntity<HiloEquipoDTO> responder(@PathVariable UUID id, @Valid @RequestBody ChatTextoRequest request) {
        return ResponseEntity.ok(chat.responder(id, request));
    }

    @PostMapping("/chats/{id}/leido")
    public ResponseEntity<Void> leido(@PathVariable UUID id) {
        chat.marcarLeidoEquipo(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/chats/{id}")
    public ResponseEntity<HiloEquipoDTO> actualizarChat(@PathVariable UUID id, @Valid @RequestBody com.policalum.erp.application.dto.request.DatosChatRequest request) {
        return ResponseEntity.ok(chat.actualizarDatos(id, request));
    }

    @PostMapping("/chats/{id}/estado")
    public ResponseEntity<Void> estadoChat(@PathVariable UUID id, @Valid @RequestBody com.policalum.erp.application.dto.request.EstadoChatRequest request) {
        chat.cambiarEstado(id, request.estado());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/chats/{id}")
    public ResponseEntity<Void> eliminarChat(@PathVariable UUID id) {
        chat.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chats/{id}/cliente")
    public ResponseEntity<HiloEquipoDTO> clienteDesdeChat(@PathVariable UUID id, @Valid @RequestBody com.policalum.erp.application.dto.request.ClienteRequestDTO request) {
        return ResponseEntity.ok(chat.guardarCliente(id, request));
    }

    @PostMapping("/chats/{id}/escribiendo")
    public ResponseEntity<Void> escribiendo(@PathVariable UUID id, @RequestBody(required = false) PresenciaChatRequest request) {
        chat.presenciaEquipo(id, request == null || request.activo() == null || request.activo());
        return ResponseEntity.noContent().build();
    }

    /** El ERP abre este flujo con access_token porque EventSource no manda Authorization. */
    @GetMapping(value = "/canal-web/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos() {
        return notificador.suscribir();
    }
}
