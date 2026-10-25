// ✅ GUARDA ESTE ARCHIVO EN:
// src/main/java/com/policalum/erp/infraestructure/controller/AuthController.java

package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.LoginRequestDTO;
import com.policalum.erp.application.dto.request.LogoutRequestDTO;
import com.policalum.erp.application.dto.request.RefreshRequestDTO;
import com.policalum.erp.application.dto.response.LoginResponseDTO;
import com.policalum.erp.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO request, HttpServletRequest http) {
        LoginResponseDTO response = authService.login(request, ipDe(http));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO request) {
        return ResponseEntity.ok(authService.refrescar(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequestDTO request, HttpServletRequest http) {
        String header = http.getHeader("Authorization");
        String access = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        authService.cerrarSesion(request == null ? null : request.refreshToken(), access);
        return ResponseEntity.noContent().build();
    }

    static String ipDe(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return ip == null ? "" : ip;
    }
}