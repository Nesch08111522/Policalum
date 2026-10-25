package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.ValidationException;
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

import com.policalum.erp.application.dto.request.ProveedorRequestDTO;
import com.policalum.erp.application.dto.response.ProveedorResponseDTO;
import com.policalum.erp.application.service.ProveedorService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "Proveedores")
@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Slf4j
public class ProveedorController {

    private final ProveedorService proveedorService;

    @PostMapping
    public ResponseEntity<ProveedorResponseDTO> create(@Valid @RequestBody ProveedorRequestDTO requestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.create(requestDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> update(
            @PathVariable UUID id, @Valid @RequestBody ProveedorRequestDTO requestDTO) {
        return ResponseEntity.ok(proveedorService.update(id, requestDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(proveedorService.findById(id));
    }

    @GetMapping("/documento")
    public ResponseEntity<ProveedorResponseDTO> findByDocumento(
            @RequestParam String tipoDocumento, @RequestParam String numeroDocumento) {
        return ResponseEntity.ok(proveedorService.findByDocumento(
                tipoDocumento.trim().toUpperCase(), numeroDocumento.trim()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ProveedorResponseDTO> delete(@PathVariable UUID id) {
        ProveedorResponseDTO respuesta = proveedorService.delete(id);
        if (respuesta == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping
    public ResponseEntity<Page<ProveedorResponseDTO>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Boolean activo,
            @PageableDefault(size = 20, sort = "nombreRazon", direction = Sort.Direction.ASC) Pageable pageable) {
        if (activo != null) {
            return ResponseEntity.ok(proveedorService.findByActivo(activo, pageable));
        }
        return ResponseEntity.ok(proveedorService.search(searchTerm, pageable));
    }

    @GetMapping("/incompletos")
    public ResponseEntity<List<ProveedorResponseDTO>> findIncompleteContactData() {
        return ResponseEntity.ok(proveedorService.findWithIncompleteContactData());
    }

    @GetMapping("/estadisticas/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(proveedorService.getDashboardStatistics());
    }

    @GetMapping("/estadisticas/tipo-documento")
    public ResponseEntity<Map<String, Long>> statisticsByDocumentType() {
        return ResponseEntity.ok(proveedorService.getStatisticsByDocumentType());
    }

    @GetMapping("/validar/documento")
    public ResponseEntity<Map<String, Boolean>> validateDocument(
            @RequestParam String tipoDocumento,
            @RequestParam String numeroDocumento,
            @RequestParam(required = false) UUID excludeId) {
        boolean disponible = proveedorService.isDocumentoDisponible(
                tipoDocumento.trim().toUpperCase(), numeroDocumento.trim(), excludeId);
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }


    private void validarCondicionesPago(ProveedorRequestDTO dto) {
        if (dto.condicionesPago() != null
                && dto.condicionesPago().toUpperCase().startsWith("CREDITO")) {
            if (dto.diasCredito() == null || dto.diasCredito() <= 0) {
                throw new ValidationException(
                        "Debe indicar los días de crédito para proveedores con condiciones de crédito");
            }
        }
    }
}