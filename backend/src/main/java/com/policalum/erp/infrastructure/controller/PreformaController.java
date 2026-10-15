package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.policalum.erp.application.dto.request.CancelarPreformaRequestDTO;
import com.policalum.erp.application.dto.request.ConfirmarPreformaRequestDTO;
import com.policalum.erp.application.dto.request.PreformaVentaRequestDTO;
import com.policalum.erp.application.dto.response.ConfirmacionPreformaDTO;
import com.policalum.erp.application.dto.response.PreformaDashboardDTO;
import com.policalum.erp.application.dto.response.PreformaListadoDTO;
import com.policalum.erp.application.dto.response.PreformaVentaResponseDTO;
import com.policalum.erp.application.service.PreformaService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Preformas de venta. Todo el personal puede crear y consultar; editar, eliminar y cancelar
 * solo el creador o el ADMIN; confirmar, cualquiera con turno de caja abierto en la sede.
 */
@Tag(name = "Preformas")
@RestController
@RequestMapping("/api/v1/preformas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','MULTIFUNCIONAL')")
public class PreformaController {

    private final PreformaService preformaService;

    /** Crea una preforma ABIERTA (no requiere turno). 201 + Location. */
    @Operation(summary = "Crear preforma", description = "Crea una preforma ABIERTA (carrito de mostrador). No requiere turno ni descuenta stock. Vigencia en minutos dentro del rango de Configuración > ventas (5 a 4320; 120 por defecto).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "404", description = "Sede, cliente, producto, fracción o servicio inexistente")
    })
    @PostMapping
    public ResponseEntity<PreformaVentaResponseDTO> crear(@Valid @RequestBody PreformaVentaRequestDTO request) {
        PreformaVentaResponseDTO creada = preformaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /** Preformas ABIERTAS vigentes del usuario actual. */
    @Operation(summary = "Mis preformas abiertas", description = "Preformas ABIERTAS y vigentes del usuario actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/abiertas")
    public ResponseEntity<List<PreformaListadoDTO>> abiertas() {
        return ResponseEntity.ok(preformaService.abiertasDelUsuario());
    }

    /** Bandeja de EXPIRADAS (ADMIN: todas; resto: las propias). */
    @Operation(summary = "Preformas vencidas", description = "Bandeja de EXPIRADAS: ADMIN ve todas; MULTIFUNCIONAL solo las propias.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/vencidas")
    public ResponseEntity<Page<PreformaListadoDTO>> vencidas(
            @RequestParam(required = false) UUID sedeId,
            @PageableDefault(size = 20, sort = "fechaExpiracion", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(preformaService.vencidas(sedeId, pageable));
    }

    /** KPIs del periodo (por defecto, mes en curso). */
    @Operation(summary = "Dashboard de preformas", description = "KPIs del periodo (mes en curso por defecto): abiertas, confirmadas, canceladas, expiradas y conversión. Solo ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PreformaDashboardDTO> dashboard(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(preformaService.dashboard(sedeId, desde, hasta));
    }

    /** Detalle (si venció, se devuelve ya como EXPIRADA). */
    @Operation(summary = "Detalle de preforma", description = "Si ya venció se devuelve como EXPIRADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Preforma no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PreformaVentaResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(preformaService.obtener(id));
    }

    /** Edita y recalcula precios (solo ABIERTA vigente). */
    @Operation(summary = "Editar preforma", description = "Edita líneas y recalcula precios. Solo preformas ABIERTAS y vigentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Preforma no encontrada"),
            @ApiResponse(responseCode = "409", description = "La preforma no está ABIERTA o expiró")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PreformaVentaResponseDTO> actualizar(@PathVariable UUID id,
                                                               @Valid @RequestBody PreformaVentaRequestDTO request) {
        return ResponseEntity.ok(preformaService.actualizar(id, request));
    }

    /** Elimina una preforma ABIERTA. 204. */
    @Operation(summary = "Eliminar preforma", description = "Elimina una preforma ABIERTA.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Sin contenido"),
            @ApiResponse(responseCode = "404", description = "Preforma no encontrada"),
            @ApiResponse(responseCode = "409", description = "La preforma no está ABIERTA")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        preformaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Convierte en venta con el turno abierto de quien confirma. 201 + Location de la venta. */
    @Operation(summary = "Confirmar preforma (convertir en venta)", description = "Registra la venta con el turno de caja abierto de quien confirma (mismas reglas que POST /ventas). Devuelve 201 con Location de la venta.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Creado (header Location con la URL del recurso)"),
            @ApiResponse(responseCode = "404", description = "Preforma, almacén o turno inexistente"),
            @ApiResponse(responseCode = "409", description = "La preforma no está ABIERTA, expiró o no hay turno abierto")
    })
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<ConfirmacionPreformaDTO> confirmar(@PathVariable UUID id,
                                                             @Valid @RequestBody ConfirmarPreformaRequestDTO request) {
        ConfirmacionPreformaDTO resultado = preformaService.confirmar(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/ventas/{id}").buildAndExpand(resultado.venta().id()).toUri();
        return ResponseEntity.created(location).body(resultado);
    }

    /** Cancela (motivo opcional). No toca stock ni caja. */
    @Operation(summary = "Cancelar preforma", description = "ABIERTA → CANCELADA con motivo opcional. No toca stock ni caja.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "404", description = "Preforma no encontrada"),
            @ApiResponse(responseCode = "409", description = "La preforma no está ABIERTA")
    })
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PreformaVentaResponseDTO> cancelar(@PathVariable UUID id,
                                                             @Valid @RequestBody(required = false) CancelarPreformaRequestDTO request) {
        return ResponseEntity.ok(preformaService.cancelar(id, request));
    }
}
