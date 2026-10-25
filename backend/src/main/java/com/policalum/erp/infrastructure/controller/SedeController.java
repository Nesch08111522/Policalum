package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.SedeRequestDTO;
import com.policalum.erp.application.dto.response.SedeMapaDTO;
import com.policalum.erp.application.dto.response.SedeResponseDTO;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.SedeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Tag(name = "Administración")
@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final SedeService sedeService;
    private final FileStorageService fileStorageService;

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

    @GetMapping("/mapa")
    public ResponseEntity<List<SedeMapaDTO>> mapa() {
        return ResponseEntity.ok(sedeService.mapa());
    }

    @GetMapping("/imagenes")
    public ResponseEntity<Resource> verImagen(@RequestParam String ruta) {
        if (ruta == null || !ruta.startsWith("sedes/") || ruta.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        Resource resource = fileStorageService.cargar(ruta);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (resource.getFilename() != null ? resource.getFilename() : "sede") + "\"")
                .contentType(tipoImagen(resource.getFilename()))
                .body(resource);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SedeResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(sedeService.buscarPorId(id));
    }

    @PostMapping(value = "/{id}/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SedeResponseDTO> subirImagen(@PathVariable UUID id, @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(sedeService.guardarImagen(id, archivo));
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

    private MediaType tipoImagen(String nombre) {
        String lower = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
