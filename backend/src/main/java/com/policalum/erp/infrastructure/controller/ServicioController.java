package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.dto.response.ServicioResponseDTO;
import com.policalum.erp.application.service.ServicioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
@Tag(name = "Catálogo")
@RestController
@RequestMapping("/api/v1/servicios")
@RequiredArgsConstructor
public class ServicioController {
    private final ServicioService servicioService;

    /**
     * Registra un nuevo servicio en el catálogo.
     *
     * @param requestDTO Datos del servicio a crear.
     * @return Servicio creado y cabecera Location con la URI del recurso.
     */
    @PostMapping
    public ResponseEntity<ServicioResponseDTO> create(@Valid @RequestBody ServicioRequestDTO requestDTO) {
        ServicioResponseDTO response = servicioService.create(requestDTO);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Actualiza la información de un servicio por su ID.
     *
     * @param id UUID del servicio.
     * @param requestDTO Nuevos datos del servicio.
     * @return Servicio actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ServicioRequestDTO requestDTO) {
        return ResponseEntity.ok(servicioService.update(id, requestDTO));
    }

    /**
     * Obtiene los detalles de un servicio específico.
     *
     * @param id UUID del servicio.
     * @return Detalle del servicio.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(servicioService.findById(id));
    }

    /**
     * Consulta paginada y filtrada del catálogo de servicios.
     *
     * @param searchTerm Término de búsqueda por nombre o descripción (opcional).
     * @param activo Estado activo/inactivo (opcional).
     * @param precioMin Filtro de precio mínimo (opcional).
     * @param precioMax Filtro de precio máximo (opcional).
     * @param pageable Paginación y ordenamiento por defecto.
     * @return Página de servicios.
     */
    @GetMapping
    public ResponseEntity<Page<ServicioResponseDTO>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) BigDecimal precioMin,
            @RequestParam(required = false) BigDecimal precioMax,
            @PageableDefault(size = 20, sort = "nombre") Pageable pageable) {

        return ResponseEntity.ok(servicioService.search(searchTerm, activo, precioMin, precioMax, pageable));
    }

    /**
     * Desactiva (borrado lógico) un servicio por su ID.
     *
     * @param id UUID del servicio a desactivar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        servicioService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Valida la disponibilidad de un nombre de servicio para prevenir duplicados.
     */
    @GetMapping("/check-nombre")
    public ResponseEntity<Map<String, Boolean>> isNombreDisponible(
            @RequestParam String nombre,
            @RequestParam(required = false) UUID excludeId) {

        boolean disponible = servicioService.isNombreDisponible(nombre, excludeId);
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    /**
     * Obtiene estadísticas resumidas de los servicios registrados.
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        return ResponseEntity.ok(servicioService.getDashboardStatistics());
    }
}
