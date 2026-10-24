package com.policalum.erp.infrastructure.controller;

import com.policalum.erp.application.dto.response.FacturacionElectronicaPanelDTO;
import com.policalum.erp.application.service.FacturacionElectronicaService;
import com.policalum.erp.application.service.SunatFacturacionClient;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/** Panel local de facturación electrónica. La emisión y la consulta CDR esperan la API de SUNAT. */
@Tag(name = "Facturación electrónica")
@RestController
@RequestMapping("/api/v1/facturacion-electronica")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class FacturacionElectronicaController {

    private final FacturacionElectronicaService service;
    private final SunatFacturacionClient sunat;

    @GetMapping("/panel")
    public ResponseEntity<FacturacionElectronicaPanelDTO> panel(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(service.panel(sedeId));
    }

    @PostMapping("/emitir")
    public ResponseEntity<Map<String, Object>> emitir() {
        return pendiente(sunat::emitir);
    }

    @PostMapping("/consultar-validez")
    public ResponseEntity<Map<String, Object>> consultar(@RequestBody(required = false) Map<String, String> body) {
        String serie = body == null ? "" : body.getOrDefault("serie", "");
        String documento = body == null ? "" : body.getOrDefault("documentoCliente", "");
        return pendiente(() -> sunat.consultarValidez(serie, documento));
    }

    private static ResponseEntity<Map<String, Object>> pendiente(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalStateException ex) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", HttpStatus.NOT_IMPLEMENTED.value());
            body.put("error", "Not Implemented");
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(body);
        }
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
