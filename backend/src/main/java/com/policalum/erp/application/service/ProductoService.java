package com.policalum.erp.application.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.domain.enums.TipoProducto;

/**
 * Servicio de aplicación para la gestión integral de Productos y su Catálogo de Fracciones (M-03).
 * <p>
 * Proporciona métodos para el mantenimiento CRUD de productos, validación de disponibilidad de SKU,
 * cálculo algorítmico de precios por fracción y estadísticas de inventario.
 * </p>
 */
public interface ProductoService {

    /**
     * Registra un nuevo producto en el sistema.
     * <p>
     * Si el producto es marcado como fraccionable ({@code esFraccionable = true}),
     * se generará o procesará automáticamente su catálogo de fracciones según las reglas de negocio.
     * </p>
     *
     * @param requestDTO Datos del producto a crear.
     * @return {@link ProductoResponseDTO} con los datos del producto creado y sus fracciones asignadas.
     * @throws jakarta.validation.ValidationException Si el SKU ya se encuentra registrado.
     */
    ProductoResponseDTO create(ProductoRequestDTO requestDTO);

    /**
     * Actualiza la información de un producto existente por su ID.
     * <p>
     * Reejecuta el procesamiento de fracciones: en caso de cambiar el precio de venta base
     * o la fraccionabilidad del producto, se limpiarán y recalcularán sus cortes asociados.
     * </p>

     * @param id Identificador único del producto a actualizar.
     * @param requestDTO Datos actualizados del producto.
     * @return {@link ProductoResponseDTO} con los datos actualizados.
     * @throws com.policalum.erp.exception.ResourceNotFoundException Si el producto no existe.
     * @throws jakarta.validation.ValidationException Si el nuevo SKU entra en conflicto con otro producto.
     */
    ProductoResponseDTO update(UUID id, ProductoRequestDTO requestDTO);

    /**
     * Busca un producto por su identificador único, incluyendo sus relaciones (Categoría, Proveedor y Fracciones).
     *
     * @param id Identificador único del producto.
     * @return {@link ProductoResponseDTO} con la información detallada del producto.
     * @throws com.policalum.erp.exception.ResourceNotFoundException Si no se encuentra el producto.
     */
    ProductoResponseDTO findById(UUID id);

    /**
     * Consulta paginada y filtrada de productos.
     *
     * @param searchTerm Término de búsqueda para coincidencia por Nombre o SKU (opcional).
     * @param categoriaId Identificador de la categoría para filtrar (opcional).
     * @param tipo Tipo de producto (FÍSICO o SERVICIO) (opcional).
     * @param activo Estado lógico del producto (opcional).
     * @param esFraccionable Indicador de fraccionabilidad (opcional).
     * @param pageable Configuración de paginación y ordenamiento.
     * @return Pauta paginada {@link Page} de {@link ProductoResponseDTO}.
     */
    Page<ProductoResponseDTO> search(String searchTerm, UUID categoriaId, TipoProducto tipo,
                                     Boolean activo, Boolean esFraccionable, Pageable pageable);

    /**
     * Elimina un producto por su ID. Las fracciones asociadas se eliminarán en cascada.
     *
     * @param id Identificador único del producto a eliminar.
     * @throws com.policalum.erp.exception.ResourceNotFoundException Si el producto no existe.
     */
    void delete(UUID id);

    /**
     * Verifica si un código SKU está disponible para su registro o actualización.
     *
     * @param codigoSku Código SKU a verificar.
     * @param excludeId ID del producto a excluir durante la validación (usado en actualizaciones).
     * @return {@code true} si el SKU está disponible o es nulo/vacío; {@code false} en caso contrario.
     */
    boolean isSkuDisponible(String codigoSku, UUID excludeId);

    /**
     * Obtiene métricas globales resumidas para el panel de control de inventario/catálogo.
     *
     * @return {@link Map} con pares clave-valor de los totales y métricas de productos.
     */
    Map<String, Object> getDashboardStatistics();
}