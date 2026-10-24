package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.PerfilActualizarRequestDTO;
import com.policalum.erp.application.dto.request.PerfilContrasenaRequestDTO;
import com.policalum.erp.application.dto.request.PerfilTotpRequestDTO;
import com.policalum.erp.application.dto.response.PerfilResponseDTO;
import com.policalum.erp.application.service.serviceImpl.PerfilServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Cuenta")
@RestController
@RequestMapping("/api/v1/perfil")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class PerfilController {

    private final PerfilServiceImpl perfilService;

    @GetMapping
    public ResponseEntity<PerfilResponseDTO> actual() {
        return ResponseEntity.ok(perfilService.actual());
    }

    @PutMapping
    public ResponseEntity<PerfilResponseDTO> actualizar(@Valid @RequestBody PerfilActualizarRequestDTO request) {
        return ResponseEntity.ok(perfilService.actualizar(request));
    }

    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PerfilResponseDTO> subirFoto(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(perfilService.guardarFoto(archivo));
    }

    @GetMapping("/foto")
    public ResponseEntity<Resource> verFoto() {
        Resource resource = perfilService.cargarFoto();
        String nombre = resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (nombre != null ? nombre : "foto") + "\"")
                .contentType(tipoFoto(nombre))
                .body(resource);
    }

    @PostMapping("/contrasena")
    public ResponseEntity<PerfilResponseDTO> contrasena(@Valid @RequestBody PerfilContrasenaRequestDTO request) {
        return ResponseEntity.ok(perfilService.cambiarContrasena(request));
    }

    @PostMapping("/totp")
    public ResponseEntity<PerfilResponseDTO> totp(@Valid @RequestBody PerfilTotpRequestDTO request) {
        return ResponseEntity.ok(perfilService.cambiarTotp(request.activo()));
    }

    @PostMapping("/cerrar-sesiones")
    public ResponseEntity<PerfilResponseDTO> cerrarSesiones() {
        return ResponseEntity.ok(perfilService.cerrarOtrasSesiones());
    }

    private MediaType tipoFoto(String nombre) {
        String lower = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
