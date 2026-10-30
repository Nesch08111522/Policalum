package com.policalum.erp.application.service.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.application.mapper.ProductoMapper;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.ProductoService;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CatalogoFraccionRepository;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.ProveedorRepository;

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
    private final CatalogoFraccionRepository catalogoFraccionRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoMapper productoMapper;
    private final FileStorageService fileStorageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductoResponseDTO create(ProductoRequestDTO requestDTO) {
        validateSkuAvailability(requestDTO.codigoSku(), null);

        Producto producto = productoMapper.toEntity(requestDTO);
        resolverReferencias(producto, requestDTO);
        if (producto.getDestacado() == null) producto.setDestacado(false);
        normalize(producto);

        procesarFracciones(producto, requestDTO, null);

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
        String imagenAnterior = producto.getImagen();
        BigDecimal precioAnterior = producto.getPrecioVentaBase();
        boolean destacadoPrevio = Boolean.TRUE.equals(producto.getDestacado());

        productoMapper.updateEntityFromDto(requestDTO, producto);
        if (requestDTO.destacado() == null) producto.setDestacado(destacadoPrevio);
        resolverReferencias(producto, requestDTO);
        normalize(producto);

        procesarFracciones(producto, requestDTO, precioAnterior);

        Producto productoActualizado = productoRepository.save(producto);
        descartarImagenAnterior(imagenAnterior, productoActualizado.getImagen());
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
     * {@inheritDoc}
     */
    @Override
    public ProductoResponseDTO delete(UUID id) {
        Producto producto = findEntityById(id);
        if (productoRepository.tieneHistorial(id)) {
            if (Boolean.FALSE.equals(producto.getActivo())) {
                throw new ValidationException("El producto tiene historial de inventario o ventas y permanece inactivo");
            }
            producto.setActivo(false);
            return productoMapper.toResponseDto(productoRepository.save(producto));
        }
        productoRepository.delete(producto);
        return null;
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
     * Concilia las fracciones sin cambiar el id de las que ya existen.
     * Una fracción usada en ventas o compras se desactiva; no se borra.
     * Si el precio enviado sigue siendo el proporcional al precio base anterior, se recalcula.
     */
    private void procesarFracciones(Producto producto, ProductoRequestDTO requestDTO, BigDecimal precioAnterior) {
        List<CatalogoFraccion> actuales = producto.getFracciones() == null
                ? new ArrayList<>()
                : new ArrayList<>(producto.getFracciones());

        if (!Boolean.TRUE.equals(producto.getEsFraccionable())) {
            reemplazarFracciones(producto, conservarConHistorial(actuales, Set.of()));
            return;
        }

        List<CatalogoFraccionRequestDTO> pedidas = requestDTO.fracciones();
        if (pedidas == null || pedidas.isEmpty()) {
            if (actuales.isEmpty()) {
                generarFraccionesEstandar(producto);
                return;
            }
            for (CatalogoFraccion actual : actuales) {
                if (precioProporcional(actual.getPrecioVenta(), precioAnterior, actual.getFactorDescuentoKardex())) {
                    actual.setPrecioVenta(calcularPrecioFraccion(producto.getPrecioVentaBase(), actual.getFactorDescuentoKardex()));
                }
            }
            return;
        }

        validarFactores(pedidas);
        Set<CatalogoFraccion> usadas = new HashSet<>();
        List<CatalogoFraccion> resultado = new ArrayList<>();
        for (CatalogoFraccionRequestDTO dto : pedidas) {
            CatalogoFraccion existente = buscarPorFactor(actuales, dto.factorDescuentoKardex(), usadas);
            if (existente == null) {
                CatalogoFraccion nueva = CatalogoFraccion.builder()
                        .nombreFraccion(dto.nombreFraccion() == null ? null : dto.nombreFraccion().trim())
                        .factorDescuentoKardex(dto.factorDescuentoKardex())
                        .precioVenta(dto.precioVenta() != null
                                ? dto.precioVenta()
                                : calcularPrecioFraccion(producto.getPrecioVentaBase(), dto.factorDescuentoKardex()))
                        .activo(dto.activo() != null ? dto.activo() : true)
                        .build();
                resultado.add(nueva);
            } else {
                usadas.add(existente);
                existente.setNombreFraccion(dto.nombreFraccion().trim());
                existente.setFactorDescuentoKardex(dto.factorDescuentoKardex());
                existente.setPrecioVenta(resolverPrecio(producto, dto, precioAnterior));
                existente.setActivo(dto.activo() != null ? dto.activo() : true);
                resultado.add(existente);
            }
        }
        resultado.addAll(conservarConHistorial(actuales, usadas));
        reemplazarFracciones(producto, resultado);
    }

    private void validarFactores(List<CatalogoFraccionRequestDTO> pedidas) {
        Set<String> vistos = new HashSet<>();
        for (CatalogoFraccionRequestDTO dto : pedidas) {
            if (dto.factorDescuentoKardex() == null) {
                throw new ValidationException("El factor de la fracción es requerido");
            }
            if (dto.nombreFraccion() == null || dto.nombreFraccion().isBlank()) {
                throw new ValidationException("El nombre de la fracción es requerido");
            }
            String clave = dto.factorDescuentoKardex().stripTrailingZeros().toPlainString();
            if (!vistos.add(clave)) {
                throw new ValidationException("No se puede repetir el factor de una fracción en el mismo producto");
            }
        }
    }

    private CatalogoFraccion buscarPorFactor(List<CatalogoFraccion> actuales, BigDecimal factor, Set<CatalogoFraccion> usadas) {
        if (factor == null) {
            return null;
        }
        for (CatalogoFraccion actual : actuales) {
            if (usadas.contains(actual) || actual.getFactorDescuentoKardex() == null) {
                continue;
            }
            if (actual.getFactorDescuentoKardex().compareTo(factor) == 0) {
                return actual;
            }
        }
        return null;
    }

    private BigDecimal resolverPrecio(Producto producto,
                                      CatalogoFraccionRequestDTO dto,
                                      BigDecimal precioAnterior) {
        if (dto.precioVenta() == null) {
            return calcularPrecioFraccion(producto.getPrecioVentaBase(), dto.factorDescuentoKardex());
        }
        if (precioAnterior != null
                && producto.getPrecioVentaBase() != null
                && precioAnterior.compareTo(producto.getPrecioVentaBase()) != 0
                && precioProporcional(dto.precioVenta(), precioAnterior, dto.factorDescuentoKardex())) {
            return calcularPrecioFraccion(producto.getPrecioVentaBase(), dto.factorDescuentoKardex());
        }
        return dto.precioVenta();
    }

    private boolean precioProporcional(BigDecimal precio, BigDecimal base, BigDecimal factor) {
        if (precio == null || base == null || factor == null) {
            return false;
        }
        return precio.compareTo(calcularPrecioFraccion(base, factor)) == 0;
    }

    private List<CatalogoFraccion> conservarConHistorial(List<CatalogoFraccion> actuales, Set<CatalogoFraccion> usadas) {
        List<CatalogoFraccion> historicas = new ArrayList<>();
        for (CatalogoFraccion actual : actuales) {
            if (usadas.contains(actual) || actual.getId() == null) {
                continue;
            }
            if (catalogoFraccionRepository.tieneHistorial(actual.getId())) {
                actual.setActivo(false);
                historicas.add(actual);
            }
        }
        return historicas;
    }

    private void reemplazarFracciones(Producto producto, List<CatalogoFraccion> destino) {
        if (producto.getFracciones() == null) {
            producto.setFracciones(new ArrayList<>());
        }
        producto.getFracciones().clear();
        for (CatalogoFraccion fraccion : destino) {
            producto.addFraccion(fraccion);
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
    private void resolverReferencias(Producto producto, ProductoRequestDTO request) {
        if (request.categoriaId() == null) {
            producto.setCategoria(null);
        } else {
            producto.setCategoria(categoriaRepository.findById(request.categoriaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada")));
        }
        if (request.proveedorId() == null) {
            producto.setProveedor(null);
        } else {
            producto.setProveedor(proveedorRepository.findById(request.proveedorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado")));
        }
    }

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
        producto.setImagen(normalizarImagen(producto.getImagen()));
    }

    private String normalizarImagen(String imagen) {
        if (imagen == null || imagen.isBlank()) {
            return null;
        }
        String valor = imagen.trim();
        if (valor.length() > 500) {
            throw new ValidationException("La imagen no puede exceder 500 caracteres");
        }
        String lower = valor.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return valor;
        }
        if (lower.startsWith("data:") || lower.contains("://") || valor.startsWith("/") || valor.contains("..") || !valor.startsWith("productos/")) {
            throw new ValidationException("La imagen debe ser una URL http(s) o una ruta relativa del directorio de productos");
        }
        if (!fileStorageService.existe(valor)) {
            throw new ValidationException("No se encontró el archivo de imagen indicado");
        }
        return valor;
    }

    /** Una URL externa se conserva. Si la foto local cambió, se borra el archivo anterior. */
    private void descartarImagenAnterior(String anterior, String nueva) {
        if (anterior == null || anterior.isBlank() || anterior.equals(nueva)) return;
        if (anterior.contains("..") || !anterior.startsWith("productos/")) return;
        fileStorageService.eliminar(anterior);
    }

    /**
     * Record interno para estructurar los cortes estándar durante la generación algorítmica.
     */
    private record FraccionEstandar(String nombre, BigDecimal factor) {}
}