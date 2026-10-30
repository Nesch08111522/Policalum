package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.ProyectoPortafolioRequestDTO;
import com.policalum.erp.application.dto.response.ProyectoPortafolioResponseDTO;
import com.policalum.erp.application.service.serviceImpl.PortafolioService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@Tag(name = "Portafolio web")
@RestController
@RequestMapping("/api/v1/portafolio")
@RequiredArgsConstructor
public class PortafolioController {

    private final PortafolioService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<List<ProyectoPortafolioResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProyectoPortafolioResponseDTO> crear(@Valid @RequestBody ProyectoPortafolioRequestDTO request) {
        return ResponseEntity.status(201).body(service.crear(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProyectoPortafolioResponseDTO> actualizar(@PathVariable UUID id, @Valid @RequestBody ProyectoPortafolioRequestDTO request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
