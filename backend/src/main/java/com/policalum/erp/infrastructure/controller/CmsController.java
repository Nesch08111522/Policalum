package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.CmsAjusteRequestDTO;
import com.policalum.erp.application.dto.request.HorarioOperativoRequestDTO;
import com.policalum.erp.application.dto.request.OfertaManualRequestDTO;
import com.policalum.erp.application.dto.request.ProyectoPortafolioRequestDTO;
import com.policalum.erp.application.dto.response.CmsPublicoDTO;
import com.policalum.erp.application.dto.response.ProyectoPortafolioResponseDTO;
import com.policalum.erp.application.service.serviceImpl.CmsPublicador;
import com.policalum.erp.application.service.serviceImpl.CmsService;
import com.policalum.erp.application.service.serviceImpl.PortafolioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class CmsController {

    private final CmsService cms;
    private final CmsPublicador publicador;
    private final PortafolioService portafolio;

    @GetMapping("/api/v1/cms/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public CmsPublicoDTO leerAjustes() {
        return cms.actual();
    }

    @PutMapping("/api/v1/cms/settings")
    @PreAuthorize("hasRole('ADMIN')")
    public CmsPublicoDTO guardarAjustes(@Valid @RequestBody CmsAjusteRequestDTO request) {
        return cms.guardarAjustes(request);
    }

    @PutMapping("/api/v1/cms/horario")
    @PreAuthorize("hasRole('ADMIN')")
    public CmsPublicoDTO guardarHorario(@Valid @RequestBody HorarioOperativoRequestDTO request) {
        return cms.guardarHorario(request);
    }

    @PostMapping("/api/v1/cms/ofertas/sincronizar")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> sincronizarOfertas() {
        return Map.of("marcados", cms.sincronizarOfertas());
    }

    @PutMapping("/api/v1/cms/productos/{id}/oferta")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> marcarOferta(@PathVariable UUID id, @Valid @RequestBody OfertaManualRequestDTO request) {
        cms.marcarOferta(id, request.oferta());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/cms/proyectos")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ProyectoPortafolioResponseDTO> proyectos() {
        return portafolio.listar();
    }

    @PostMapping("/api/v1/cms/proyectos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProyectoPortafolioResponseDTO> crearProyecto(@Valid @RequestBody ProyectoPortafolioRequestDTO request) {
        return ResponseEntity.status(201).body(portafolio.crear(request));
    }

    @PutMapping("/api/v1/cms/proyectos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProyectoPortafolioResponseDTO actualizarProyecto(@PathVariable UUID id, @Valid @RequestBody ProyectoPortafolioRequestDTO request) {
        return portafolio.actualizar(id, request);
    }

    @DeleteMapping("/api/v1/cms/proyectos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarProyecto(@PathVariable UUID id) {
        portafolio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/public/cms")
    public CmsPublicoDTO publico() {
        return cms.publico();
    }

    @GetMapping("/api/v1/public/cms/eventos")
    public SseEmitter eventos() {
        return publicador.suscribir();
    }
}
