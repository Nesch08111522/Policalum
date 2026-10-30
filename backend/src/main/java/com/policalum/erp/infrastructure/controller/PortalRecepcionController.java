package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.ContactoWebRequestDTO;
import com.policalum.erp.application.dto.request.CotizacionWebRequestDTO;
import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.dto.response.MensajeContactoResponseDTO;
import com.policalum.erp.application.service.CotizacionService;
import com.policalum.erp.application.service.serviceImpl.CanalWebNotificador;
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
    private final CanalWebNotificador notificador;
    private final LimitePublico limite;

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

    private void exigirCupo(String clave) {
        if (!limite.permitir(clave)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiadas solicitudes. Intenta de nuevo en un minuto.");
        }
    }
}
