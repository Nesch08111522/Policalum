package com.policalum.erp.application.service.serviceImpl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.ProductoImportacionFilaDTO;
import com.policalum.erp.application.dto.request.ProductoImportacionRequestDTO;
import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.response.ProductoImportacionResultadoDTO;
import com.policalum.erp.application.dto.response.ProductoResponseDTO;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.ProductoImportacionService;
import com.policalum.erp.application.service.ProductoService;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Categoria;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ImportacionRechazadaException;
import com.policalum.erp.exception.ImportacionRechazadaException.ErrorFila;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductoImportacionServiceImpl implements ProductoImportacionService {

    private final ProductoService productoService;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final AlmacenRepository almacenRepository;
    private final KardexService kardexService;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductoImportacionResultadoDTO importar(ProductoImportacionRequestDTO request) {
        List<ProductoImportacionFilaDTO> filas = request.filas();
        if (filas == null || filas.isEmpty()) {
            throw new ImportacionRechazadaException(List.of(new ErrorFila(0, "No hay filas para importar")));
        }

        List<ErrorFila> errores = new ArrayList<>();
        List<FilaLista> listas = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        int numero = 0;
        for (ProductoImportacionFilaDTO fila : filas) {
            numero++;
            try {
                listas.add(preparar(numero, fila, skus));
            } catch (RuntimeException ex) {
                errores.add(new ErrorFila(numero, mensaje(ex)));
            }
        }
        if (!errores.isEmpty()) {
            throw new ImportacionRechazadaException(errores);
        }

        int creados = 0;
        int actualizados = 0;
        for (FilaLista lista : listas) {
            try {
                if (lista.existente() == null) {
                    ProductoResponseDTO creado = productoService.create(lista.request());
                    creados++;
                    if (lista.stock() != null) {
                        registrarEntrada(creado.id(), lista.almacen(), lista.stock(), lista.sku());
                    }
                } else {
                    productoService.update(lista.existente().getId(), lista.request());
                    actualizados++;
                }
            } catch (RuntimeException ex) {
                throw new ImportacionRechazadaException(List.of(new ErrorFila(lista.numero(), mensaje(ex))));
            }
        }
        return new ProductoImportacionResultadoDTO(creados, actualizados, filas.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoImportacionFilaDTO> exportar(String searchTerm, UUID categoriaId, TipoProducto tipo,
                                                     Boolean activo, Boolean esFraccionable) {
        String termino = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();
        Pageable pageable = PageRequest.of(0, 5000, Sort.by("nombre"));
        Page<Producto> page = productoRepository.search(termino, categoriaId, tipo, activo, esFraccionable, pageable);
        if (page.getTotalElements() > 5000) {
            throw new ValidationException("Hay más de 5000 productos para exportar. Acota la búsqueda.");
        }
        return page.getContent().stream().map(this::filaDe).toList();
    }

    private FilaLista preparar(int numero, ProductoImportacionFilaDTO fila, Set<String> skus) {
        if (fila == null || fila.codigoSku() == null || fila.codigoSku().isBlank()) {
            throw new ValidationException("El SKU es obligatorio");
        }
        String sku = fila.codigoSku().trim();
        if (sku.length() > 50) {
            throw new ValidationException("El SKU no puede exceder 50 caracteres");
        }
        if (!skus.add(sku.toLowerCase(Locale.ROOT))) {
            throw new ValidationException("El SKU está repetido en el archivo");
        }

        Producto existente = productoRepository.findByCodigoSkuIgnoreCase(sku).orElse(null);
        Categoria categoria = resolverCategoria(fila.categoria());
        validarImagenDeclarada(fila.imagen());
        BigDecimal stock = stockDe(fila.stockInicial(), existente != null);
        Almacen almacen = null;
        if (stock != null) {
            almacen = resolverAlmacen(fila.almacen());
        }
        ProductoRequestDTO cuerpo = requestDe(existente, fila, categoria, sku);
        return new FilaLista(numero, existente, cuerpo, almacen, stock, sku);
    }

    private ProductoRequestDTO requestDe(Producto existente, ProductoImportacionFilaDTO fila, Categoria categoria, String sku) {
        boolean nuevo = existente == null;
        String nombre = texto(fila.nombre());
        if (nombre == null) {
            if (nuevo) {
                throw new ValidationException("El nombre es obligatorio para un producto nuevo");
            }
            nombre = existente.getNombre();
        }
        if (nombre.length() > 255) {
            throw new ValidationException("El nombre no puede exceder 255 caracteres");
        }

        BigDecimal compra = fila.precioCompra() != null ? fila.precioCompra() : (existente != null ? existente.getPrecioCompra() : null);
        BigDecimal venta = fila.precioVentaBase() != null ? fila.precioVentaBase() : (existente != null ? existente.getPrecioVentaBase() : null);
        if (compra == null) {
            throw new ValidationException("El precio de compra es obligatorio");
        }
        if (venta == null) {
            throw new ValidationException("El precio de venta es obligatorio");
        }
        if (compra.signum() < 0 || venta.signum() < 0) {
            throw new ValidationException("Los precios no pueden ser negativos");
        }

        Boolean fraccionable = fila.esFraccionable() != null
                ? fila.esFraccionable()
                : existente != null && Boolean.TRUE.equals(existente.getEsFraccionable());
        Boolean activo = fila.activo() != null
                ? fila.activo()
                : existente == null || !Boolean.FALSE.equals(existente.getActivo());
        Boolean igv = fila.incluyeIgv() != null
                ? fila.incluyeIgv()
                : existente == null || !Boolean.FALSE.equals(existente.getIncluyeIgv());
        String unidad = texto(fila.unidad());
        if (unidad == null) {
            unidad = existente != null && existente.getUnidad() != null ? existente.getUnidad() : "UNID";
        }
        String afectacion = texto(fila.tipoAfectacion());
        if (afectacion == null) {
            afectacion = existente != null && existente.getTipoAfectacion() != null ? existente.getTipoAfectacion() : "10";
        }
        String descripcion = fila.descripcion();
        if (descripcion == null && existente != null) {
            descripcion = existente.getDescripcion();
        }
        String imagen = texto(fila.imagen());
        if (imagen == null && existente != null) {
            imagen = existente.getImagen();
        }
        UUID categoriaId = categoria != null
                ? categoria.getId()
                : (existente != null && existente.getCategoria() != null ? existente.getCategoria().getId() : null);
        UUID proveedorId = existente != null && existente.getProveedor() != null ? existente.getProveedor().getId() : null;

        return new ProductoRequestDTO(
                categoriaId,
                proveedorId,
                sku,
                nombre,
                descripcion,
                resolverTipo(fila.tipo(), existente),
                compra,
                venta,
                unidad,
                fraccionable,
                activo,
                igv,
                afectacion,
                fraccionesDe(existente, fraccionable),
                imagen);
    }

    private List<CatalogoFraccionRequestDTO> fraccionesDe(Producto existente, Boolean fraccionable) {
        if (!Boolean.TRUE.equals(fraccionable) || existente == null || !Boolean.TRUE.equals(existente.getEsFraccionable())
                || existente.getFracciones() == null) {
            return null;
        }
        List<CatalogoFraccionRequestDTO> fracciones = existente.getFracciones().stream()
                .filter(fraccion -> fraccion.getNombreFraccion() != null && fraccion.getFactorDescuentoKardex() != null)
                .map(fraccion -> new CatalogoFraccionRequestDTO(
                        existente.getId(),
                        fraccion.getNombreFraccion(),
                        fraccion.getFactorDescuentoKardex(),
                        fraccion.getPrecioVenta(),
                        fraccion.getActivo()))
                .toList();
        return fracciones.isEmpty() ? null : fracciones;
    }

    private Categoria resolverCategoria(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        List<Categoria> lista = categoriaRepository.buscarPorNombreOSlug(texto.trim());
        if (lista.isEmpty()) {
            throw new ValidationException("No existe la categoría \"" + texto.trim() + "\"");
        }
        if (lista.size() > 1) {
            throw new ValidationException("Hay más de una categoría con el nombre o slug \"" + texto.trim() + "\"");
        }
        if (Boolean.FALSE.equals(lista.get(0).getActiva())) {
            throw new ValidationException("La categoría está inactiva");
        }
        return lista.get(0);
    }

    private Almacen resolverAlmacen(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidationException("Indica un almacén existente para el stock inicial. No se crea un almacén nuevo.");
        }
        String valor = texto.trim();
        try {
            UUID id = UUID.fromString(valor);
            Almacen almacen = almacenRepository.findById(id)
                    .orElseThrow(() -> new ValidationException("No existe el almacén indicado"));
            if (Boolean.FALSE.equals(almacen.getActivo())) {
                throw new ValidationException("El almacén está inactivo");
            }
            return almacen;
        } catch (IllegalArgumentException ex) {
            List<Almacen> lista = almacenRepository.buscarPorNombre(valor);
            if (lista.isEmpty()) {
                throw new ValidationException("No existe un almacén con el nombre indicado. No se crea un almacén nuevo.");
            }
            if (lista.size() > 1) {
                throw new ValidationException("Hay más de un almacén con ese nombre. Indica el identificador del almacén.");
            }
            if (Boolean.FALSE.equals(lista.get(0).getActivo())) {
                throw new ValidationException("El almacén está inactivo");
            }
            return lista.get(0);
        }
    }

    private BigDecimal stockDe(BigDecimal stock, boolean existente) {
        if (stock == null || stock.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (stock.signum() < 0) {
            throw new ValidationException("El stock inicial no puede ser negativo");
        }
        if (existente) {
            throw new ValidationException("El stock inicial solo se registra al crear un SKU nuevo. Para sumar existencias usa un ajuste de kardex.");
        }
        return stock;
    }

    private TipoProducto resolverTipo(String tipo, Producto existente) {
        if (tipo == null || tipo.isBlank()) {
            return existente != null && existente.getTipo() != null ? existente.getTipo() : TipoProducto.FISICO;
        }
        try {
            return TipoProducto.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("El tipo debe ser FISICO o SERVICIO");
        }
    }

    private void validarImagenDeclarada(String imagen) {
        if (imagen == null || imagen.isBlank()) {
            return;
        }
        String valor = imagen.trim();
        if (valor.length() > 500) {
            throw new ValidationException("La imagen no puede exceder 500 caracteres");
        }
        String lower = valor.toLowerCase(Locale.ROOT);
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return;
        }
        if (valor.contains("..") || !valor.startsWith("productos/") || !fileStorageService.existe(valor)) {
            throw new ValidationException("La imagen debe ser una URL http(s) o un archivo ya guardado en productos/");
        }
    }

    private void registrarEntrada(UUID productoId, Almacen almacen, BigDecimal cantidad, String sku) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalStateException("El producto recién creado no está disponible para el kardex"));
        kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(
                producto,
                almacen,
                null,
                TipoReferencia.AJUSTE,
                TipoMovimiento.ENTRADA,
                cantidad,
                BigDecimal.ZERO,
                null,
                "Stock inicial por importación masiva. SKU " + sku));
    }

    private ProductoImportacionFilaDTO filaDe(Producto producto) {
        return new ProductoImportacionFilaDTO(
                producto.getCodigoSku(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCategoria() != null ? producto.getCategoria().getNombre() : null,
                producto.getTipo() != null ? producto.getTipo().name() : null,
                producto.getPrecioCompra(),
                producto.getPrecioVentaBase(),
                producto.getUnidad(),
                producto.getEsFraccionable(),
                producto.getActivo(),
                producto.getIncluyeIgv(),
                producto.getTipoAfectacion(),
                producto.getImagen(),
                null,
                null);
    }

    private static String texto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private static String mensaje(RuntimeException ex) {
        String texto = ex.getMessage();
        return texto == null || texto.isBlank() ? "No se pudo procesar la fila" : texto;
    }

    private record FilaLista(int numero, Producto existente, ProductoRequestDTO request, Almacen almacen, BigDecimal stock, String sku) {
    }
}
