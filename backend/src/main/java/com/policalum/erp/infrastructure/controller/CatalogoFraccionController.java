package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
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

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.response.CatalogoFraccionResponseDTO;
import com.policalum.erp.application.service.CatalogoFraccionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Catálogo")
@RestController
@RequestMapping("/api/v1/catalogo-fracciones")
@RequiredArgsConstructor
public class CatalogoFraccionController {

    private final CatalogoFraccionService catalogoFraccionService;

    @PostMapping
    public ResponseEntity<CatalogoFraccionResponseDTO> create(
            @Valid @RequestBody CatalogoFraccionRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogoFraccionService.create(requestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CatalogoFraccionResponseDTO> update(
            @PathVariable UUID id, @Valid @RequestBody CatalogoFraccionRequestDTO requestDTO) {
        return ResponseEntity.ok(catalogoFraccionService.update(id, requestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogoFraccionResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(catalogoFraccionService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CatalogoFraccionResponseDTO>> search(
            @RequestParam(required = false) UUID productoId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String searchTerm,
            @PageableDefault(size = 20, sort = "factorDescuentoKardex", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(catalogoFraccionService.search(productoId, activo, searchTerm, pageable));
    }

    @GetMapping("/producto/{productoId}/disponibles")
    public ResponseEntity<List<CatalogoFraccionResponseDTO>> findAvailableByProducto(
            @PathVariable UUID productoId) {
        return ResponseEntity.ok(catalogoFraccionService.findAvailableByProducto(productoId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CatalogoFraccionResponseDTO> delete(@PathVariable UUID id) {
        CatalogoFraccionResponseDTO respuesta = catalogoFraccionService.delete(id);
        if (respuesta == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/validar-factor")
    public ResponseEntity<Map<String, Boolean>> validateFactor(
            @RequestParam UUID productoId,
            @RequestParam BigDecimal factor,
            @RequestParam(required = false) UUID excludeId) {
        return ResponseEntity.ok(Map.of("disponible",
                catalogoFraccionService.isFactorDisponible(productoId, factor, excludeId)));
    }

    @GetMapping("/estadisticas/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(catalogoFraccionService.getDashboardStatistics());
    }
}
