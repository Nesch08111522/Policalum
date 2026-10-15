package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.CuentaFinancieraRequestDTO;
import com.policalum.erp.application.dto.request.RegistrarSaldoCuentaRequestDTO;
import com.policalum.erp.application.dto.response.CuentaFinancieraResponseDTO;
import com.policalum.erp.application.dto.response.SaldoCuentaResponseDTO;
import com.policalum.erp.application.service.CuentaFinancieraService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

/** Cuentas de la empresa (cajas, bancos, billeteras) y sus saldos de corte (Módulo 6). Solo ADMIN. */
@Tag(name = "Finanzas")
@RestController
@RequestMapping("/api/v1/finanzas/cuentas")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CuentaFinancieraController {

    private final CuentaFinancieraService cuentaService;

    /** Crea una cuenta. 409 si un método de pago ya está en otra cuenta de la misma sede. */
    @PostMapping
    public ResponseEntity<CuentaFinancieraResponseDTO> crear(@Valid @RequestBody CuentaFinancieraRequestDTO request) {
        CuentaFinancieraResponseDTO creada = cuentaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(location).body(creada);
    }

    /** Lista cuentas (por defecto solo activas) con su último saldo registrado. */
    @GetMapping
    public ResponseEntity<List<CuentaFinancieraResponseDTO>> listar(
            @RequestParam(required = false) UUID sedeId,
            @RequestParam(defaultValue = "false") boolean incluirInactivas) {
        return ResponseEntity.ok(cuentaService.listar(sedeId, incluirInactivas));
    }

    /** Detalle de una cuenta. */
    @GetMapping("/{id}")
    public ResponseEntity<CuentaFinancieraResponseDTO> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(cuentaService.obtener(id));
    }

    /** Actualiza datos y métodos de pago de una cuenta activa. */
    @PutMapping("/{id}")
    public ResponseEntity<CuentaFinancieraResponseDTO> actualizar(@PathVariable UUID id,
                                                                  @Valid @RequestBody CuentaFinancieraRequestDTO request) {
        return ResponseEntity.ok(cuentaService.actualizar(id, request));
    }

    /** Desactiva la cuenta (conserva su historial). 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        cuentaService.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    /** Registra el saldo real de la cuenta a una fecha (corte). 201 + Location del historial. */
    @PostMapping("/{id}/saldos")
    public ResponseEntity<SaldoCuentaResponseDTO> registrarSaldo(@PathVariable UUID id,
                                                                @Valid @RequestBody RegistrarSaldoCuentaRequestDTO request) {
        SaldoCuentaResponseDTO saldo = cuentaService.registrarSaldo(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(saldo);
    }

    /** Historial de saldos de la cuenta, del más reciente al más antiguo. */
    @GetMapping("/{id}/saldos")
    public ResponseEntity<Page<SaldoCuentaResponseDTO>> historialSaldos(@PathVariable UUID id,
                                                                       @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(cuentaService.historialSaldos(id, pageable));
    }
}
