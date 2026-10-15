package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.policalum.erp.application.dto.request.RegistrarVentaRequestDTO;
import com.policalum.erp.application.dto.request.AnularVentaRequestDTO;
import com.policalum.erp.application.dto.response.VentaDetalleResponseDTO;
import com.policalum.erp.application.dto.response.VentaListadoResponseDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.service.VentaService;
import com.policalum.erp.domain.enums.CanalVenta;
import com.policalum.erp.domain.enums.EstadoVenta;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Ventas")
@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {
    private final VentaService ventaService;

    @Operation(summary = "Registrar venta", description = "Registra una venta COMPLETADA con sus líneas (producto, fracción o servicio) y pagos. Requiere un turno de caja ABIERTO del usuario en la sede. Los precios e IGV los calcula el servidor; la suma de pagos debe ser igual al total. Descuenta stock con una salida de Kardex.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "400", description = "Validación de campos, pagos que no suman el total, producto inactivo o stock insuficiente"),
            @ApiResponse(responseCode = "404", description = "Sede, almacén, turno, cliente, producto, fracción o servicio inexistente"),
            @ApiResponse(responseCode = "409", description = "El turno no está abierto o no pertenece al usuario/sede")
    })
    @PostMapping
    public ResponseEntity<VentaProcesadaResponseDTO> registrar(@Valid @RequestBody RegistrarVentaRequestDTO request) {
        VentaProcesadaResponseDTO venta = ventaService.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(venta.id()).toUri();
        return ResponseEntity.created(location).body(venta);
    }

    @Operation(summary = "Detalle de venta", description = "Venta con líneas y pagos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<VentaDetalleResponseDTO> obtener(@PathVariable UUID id) { return ResponseEntity.ok(ventaService.obtener(id)); }

    @Operation(summary = "Buscar ventas", description = "Listado paginado. Filtros opcionales: sede, vendedor, cliente, estado, canal y rango de fecha-hora (ISO date-time).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping
    public ResponseEntity<Page<VentaListadoResponseDTO>> buscar(
            @RequestParam(required = false) UUID sedeId, @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) UUID clienteId, @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) CanalVenta canal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ventaService.buscar(sedeId, userId, clienteId, estado, canal, desde, hasta, pageable));
    }

    /** Completed fiscal sales are immutable; this endpoint reverses stock and preserves the audit trail. */
    @Operation(summary = "Anular venta", description = "Marca la venta ANULADA y reingresa al Kardex todo lo que salió. Solo el vendedor de la venta o un ADMIN. No se permite si la venta tiene devoluciones PENDIENTES o APROBADAS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Venta no encontrada"),
            @ApiResponse(responseCode = "409", description = "Ya anulada, tiene devoluciones vigentes o el usuario no es el vendedor ni ADMIN")
    })
    @PostMapping("/{id}/anulacion")
    public ResponseEntity<VentaProcesadaResponseDTO> anular(@PathVariable UUID id, @Valid @RequestBody AnularVentaRequestDTO request) {
        return ResponseEntity.ok(ventaService.anular(id, request));
    }
}
