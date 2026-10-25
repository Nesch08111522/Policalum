package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.policalum.erp.application.dto.request.AlmacenRequestDTO;
import com.policalum.erp.application.dto.response.AlmacenResponseDTO;
import com.policalum.erp.application.service.AlmacenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Inventario")
@RestController
@RequestMapping("/api/v1/almacenes")
@RequiredArgsConstructor
public class AlmacenController {

    private final AlmacenService almacenService;

    @PostMapping
    public ResponseEntity<AlmacenResponseDTO> create(@Valid @RequestBody AlmacenRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(almacenService.create(requestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlmacenResponseDTO> update(
            @PathVariable UUID id, @Valid @RequestBody AlmacenRequestDTO requestDTO) {
        return ResponseEntity.ok(almacenService.update(id, requestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlmacenResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(almacenService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<AlmacenResponseDTO>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(almacenService.search(searchTerm, sedeId, activo, pageable));
    }

    @GetMapping("/sede/{sedeId}")
    public ResponseEntity<List<AlmacenResponseDTO>> findBySede(@PathVariable UUID sedeId) {
        return ResponseEntity.ok(almacenService.findBySede(sedeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AlmacenResponseDTO> delete(@PathVariable UUID id) {
        AlmacenResponseDTO respuesta = almacenService.delete(id);
        if (respuesta == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/validar-nombre")
    public ResponseEntity<Map<String, Boolean>> validateName(
            @RequestParam UUID sedeId,
            @RequestParam String nombre,
            @RequestParam(required = false) UUID excludeId) {
        boolean disponible = almacenService.isNombreDisponible(sedeId, nombre, excludeId);
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    @GetMapping("/estadisticas/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(almacenService.getDashboardStatistics());
    }
}
