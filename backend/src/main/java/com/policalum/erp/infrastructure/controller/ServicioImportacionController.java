package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.service.serviceImpl.ServicioImportacionServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Servicios")
@RestController
@RequestMapping("/api/v1/servicios/importar")
@RequiredArgsConstructor
public class ServicioImportacionController {

    private final ServicioImportacionServiceImpl service;

    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-servicios.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(service.plantilla());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MULTIFUNCIONAL')")
    public ResponseEntity<Map<String, Integer>> importar(@RequestPart("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(service.importar(archivo));
    }
}
