package com.policalum.erp.application.service.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.application.mapper.ProductoMapper;
import com.policalum.erp.application.service.ProductoService;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ProductoRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

/**
 * Implementación de {@link ProductoService} para la gestión de productos y fracciones.
 * <p>
 * Administra las reglas de negocio de SKU único, normalización de cadenas de texto y
 * el cálculo de importes para los cortes de productos fraccionables (1.0, 0.75, 0.50, 0.25).
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductoServiceImpl implements ProductoService {

    private static final String ENTITY_NAME = "Producto";

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductoResponseDTO create(ProductoRequestDTO requestDTO) {
        validateSkuAvailability(requestDTO.codigoSku(), null);

        Producto producto = productoMapper.toEntity(requestDTO);
        normalize(producto);

        // Aplica el cálculo algorítmico si el producto es fraccionable
        procesarFracciones(producto, requestDTO);

        Producto productoGuardado = productoRepository.save(producto);
        return productoMapper.toResponseDto(productoGuardado);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductoResponseDTO update(UUID id, ProductoRequestDTO requestDTO) {
        Producto producto = findEntityById(id);
        validateSkuAvailability(requestDTO.codigoSku(), id);

        productoMapper.updateEntityFromDto(requestDTO, producto);
        normalize(producto);

        // Recalcula o elimina el catálogo de fracciones según el nuevo estado del DTO
        procesarFracciones(producto, requestDTO);

        Producto productoActualizado = productoRepository.save(producto);
        return productoMapper.toResponseDto(productoActualizado);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public ProductoResponseDTO findById(UUID id) {
        return productoMapper.toResponseDto(findEntityById(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductoResponseDTO> search(String searchTerm, UUID categoriaId, TipoProducto tipo,
                                            Boolean activo, Boolean esFraccionable, Pageable pageable) {
        String normalizedSearchTerm = (searchTerm == null || searchTerm.isBlank())
                ? null
                : searchTerm.trim();

        return productoRepository.search(normalizedSearchTerm, categoriaId, tipo, activo, esFraccionable, pageable)
                .map(productoMapper::toResponseDto);
    }

    /**
     * {@inheritDoc}''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''''
     */
    @Override
    public void delete(UUID id) {
        Producto producto = findEntityById(id);
        productoRepository.delete(producto);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isSkuDisponible(String codigoSku, UUID excludeId) {
        if (codigoSku == null || codigoSku.isBlank()) {
            return true;
        }
        return productoRepository.findByCodigoSkuIgnoreCase(codigoSku.trim())
                .map(producto -> excludeId != null && producto.getId().equals(excludeId))
                .orElse(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalProductos", productoRepository.count());
        statistics.put("productosActivos", productoRepository.countByActivo(true));
        statistics.put("productosInactivos", productoRepository.countByActivo(false));
        statistics.put("productosFraccionables", productoRepository.countByEsFraccionable(true));
        statistics.put("productosFisicos", productoRepository.countByTipo(TipoProducto.FISICO));
        statistics.put("productosServicios", productoRepository.countByTipo(TipoProducto.SERVICIO));
        return statistics;
    }

    // =========================================================================
    // MÉTODOS PRIVADOS Y LÓGICA DE NEGOCIO (FRACCIONES, NORMALIZACIÓN Y VALIDACIÓN)
    // =========================================================================

    /**
     * Administra el catálogo de fracciones de un producto.
     * <p>
     * Limpia la colección anterior (desencadenando {@code orphanRemoval} en Hibernate) y
     * determina si genera la tabla estándar de proporciones o procesa la lista enviada en la petición.
     * </p>
     *
     * @param producto Entidad del producto a modificar.
     * @param requestDTO DTO de origen que contiene los parámetros de fraccionabilidad.
     */
    private void procesarFracciones(Producto producto, ProductoRequestDTO requestDTO) {
        // Limpieza de fracciones anteriores para evitar inconsistencias u huérfanos
        producto.clearFracciones();

        if (!Boolean.TRUE.equals(producto.getEsFraccionable())) {
            return;
        }

        // Si la petición no especifica precios personalizados por fracción, genera el estándar
        if (requestDTO.fracciones() == null || requestDTO.fracciones().isEmpty()) {
            generarFraccionesEstandar(producto);
        } else {
            // Procesa la lista explicita provista en la petición HTTP
            requestDTO.fracciones().forEach(dto -> {
                BigDecimal precioFinal = dto.precioVenta() != null
                        ? dto.precioVenta()
                        : calcularPrecioFraccion(producto.getPrecioVentaBase(), dto.factorDescuentoKardex());

                CatalogoFraccion fraccion = CatalogoFraccion.builder()
                        .nombreFraccion(dto.nombreFraccion())
                        .factorDescuentoKardex(dto.factorDescuentoKardex())
                        .precioVenta(precioFinal)
                        .activo(dto.activo() != null ? dto.activo() : true)
                        .build();

                producto.addFraccion(fraccion);
            });
        }
    }

    /**
     * Genera la lista estándar de cortes basada en factores comunes (1.0000, 0.7500, 0.5000, 0.2500)
     * calculando su precio proporcional en base al {@code precioVentaBase} del producto.
     *
     * @param producto Entidad a la que se le asociarán las fracciones generadas.
     */
    private void generarFraccionesEstandar(Producto producto) {
        List<FraccionEstandar> cortesEstandar = List.of(
                new FraccionEstandar("Plancha Completa", new BigDecimal("1.0000")),
                new FraccionEstandar("3/4 Plancha", new BigDecimal("0.7500")),
                new FraccionEstandar("Media Plancha (1/2)", new BigDecimal("0.5000")),
                new FraccionEstandar("1/4 Plancha", new BigDecimal("0.2500"))
        );

        for (FraccionEstandar corte : cortesEstandar) {
            BigDecimal precioVenta = calcularPrecioFraccion(producto.getPrecioVentaBase(), corte.factor());

            CatalogoFraccion fraccion = CatalogoFraccion.builder()
                    .nombreFraccion(corte.nombre())
                    .factorDescuentoKardex(corte.factor())
                    .precioVenta(precioVenta)
                    .activo(true)
                    .build();

            producto.addFraccion(fraccion);
        }
    }

    /**
     * Realiza el cálculo del precio multiplicando el precio base por el factor de Kardex,
     * redondeando el resultado a 2 decimales según la regla estándar bancaria (HALF_UP).
     *
     * @param precioBase Precio unitario de la presentación entera.
     * @param factor Factor de descuento en Kardex.
     * @return Importe calculado del corte en {@link BigDecimal}.
     */
    private BigDecimal calcularPrecioFraccion(BigDecimal precioBase, BigDecimal factor) {
        if (precioBase == null || factor == null) {
            return BigDecimal.ZERO;
        }
        return precioBase.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Busca la entidad {@link Producto} por su ID garantizando que se carguen sus relaciones.
     *
     * @param id Identificador único a buscar.
     * @return Entidad {@link Producto}.
     * @throws ResourceNotFoundException Si no se encuentra el registro en base de datos.
     */
    private Producto findEntityById(UUID id) {
        return productoRepository.findWithRelationsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("%s no encontrado con ID: %s", ENTITY_NAME, id)));
    }

    /**
     * Valida que el SKU proporcionado sea único en la base de datos.
     *
     * @param codigoSku SKU a validar.
     * @param excludeId ID del producto actual si es una modificación (null si es creación).
     * @throws ValidationException Si el SKU ya está en uso por otro producto.
     */
    private void validateSkuAvailability(String codigoSku, UUID excludeId) {
        if (!isSkuDisponible(codigoSku, excludeId)) {
            throw new ValidationException("Ya existe un producto con el SKU indicado");
        }
    }

    /**
     * Recorta espacios sobrantes en las propiedades de tipo texto y estandariza la unidad a mayúsculas.
     *
     * @param producto Entidad a normalizar.
     */
    private void normalize(Producto producto) {
        if (producto.getCodigoSku() != null) {
            producto.setCodigoSku(producto.getCodigoSku().trim());
        }
        if (producto.getNombre() != null) {
            producto.setNombre(producto.getNombre().trim());
        }
        if (producto.getUnidad() != null) {
            producto.setUnidad(producto.getUnidad().trim().toUpperCase());
        }
    }

    /**
     * Record interno para estructurar los cortes estándar durante la generación algorítmica.
     */
    private record FraccionEstandar(String nombre, BigDecimal factor) {}
}