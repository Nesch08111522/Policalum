package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.request.PerfilActualizarRequestDTO;
import com.policalum.erp.application.dto.request.PerfilContrasenaRequestDTO;
import com.policalum.erp.application.dto.request.PerfilTotpRequestDTO;
import com.policalum.erp.application.dto.response.PerfilResponseDTO;
import com.policalum.erp.application.service.serviceImpl.PerfilServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
