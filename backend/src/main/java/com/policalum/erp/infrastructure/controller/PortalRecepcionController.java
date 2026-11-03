package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.ChatClienteRequest;
import com.policalum.erp.application.dto.request.ContactoWebRequestDTO;
import com.policalum.erp.application.dto.request.CotizacionWebRequestDTO;
import com.policalum.erp.application.dto.request.PresenciaChatRequest;
import com.policalum.erp.application.dto.request.VistaCotizacionRequestDTO;
import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.dto.response.FormatoCotizacionDTO;
import com.policalum.erp.application.dto.response.HiloPublicoDTO;
import com.policalum.erp.application.dto.response.MensajeContactoResponseDTO;
import com.policalum.erp.application.dto.response.VistaCotizacionDTO;
import com.policalum.erp.application.service.CotizacionService;
import com.policalum.erp.application.service.serviceImpl.CanalWebNotificador;
import com.policalum.erp.application.service.serviceImpl.ChatWebService;
import com.policalum.erp.application.service.serviceImpl.CotizacionDocumentoService;
import com.policalum.erp.application.service.serviceImpl.LimitePublico;
import com.policalum.erp.application.service.serviceImpl.MensajeWebService;
import com.policalum.erp.domain.enums.CanalCotizacion;
import com.policalum.erp.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Portal público")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PortalRecepcionController {

    private final CotizacionService cotizacionService;
    private final CotizacionDocumentoService documentos;
    private final MensajeWebService mensajes;
    private final ChatWebService chat;
    private final CanalWebNotificador notificador;
    private final LimitePublico limite;

    @GetMapping("/cotizaciones/formato")
    public ResponseEntity<FormatoCotizacionDTO> formato() {
        return ResponseEntity.ok(cotizacionService.formatoPublico());
    }

    @PostMapping("/cotizaciones/vista")
    public ResponseEntity<VistaCotizacionDTO> vista(@Valid @RequestBody VistaCotizacionRequestDTO request, HttpServletRequest http) {
        exigirCupo("vista:" + http.getRemoteAddr());
        return ResponseEntity.ok(cotizacionService.vista(request));
    }

    @PostMapping("/cotizaciones")
    public ResponseEntity<CotizacionResponseDTO> cotizar(@Valid @RequestBody CotizacionWebRequestDTO request, HttpServletRequest http) {
        exigirCupo("cotizacion:" + http.getRemoteAddr());
        if (request.website() != null && !request.website().isBlank()) {
            throw new IllegalArgumentException("Solicitud no válida");
        }
        CotizacionResponseDTO creada = cotizacionService.crearDesdeWeb(request);
        notificador.cotizacion(creada);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    /** PDF solo de cotizaciones nacidas en la web. El id es el que devolvió el POST. */
    @GetMapping("/cotizaciones/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        CotizacionResponseDTO cotizacion = cotizacionService.findById(id);
        if (cotizacion.canal() != CanalCotizacion.WEB) {
            throw new ResourceNotFoundException("Cotización no encontrada");
        }
        return documentos.descargar(id, "pdf");
    }

    @PostMapping("/contacto")
    public ResponseEntity<MensajeContactoResponseDTO> contacto(@Valid @RequestBody ContactoWebRequestDTO request, HttpServletRequest http) {
        exigirCupo("contacto:" + http.getRemoteAddr());
        if (request.website() != null && !request.website().isBlank()) {
            throw new IllegalArgumentException("Solicitud no válida");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(mensajes.recibir(request));
    }

    @GetMapping("/chat-coincidencias")
    public ResponseEntity<com.policalum.erp.application.dto.response.ReconocimientoChatDTO> coincidencias(
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String dispositivo) {
        return ResponseEntity.ok(chat.reconocer(correo, telefono, dispositivo));
    }

    @PostMapping("/chat-continuar")
    public ResponseEntity<java.util.Map<String, String>> continuar(
            @Valid @RequestBody com.policalum.erp.application.dto.request.ContinuarChatRequest request) {
        return ResponseEntity.ok(java.util.Map.of("clave", chat.continuar(request.referencia(), request.correo(), request.telefono(), request.dispositivo())));
    }

    @GetMapping("/chat/{clave}")
    public ResponseEntity<HiloPublicoDTO> leerChat(@PathVariable String clave) {
        return ResponseEntity.ok(chat.leerCliente(clave));
    }

    @PostMapping("/chat")
    public ResponseEntity<HiloPublicoDTO> escribirChat(@Valid @RequestBody ChatClienteRequest request, HttpServletRequest http) {
        exigirCupo("chat:" + http.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(chat.enviarCliente(request));
    }

    @PostMapping("/chat/{clave}/leido")
    public ResponseEntity<Void> leerChatCliente(@PathVariable String clave) {
        chat.marcarLeidoCliente(clave);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/chat/{clave}/escribiendo")
    public ResponseEntity<Void> presenciaChat(@PathVariable String clave, @RequestBody(required = false) PresenciaChatRequest request) {
        chat.presenciaCliente(clave, request == null || request.activo() == null || request.activo());
        return ResponseEntity.noContent().build();
    }

    private void exigirCupo(String clave) {
        if (!limite.permitir(clave)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Intenta de nuevo en un minuto.");
        }
    }
}
