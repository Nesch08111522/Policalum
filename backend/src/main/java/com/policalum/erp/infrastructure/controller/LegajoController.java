package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.LegajoRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.LegajoDashboardDTO;
import com.policalum.erp.application.dto.response.LegajoDocumentoResponseDTO;
import com.policalum.erp.application.dto.response.LegajoListadoDTO;
import com.policalum.erp.application.dto.response.LegajoResponseDTO;
import com.policalum.erp.application.service.LegajoService;
import com.policalum.erp.domain.enums.SistemaPension;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Legajo digital. Ojo con los ids:
 * - /{empleadoId} usa el id del USUARIO (empleado).
 * - /{id}/documentos usa el id del LEGAJO (campo "id" de LegajoResponseDTO).
 */
@Tag(name = "RRHH")
@RestController
@RequestMapping("/api/v1/rrhh/legajos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LegajoController {

    private final LegajoService legajoService;

    /** Crea el legajo de un empleado. 201 + Location /legajos/{empleadoId}. */
    @PostMapping
    public ResponseEntity<LegajoResponseDTO> crear(@Valid @RequestBody LegajoRequestDTO request) {
        LegajoResponseDTO creado = legajoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{empleadoId}").buildAndExpand(creado.empleadoId()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    /** Legajo del usuario autenticado (cualquier rol). */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<LegajoResponseDTO> miLegajo() {
        return ResponseEntity.ok(legajoService.obtenerMio());
    }

    /** KPIs de legajos. */
    @GetMapping("/dashboard")
    public ResponseEntity<LegajoDashboardDTO> dashboard(@RequestParam(required = false) UUID sedeId) {
        return ResponseEntity.ok(legajoService.dashboard(sedeId));
    }

    /** Listado paginado. Filtros opcionales; "texto" busca en nombre, apellido y email. */
    @GetMapping
    public ResponseEntity<Page<LegajoListadoDTO>> buscar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) String tipoContrato,
            @RequestParam(required = false) SistemaPension sistemaPension,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20, sort = "fechaIngreso", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(legajoService.buscar(sedeId, tipoContrato, sistemaPension, activo, texto, pageable));
    }

    /** Legajo de un empleado (id de usuario). */
    @GetMapping("/{empleadoId}")
    public ResponseEntity<LegajoResponseDTO> obtener(@PathVariable UUID empleadoId) {
        return ResponseEntity.ok(legajoService.obtenerPorEmpleado(empleadoId));
    }

    /** Actualiza el legajo de un empleado (id de usuario). */
    @PutMapping("/{empleadoId}")
    public ResponseEntity<LegajoResponseDTO> actualizar(@PathVariable UUID empleadoId,
                                                        @Valid @RequestBody LegajoRequestDTO request) {
        return ResponseEntity.ok(legajoService.actualizar(empleadoId, request));
    }

    // ============ DOCUMENTOS (id = id del legajo) ============

    /** Sube un documento (multipart: archivo, tipo, descripcion opcional). 201 + Location. */
    @PostMapping(value = "/{id}/documentos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LegajoDocumentoResponseDTO> subirDocumento(
            @PathVariable UUID id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("tipo") String tipo,
            @RequestParam(value = "descripcion", required = false) String descripcion) {
        LegajoDocumentoResponseDTO doc = legajoService.subirDocumento(id, archivo, tipo, descripcion);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{docId}").buildAndExpand(doc.id()).toUri();
        return ResponseEntity.created(location).body(doc);
    }

    /** Documentos del legajo. ADMIN o el dueño. */
    @GetMapping("/{id}/documentos")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<List<LegajoDocumentoResponseDTO>> listarDocumentos(@PathVariable UUID id) {
        return ResponseEntity.ok(legajoService.listarDocumentos(id));
    }

    /** Descarga un documento con su nombre original. ADMIN o el dueño. */
    @GetMapping("/{id}/documentos/{docId}")
    @PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
    public ResponseEntity<Resource> descargarDocumento(@PathVariable UUID id, @PathVariable UUID docId) {
        ArchivoDescargaDTO archivo = legajoService.descargarDocumento(id, docId);
        return ResponseEntity.ok()
                .contentType(tipoContenido(archivo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombreOriginal(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.resource());
    }

    /** Elimina un documento. 204. */
    @DeleteMapping("/{id}/documentos/{docId}")
    public ResponseEntity<Void> eliminarDocumento(@PathVariable UUID id, @PathVariable UUID docId) {
        legajoService.eliminarDocumento(id, docId);
        return ResponseEntity.noContent().build();
    }

    private MediaType tipoContenido(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
