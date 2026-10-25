package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.dto.response.SedeResponseDTO;
import com.policalum.erp.application.service.SedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Administración")
@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SedeController {

    private final SedeService sedeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SedeResponseDTO> crearSede(@Valid @RequestBody SedeRequestDTO request) {
        SedeResponseDTO response = sedeService.crearSede(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<SedeResponseDTO>> listarTodas(
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(sedeService.listarTodas(pageable));
    }

    @GetMapping("/estado")
    public ResponseEntity<Page<SedeResponseDTO>> listarPorEstado(
            @RequestParam Boolean activa,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {
        return ResponseEntity.ok(sedeService.listarPorEstado(activa, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SedeResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(sedeService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SedeResponseDTO> actualizarSede(
            @PathVariable UUID id,
            @Valid @RequestBody SedeRequestDTO request) {
        SedeResponseDTO response = sedeService.actualizarSede(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SedeResponseDTO> eliminarSede(@PathVariable UUID id) {
        SedeResponseDTO respuesta = sedeService.eliminarSede(id);
        if (respuesta == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(respuesta);
    }
}