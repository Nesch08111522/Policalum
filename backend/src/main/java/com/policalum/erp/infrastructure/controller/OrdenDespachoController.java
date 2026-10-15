package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AsignarRepartidorRequestDTO;
import com.policalum.erp.application.dto.request.CambiarEstadoDespachoRequestDTO;
import com.policalum.erp.application.dto.request.CrearDespachoRequestDTO;
import com.policalum.erp.application.dto.request.EvidenciaEntregaRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.DespachoDashboardDTO;
import com.policalum.erp.application.dto.response.OrdenDespachoResponseDTO;
import com.policalum.erp.application.service.OrdenDespachoService;
import com.policalum.erp.domain.enums.EstadoDespacho;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Órdenes de despacho. Todo el personal (ADMIN y MULTIFUNCIONAL) puede crear, consultar y
 * asignar; cambiar estado y registrar la entrega solo el ADMIN o el repartidor asignado
 * (lo valida el servicio).
 */
@Tag(name = "Despachos")
@RestController
@RequestMapping("/api/v1/despachos")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class OrdenDespachoController {

    private final OrdenDespachoService despachoService;

    /** Crea una orden PENDIENTE con código DESP-AAAA-NNNNNN. 201 + Location. */
    @PostMapping
    public ResponseEntity<OrdenDespachoResponseDTO> crear(@Valid @RequestBody CrearDespachoRequestDTO request) {
        OrdenDespachoResponseDTO creada = despachoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /** KPIs del día (por defecto hoy). */
    @GetMapping("/dashboard")
    public ResponseEntity<DespachoDashboardDTO> dashboard(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(despachoService.dashboard(sedeId, fecha));
    }

    /** Busca una orden por su código de seguimiento (personal del ERP). */
    @GetMapping("/seguimiento/{codigo}")
    public ResponseEntity<OrdenDespachoResponseDTO> seguimiento(@PathVariable String codigo) {
        return ResponseEntity.ok(despachoService.seguimiento(codigo));
    }

    /** Órdenes de un repartidor; por defecto solo PENDIENTE y EN_CAMINO. */
    @GetMapping("/repartidor/{userId}")
    public ResponseEntity<List<OrdenDespachoResponseDTO>> porRepartidor(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        return ResponseEntity.ok(despachoService.porRepartidor(userId, soloActivas));
    }

    /** Listado paginado. Las fechas filtran la fecha programada (inclusivas). */
    @GetMapping
    public ResponseEntity<Page<OrdenDespachoResponseDTO>> buscar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoDespacho estado,
            @RequestParam(required = false) UUID responsableId,
            @RequestParam(required = false) UUID ventaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "fechaProgramada", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(despachoService.buscar(sedeId, estado, responsableId, ventaId, desde, hasta, pageable));
    }

    /** Detalle de una orden. */
    @GetMapping("/{id}")
    public ResponseEntity<OrdenDespachoResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(despachoService.obtener(id));
    }

    /** Asigna o cambia el repartidor (y la movilidad). Solo en PENDIENTE. */
    @PostMapping("/{id}/asignar-repartidor")
    public ResponseEntity<OrdenDespachoResponseDTO> asignarRepartidor(@PathVariable UUID id,
                                                                      @Valid @RequestBody AsignarRepartidorRequestDTO request) {
        return ResponseEntity.ok(despachoService.asignarRepartidor(id, request));
    }

    /** Cambia el estado (EN_CAMINO, FALLIDO o reprogramar a PENDIENTE). ENTREGADO va por /evidencia. */
    @PatchMapping("/{id}/estado")
    public ResponseEntity<OrdenDespachoResponseDTO> cambiarEstado(@PathVariable UUID id,
                                                                  @Valid @RequestBody CambiarEstadoDespachoRequestDTO request) {
        return ResponseEntity.ok(despachoService.cambiarEstado(id, request));
    }

    /**
     * Registra la entrega (multipart): foto obligatoria + quién recibe; marca ENTREGADO.
     * Campos: archivo, recibidoPorNombre, recibidoPorDocumento?, latitud?, longitud?, observaciones?
     */
    @PostMapping(value = "/{id}/evidencia", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrdenDespachoResponseDTO> registrarEntrega(
            @PathVariable UUID id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("recibidoPorNombre") String recibidoPorNombre,
            @RequestParam(value = "recibidoPorDocumento", required = false) String recibidoPorDocumento,
            @RequestParam(value = "latitud", required = false) BigDecimal latitud,
            @RequestParam(value = "longitud", required = false) BigDecimal longitud,
            @RequestParam(value = "observaciones", required = false) String observaciones) {
        EvidenciaEntregaRequestDTO datos = new EvidenciaEntregaRequestDTO(
                recibidoPorNombre, recibidoPorDocumento, latitud, longitud, observaciones);
        return ResponseEntity.ok(despachoService.registrarEntrega(id, archivo, datos));
    }

    /** Descarga la foto de entrega. */
    @GetMapping("/{id}/evidencia")
    public ResponseEntity<Resource> descargarEvidencia(@PathVariable UUID id) {
        ArchivoDescargaDTO archivo = despachoService.descargarEvidencia(id);
        MediaType tipo;
        try {
            tipo = archivo.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(archivo.contentType());
        } catch (Exception e) {
            tipo = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(archivo.nombreOriginal(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.resource());
    }
}
