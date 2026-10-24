package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.EmpresaRequestDTO;
import com.policalum.erp.application.dto.request.MarcaRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosRequestDTO;
import com.policalum.erp.application.dto.request.SeriesRequestDTO;
import com.policalum.erp.application.dto.response.ConfiguracionPublicaDTO;
import com.policalum.erp.application.dto.response.EmpresaResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosResponseDTO;
import com.policalum.erp.application.dto.response.SerieComprobanteResponseDTO;
import com.policalum.erp.application.service.ConfiguracionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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

    /** Actualiza el nombre comercial y el logo sin exigir RUC. */
    @PutMapping("/marca")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionPublicaDTO> actualizarMarca(@Valid @RequestBody MarcaRequestDTO request) {
        return ResponseEntity.ok(service.actualizarMarca(request));
    }

    /** Sube el archivo del logo. La empresa lo adopta al guardar la marca. */
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> subirLogo(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(Map.of("ruta", service.guardarLogo(archivo)));
    }

    /** Sirve el logo guardado en disco. Las URL externas se usan tal cual en el cliente. */
    @GetMapping("/logo")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Resource> verLogo() {
        Resource resource = service.cargarLogo();
        String nombre = resource.getFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (nombre != null ? nombre : "logo") + "\"")
                .contentType(tipoLogo(nombre))
                .body(resource);
    }

    private MediaType tipoLogo(String nombre) {
        String lower = nombre == null ? "" : nombre.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
