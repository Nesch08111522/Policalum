package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.response.NotificacionResponseDTO;
import com.policalum.erp.application.service.serviceImpl.NotificacionServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notificaciones")
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionServiceImpl service;

    @GetMapping
    public ResponseEntity<List<NotificacionResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<NotificacionResponseDTO> leida(@PathVariable UUID id) {
        return ResponseEntity.ok(service.marcarLeida(id));
    }

    @PostMapping("/leer-todas")
    public ResponseEntity<Map<String, Integer>> leerTodas() {
        return ResponseEntity.ok(Map.of("marcadas", service.marcarTodasLeidas()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<NotificacionResponseDTO> archivar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.archivar(id));
    }
}
