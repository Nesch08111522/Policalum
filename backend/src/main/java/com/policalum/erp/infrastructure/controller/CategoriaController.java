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

import com.policalum.erp.application.dto.request.CategoriaRequestDTO;
import com.policalum.erp.application.dto.response.CategoriaResponseDTO;
import com.policalum.erp.application.service.CategoriaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Catálogo")
@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> create(@Valid @RequestBody CategoriaRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.create(requestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> update(
            @PathVariable UUID id, @Valid @RequestBody CategoriaRequestDTO requestDTO) {
        return ResponseEntity.ok(categoriaService.update(id, requestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoriaService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<CategoriaResponseDTO>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Boolean activa,
            @PageableDefault(size = 20, sort = "nombre", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(categoriaService.search(searchTerm, activa, pageable));
    }

    @GetMapping("/raices")
    public ResponseEntity<List<CategoriaResponseDTO>> findRoots() {
        return ResponseEntity.ok(categoriaService.findRoots());
    }

    @GetMapping("/{parentId}/hijas")
    public ResponseEntity<List<CategoriaResponseDTO>> findChildren(@PathVariable UUID parentId) {
        return ResponseEntity.ok(categoriaService.findChildren(parentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoriaService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/validar-slug")
    public ResponseEntity<Map<String, Boolean>> validateSlug(
            @RequestParam String slug,
            @RequestParam(required = false) UUID excludeId) {
        return ResponseEntity.ok(Map.of("disponible",
                categoriaService.isSlugDisponible(slug, excludeId)));
    }

    @GetMapping("/estadisticas/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(categoriaService.getDashboardStatistics());
    }
}
