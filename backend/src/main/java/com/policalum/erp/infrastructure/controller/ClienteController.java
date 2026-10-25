package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.ClienteRequestDTO;
import com.policalum.erp.application.dto.response.ClienteResponseDTO;
import com.policalum.erp.application.service.ClienteService;
import com.policalum.erp.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Clientes")
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Slf4j
public class ClienteController {

    private final ClienteService clienteService;

    // ============ CRUD OPERATIONS ============

    @PostMapping
    public ResponseEntity<ClienteResponseDTO> createCliente(@Valid @RequestBody ClienteRequestDTO requestDTO) {
        log.info("POST /api/v1/clientes - Creando nuevo cliente con documento: {}", requestDTO.numeroDocumento());

        try {
            ClienteResponseDTO response = clienteService.create(requestDTO);
            log.info("Cliente creado exitosamente con ID: {}", response.id());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Error al crear cliente: {}", e.getMessage());
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> updateCliente(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteRequestDTO requestDTO) {
        log.info("PUT /api/v1/clientes/{} - Actualizando cliente", id);

        if (id == null) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo");
        }

        ClienteResponseDTO response = clienteService.update(id, requestDTO);
        log.info("Cliente actualizado exitosamente: {}", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> getClienteById(@PathVariable UUID id) {
        log.debug("GET /api/v1/clientes/{} - Buscando cliente por ID", id);

        if (id == null) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo");
        }

        ClienteResponseDTO response = clienteService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/documento/{numeroDocumento}")
    public ResponseEntity<ClienteResponseDTO> getClienteByDocumento(@PathVariable String numeroDocumento) {
        log.debug("GET /api/v1/clientes/documento/{} - Buscando cliente por documento", numeroDocumento);

        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de documento no puede estar vacío");
        }

        ClienteResponseDTO response = clienteService.findByNumeroDocumento(numeroDocumento.trim());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ClienteResponseDTO> deleteCliente(@PathVariable UUID id) {
        log.info("DELETE /api/v1/clientes/{} - Eliminando cliente", id);

        if (id == null) {
            throw new IllegalArgumentException("El ID del cliente no puede ser nulo");
        }

        ClienteResponseDTO respuesta = clienteService.delete(id);
        if (respuesta == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(respuesta);
    }

    // ============ SEARCH & FILTER ============

    @GetMapping
    public ResponseEntity<Page<ClienteResponseDTO>> searchClientes(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String tipoDocumento,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.debug("GET /api/v1/clientes - Buscando clientes con término: '{}', tipo: '{}'", searchTerm, tipoDocumento);

        // Validar tipoDocumento si viene
        if (tipoDocumento != null && !tipoDocumento.isEmpty()) {
            // Validar que el tipo de documento sea válido
            if (!isValidTipoDocumento(tipoDocumento)) {
                throw new IllegalArgumentException("Tipo de documento no válido: " + tipoDocumento);
            }
            Page<ClienteResponseDTO> response = clienteService.findByTipoDocumento(tipoDocumento, pageable);
            return ResponseEntity.ok(response);
        }

        Page<ClienteResponseDTO> response = clienteService.search(searchTerm, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recientes")
    public ResponseEntity<List<ClienteResponseDTO>> getRecentClientes() {
        log.debug("GET /api/v1/clientes/recientes - Obteniendo clientes recientes");
        List<ClienteResponseDTO> response = clienteService.findTop10Recent();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/incompletos")
    public ResponseEntity<List<ClienteResponseDTO>> getClientesIncompletos() {
        log.debug("GET /api/v1/clientes/incompletos - Obteniendo clientes con datos incompletos");
        List<ClienteResponseDTO> response = clienteService.findClientesWithIncompleteData();
        return ResponseEntity.ok(response);
    }

    // ============ BUSINESS CONSULTATIONS ============

    @GetMapping("/compras/activos")
    public ResponseEntity<List<ClienteResponseDTO>> getClientesWithPurchases(
            @RequestParam(required = false) LocalDateTime fechaDesde) {
        log.debug("GET /api/v1/clientes/compras/activos - Obteniendo clientes con compras desde: {}", fechaDesde);

        LocalDateTime fecha = fechaDesde != null ? fechaDesde : LocalDateTime.now().minusMonths(3);

        // Validar que la fecha no sea futura
        if (fecha.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha no puede ser futura");
        }

        List<ClienteResponseDTO> response = clienteService.findClientesWithPurchasesSince(fecha);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/compras/inactivos")
    public ResponseEntity<List<ClienteResponseDTO>> getClientesWithoutPurchases(
            @RequestParam(required = false) LocalDateTime fechaDesde) {
        log.debug("GET /api/v1/clientes/compras/inactivos - Obteniendo clientes sin compras desde: {}", fechaDesde);

        LocalDateTime fecha = fechaDesde != null ? fechaDesde : LocalDateTime.now().minusMonths(6);

        // Validar que la fecha no sea futura
        if (fecha.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha no puede ser futura");
        }

        List<ClienteResponseDTO> response = clienteService.findClientesWithoutPurchasesSince(fecha);
        return ResponseEntity.ok(response);
    }

    // ============ STATISTICS & REPORTS ============

    @GetMapping("/estadisticas/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        log.debug("GET /api/v1/clientes/estadisticas/dashboard - Obteniendo estadísticas para dashboard");
        Map<String, Object> response = clienteService.getDashboardStatistics();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estadisticas/tipo-documento")
    public ResponseEntity<Map<String, Long>> getDistribucionPorTipoDocumento() {
        log.debug("GET /api/v1/clientes/estadisticas/tipo-documento - Obteniendo distribución por tipo de documento");
        Map<String, Long> response = clienteService.getEstadisticasPorTipoDocumento();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/estadisticas/count")
    public ResponseEntity<Map<String, Long>> countTotalClientes() {
        log.debug("GET /api/v1/clientes/estadisticas/count - Contando clientes totales");
        long total = clienteService.countTotalClientes();
        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping("/estadisticas/hoy")
    public ResponseEntity<Map<String, Long>> countClientesRegistradosHoy() {
        log.debug("GET /api/v1/clientes/estadisticas/hoy - Contando clientes registrados hoy");
        long count = clienteService.countClientesRegistradosHoy();
        return ResponseEntity.ok(Map.of("registradosHoy", count));
    }

    // ============ VALIDATIONS ============

    @GetMapping("/validar/documento")
    public ResponseEntity<Map<String, Boolean>> validateDocumento(
            @RequestParam String numeroDocumento,
            @RequestParam(required = false) UUID excludeId) {
        log.debug("GET /api/v1/clientes/validar/documento - Validando documento: {}, excluyendo: {}",
                numeroDocumento, excludeId);

        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            throw new IllegalArgumentException("El número de documento es requerido");
        }

        boolean disponible = clienteService.validateDocumentoUniqueness(numeroDocumento.trim(), excludeId);
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    @GetMapping("/validar/email")
    public ResponseEntity<Map<String, Boolean>> validateEmail(@RequestParam String email) {
        log.debug("GET /api/v1/clientes/validar/email - Validando email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es requerido");
        }

        // Validar formato de email básico
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Formato de email no válido: " + email);
        }

        boolean disponible = !clienteService.existsByEmail(email.trim());
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    // ============ PRIVATE METHODS ============

    /**
     * Valida si el tipo de documento es válido
     */
    private boolean isValidTipoDocumento(String tipoDocumento) {
        if (tipoDocumento == null) return false;
        String tipo = tipoDocumento.trim().toUpperCase();
        return tipo.equals("DNI") || tipo.equals("RUC") ||
                tipo.equals("CE") || tipo.equals("PASAPORTE");
    }

    /**
     * Valida el formato del email (validación básica)
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email.trim().matches(emailRegex);
    }
}