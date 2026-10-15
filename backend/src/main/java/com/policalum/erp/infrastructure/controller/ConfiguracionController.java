package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.EmpresaRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosRequestDTO;
import com.policalum.erp.application.dto.request.SeriesRequestDTO;
import com.policalum.erp.application.dto.response.ConfiguracionPublicaDTO;
import com.policalum.erp.application.dto.response.EmpresaResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosResponseDTO;
import com.policalum.erp.application.dto.response.SerieComprobanteResponseDTO;
import com.policalum.erp.application.service.ConfiguracionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Configuración global (Módulo 14). Lectura: ADMIN y MULTIFUNCIONAL. Escritura: solo ADMIN.
 * /publica no requiere token (permitAll en SecurityConfig) y solo expone datos de marca.
 */
@Tag(name = "Configuración")
@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class ConfiguracionController {

    private final ConfiguracionService service;

    /** Datos de la empresa. configurada=false mientras no tenga RUC. */
    @GetMapping("/empresa")
    public ResponseEntity<EmpresaResponseDTO> obtenerEmpresa() {
        return ResponseEntity.ok(service.obtenerEmpresa());
    }

    /** Reemplaza los datos de la empresa. 400 si el RUC no pasa el dígito verificador. */
    @PutMapping("/empresa")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmpresaResponseDTO> actualizarEmpresa(@Valid @RequestBody EmpresaRequestDTO request) {
        return ResponseEntity.ok(service.actualizarEmpresa(request));
    }

    /** Parámetros de negocio por dominio (ventas, inventario, finanzas, reportes). */
    @GetMapping("/parametros")
    public ResponseEntity<ParametrosResponseDTO> obtenerParametros() {
        return ResponseEntity.ok(service.obtenerParametros());
    }

    /** Actualiza los dominios enviados; los omitidos no cambian. Aplica desde la siguiente operación. */
    @PutMapping("/parametros")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ParametrosResponseDTO> actualizarParametros(@Valid @RequestBody ParametrosRequestDTO request) {
        return ResponseEntity.ok(service.actualizarParametros(request));
    }

    /** Series de comprobante, opcionalmente de una sede. */
    @GetMapping("/series")
    public ResponseEntity<List<SerieComprobanteResponseDTO>> listarSeries(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(service.listarSeries(sedeId));
    }

    /** Upsert de series por sede + tipo. 409 si la serie ya pertenece a otra sede/tipo. */
    @PutMapping("/series")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SerieComprobanteResponseDTO>> actualizarSeries(@Valid @RequestBody SeriesRequestDTO request) {
        return ResponseEntity.ok(service.actualizarSeries(request));
    }

    /** Nombre comercial y logo (pantalla de login). Sin autenticación. */
    @GetMapping("/publica")
    @PreAuthorize("permitAll()")
    public ResponseEntity<ConfiguracionPublicaDTO> publica() {
        return ResponseEntity.ok(service.publica());
    }
}
