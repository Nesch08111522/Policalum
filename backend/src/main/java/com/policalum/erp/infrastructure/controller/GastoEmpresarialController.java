package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.GastoEmpresarialRequestDTO;
import com.policalum.erp.application.dto.response.GastoEmpresarialResponseDTO;
import com.policalum.erp.application.service.serviceImpl.GastoEmpresarialServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Gastos empresariales")
@RestController
@RequestMapping("/api/v1/finanzas/gastos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GastoEmpresarialController {

    private final GastoEmpresarialServiceImpl service;

    @GetMapping
    public ResponseEntity<Page<GastoEmpresarialResponseDTO>> listar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fecha") Pageable pageable) {
        return ResponseEntity.ok(service.listar(sedeId, desde, hasta, pageable));
    }

    @PostMapping
    public ResponseEntity<GastoEmpresarialResponseDTO> registrar(@Valid @RequestBody GastoEmpresarialRequestDTO request) {
        return ResponseEntity.ok(service.registrar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable UUID id) {
        service.anular(id);
        return ResponseEntity.noContent().build();
    }
}
