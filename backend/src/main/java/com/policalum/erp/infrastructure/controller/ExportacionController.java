package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.service.serviceImpl.ExportacionServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Exportación")
@RestController
@RequestMapping("/api/v1/exportacion")
@RequiredArgsConstructor
public class ExportacionController {

    private final ExportacionServiceImpl exportacion;

    @GetMapping
    public ResponseEntity<byte[]> descargar(
            @RequestParam String modulo,
            @RequestParam(defaultValue = "xlsx") String formato,
            @RequestParam(required = false) String filtros) {
        boolean pdf = "pdf".equalsIgnoreCase(formato);
        byte[] cuerpo = exportacion.generar(modulo, pdf ? "pdf" : "xlsx", filtros);
        String nombre = modulo + (pdf ? ".pdf" : ".xlsx");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.parseMediaType(pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(cuerpo);
    }
}
