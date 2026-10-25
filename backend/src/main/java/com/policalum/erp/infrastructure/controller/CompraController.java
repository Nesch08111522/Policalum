package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.policalum.erp.application.dto.request.AnularCompraRequestDTO;
import com.policalum.erp.application.dto.request.CompraRequestDTO;
import com.policalum.erp.application.dto.request.RecepcionCompraRequestDTO;
import com.policalum.erp.application.dto.response.CompraAdjuntoResponseDTO;
import com.policalum.erp.application.dto.response.CompraListadoDTO;
import com.policalum.erp.application.dto.response.CompraResponseDTO;
import com.policalum.erp.application.service.CompraAdjuntoService;
import com.policalum.erp.application.service.CompraService;
import com.policalum.erp.domain.enums.EstadoCompra;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Compras")
@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
public class CompraController {

    private final CompraService compraService;
    private final CompraAdjuntoService adjuntoService;

    // ============ COMPRAS ============

    @Operation(summary = "Registrar compra", description = "En una sola transacción crea la compra, la entrada de kardex, el stock del almacén y la cuenta por pagar. Si algo falla, no queda ninguno de esos registros. Queda en estado RECIBIDA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "404", description = "Proveedor, sede, producto, fracción o almacén inexistente")
    })
    @PostMapping
    public ResponseEntity<CompraResponseDTO> crear(@Valid @RequestBody CompraRequestDTO dto) {
        CompraResponseDTO response = compraService.crear(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Editar compra", description = "Reemplaza los datos y líneas de una compra en BORRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "409", description = "La compra ya no está en BORRADOR")
    })
    @PutMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> actualizar(
            @PathVariable UUID id, @Valid @RequestBody CompraRequestDTO dto) {
        return ResponseEntity.ok(compraService.actualizar(id, dto));
    }

    @Operation(summary = "Detalle de compra", description = "Compra con líneas, estado de recepción y de pago.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(compraService.obtener(id));
    }

    @Operation(summary = "Buscar compras", description = "Listado paginado por proveedor, sede, estado y fecha de emisión (yyyy-MM-dd).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping
    public ResponseEntity<Page<CompraListadoDTO>> buscar(
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) EstadoCompra estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(compraService.buscar(proveedorId, sedeId, estado, desde, hasta, pageable));
    }

    @Operation(summary = "Recibir compra", description = "BORRADOR → RECIBIDA: registra la ENTRADA en Kardex de cada línea, recalcula el costo promedio ponderado del producto y crea la cuenta por pagar. Body opcional: número de factura del proveedor y observaciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "409", description = "Solo se reciben compras en BORRADOR o la compra no tiene líneas")
    })
    @PostMapping("/{id}/recepcion")
    public ResponseEntity<CompraResponseDTO> recibir(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) RecepcionCompraRequestDTO dto) {
        return ResponseEntity.ok(compraService.recibir(id, dto));
    }

    @Operation(summary = "Anular compra", description = "BORRADOR → ANULADA con motivo. Una compra RECIBIDA no se anula (requiere devolución al proveedor).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "409", description = "La compra ya está anulada o ya fue RECIBIDA")
    })
    @PostMapping("/{id}/anulacion")
    public ResponseEntity<CompraResponseDTO> anular(
            @PathVariable UUID id, @Valid @RequestBody AnularCompraRequestDTO dto) {
        return ResponseEntity.ok(compraService.anular(id, dto));
    }

    @Operation(summary = "Eliminar compra", description = "Elimina una compra en BORRADOR.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sin contenido"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada"),
            @ApiResponse(responseCode = "409", description = "Solo se eliminan compras en BORRADOR")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        compraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Dashboard de compras", description = "Contadores y totales de compras por estado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(compraService.dashboard());
    }

    // ============ ADJUNTOS ============

    @Operation(summary = "Subir adjunto", description = "Sube un archivo (multipart: archivo, tipo = FACTURA | GUIA_REMISION | ORDEN_COMPRA | OTRO). Extensiones pdf, jpg, jpeg, png, webp; máximo 10 MB.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    })
    @PostMapping(value = "/{id}/adjuntos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompraAdjuntoResponseDTO> subirAdjunto(
            @PathVariable UUID id,
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "tipo", defaultValue = "OTRO") String tipo) {
        return ResponseEntity.status(201).body(adjuntoService.subir(id, archivo, tipo));
    }

    @Operation(summary = "Listar adjuntos", description = "Adjuntos de la compra.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Compra no encontrada")
    })
    @GetMapping("/{id}/adjuntos")
    public ResponseEntity<List<CompraAdjuntoResponseDTO>> listarAdjuntos(@PathVariable UUID id) {
        return ResponseEntity.ok(adjuntoService.listar(id));
    }

    @Operation(summary = "Descargar adjunto", description = "Descarga el archivo (application/octet-stream).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Compra o adjunto no encontrado")
    })
    @GetMapping("/{id}/adjuntos/{adjuntoId}")
    public ResponseEntity<Resource> descargarAdjunto(
            @PathVariable UUID id, @PathVariable UUID adjuntoId) {
        Resource resource = adjuntoService.descargar(id, adjuntoId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @Operation(summary = "Eliminar adjunto", description = "Elimina el adjunto y su archivo.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sin contenido"),
            @ApiResponse(responseCode = "404", description = "Compra o adjunto no encontrado")
    })
    @DeleteMapping("/{id}/adjuntos/{adjuntoId}")
    public ResponseEntity<Void> eliminarAdjunto(
            @PathVariable UUID id, @PathVariable UUID adjuntoId) {
        adjuntoService.eliminar(id, adjuntoId);
        return ResponseEntity.noContent().build();
    }
}