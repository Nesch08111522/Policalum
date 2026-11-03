package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.service.serviceImpl.EnvioPushService;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notificaciones/push")
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
@RequiredArgsConstructor
public class PushController {

    private final EnvioPushService push;

    @GetMapping("/clave")
    public Map<String, String> clave() {
        return Map.of("publica", push.clavePublica());
    }

    @PostMapping
    public ResponseEntity<Void> guardar(@RequestBody Suscripcion cuerpo) {
        push.guardar(cuerpo.endpoint(), cuerpo.keys().p256dh(), cuerpo.keys().auth(), cuerpo.dispositivo());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> quitar(@RequestParam String endpoint) {
        push.quitar(endpoint);
        return ResponseEntity.noContent().build();
    }

    public record Suscripcion(@NotBlank String endpoint, Keys keys, String dispositivo) {}

    public record Keys(@NotBlank String p256dh, @NotBlank String auth) {}
}
