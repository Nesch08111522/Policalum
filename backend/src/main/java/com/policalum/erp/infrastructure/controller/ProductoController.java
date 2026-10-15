package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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

import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.application.service.ProductoService;
import com.policalum.erp.domain.enums.TipoProducto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Controlador REST para la gestión integral de productos y su catálogo de fracciones.
 */
@Tag(name = "Catálogo")
@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Registra un nuevo producto en el sistema.
     *
     * @param requestDTO Datos del producto a crear.
     * @return Producto creado y URI en cabecera Location.
     */
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> create(@Valid @RequestBody ProductoRequestDTO requestDTO) {
        ProductoResponseDTO response = productoService.create(requestDTO);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Actualiza la información de un producto existente.
     *
     * @param id Identificador único del producto.
     * @param requestDTO Datos actualizados del producto.
     * @return Producto actualizado.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductoRequestDTO requestDTO) {
        return ResponseEntity.ok(productoService.update(id, requestDTO));
    }

    /**
     * Obtiene el detalle de un producto por su ID.
     *
     * @param id Identificador único del producto.
     * @return Detalle del producto encontrado.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductoResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(productoService.findById(id));
    }

    /**
     * Consulta paginada y filtrada del catálogo de productos.
     *
     * @param searchTerm Término de búsqueda por nombre o SKU (opcional).
     * @param categoriaId Filtro por ID de categoría (opcional).
     * @param tipo Filtro por tipo de producto (opcional).
     * @param activo Filtro por estado del producto (opcional).
     * @param esFraccionable Filtro por estado de fraccionabilidad (opcional).
     * @param pageable Configuración de paginación (page, size, sort).
     * @return Página con los productos filtrados.
     */
    @GetMapping
    public ResponseEntity<Page<ProductoResponseDTO>> search(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) TipoProducto tipo,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean esFraccionable,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<ProductoResponseDTO> result = productoService.search(
                searchTerm, categoriaId, tipo, activo, esFraccionable, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Elimina un producto por su ID.
     *
     * @param id Identificador único del producto a eliminar.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productoService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Verifica la disponibilidad de un código SKU.
     *
     * @param codigoSku Código SKU a verificar.
     * @param excludeId ID a excluir en caso de actualización (opcional).
     * @return Map indicando si el SKU está disponible.
     */
    @GetMapping("/check-sku")
    public ResponseEntity<Map<String, Boolean>> isSkuDisponible(
            @RequestParam String codigoSku,
            @RequestParam(required = false) UUID excludeId) {

        boolean disponible = productoService.isSkuDisponible(codigoSku, excludeId);
        return ResponseEntity.ok(Map.of("disponible", disponible));
    }

    /**
     * Obtiene las estadísticas globales del catálogo para el dashboard.
     *
     * @return Métricas resumidas del inventario.
     */
    @GetMapping("/dashboard/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        return ResponseEntity.ok(productoService.getDashboardStatistics());
    }
}