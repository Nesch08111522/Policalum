package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.response.CategoriaPublicaDTO;
import com.policalum.erp.application.dto.response.EmpresaPublicaDTO;
import com.policalum.erp.application.dto.response.FraccionPublicaDTO;
import com.policalum.erp.application.dto.response.PaginaPublicaDTO;
import com.policalum.erp.application.dto.response.ProductoPublicoDTO;
import com.policalum.erp.application.dto.response.ProyectoPortafolioResponseDTO;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Categoria;
import com.policalum.erp.domain.model.ConfiguracionEmpresa;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.ProyectoPortafolio;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;
import com.policalum.erp.infrastructure.repository.ConfiguracionEmpresaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.ProyectoPortafolioRepository;
import com.policalum.erp.infrastructure.repository.StockRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortalPublicoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final StockRepository stockRepository;
    private final ConfiguracionEmpresaRepository empresaRepository;
    private final ProyectoPortafolioRepository portafolioRepository;

    public PaginaPublicaDTO<ProductoPublicoDTO> catalogo(UUID categoriaId, String busqueda, boolean soloDestacados, Pageable pageable) {
        int tamano = Math.min(Math.max(pageable.getPageSize(), 1), 48);
        Pageable pagina = PageRequest.of(Math.max(pageable.getPageNumber(), 0), tamano, pageable.getSort());
        String q = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        Page<Producto> resultado = productoRepository.catalogoPublico(categoriaId, soloDestacados, q, pagina);
        Map<UUID, BigDecimal> saldos = saldos(resultado.getContent());
        List<ProductoPublicoDTO> contenido = resultado.getContent().stream().map(producto -> ficha(producto, saldos)).toList();
        return new PaginaPublicaDTO<>(contenido, resultado.getNumber(), resultado.getSize(), resultado.getTotalElements(), resultado.getTotalPages());
    }

    public ProductoPublicoDTO producto(UUID id) {
        Producto producto = productoRepository.findById(id)
                .filter(item -> Boolean.TRUE.equals(item.getActivo()))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return ficha(producto, saldos(List.of(producto)));
    }

    public List<CategoriaPublicaDTO> categorias() {
        return categoriaRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(categoria -> new CategoriaPublicaDTO(categoria.getId(), categoria.getNombre(), categoria.getSlug()))
                .toList();
    }

    public List<ProductoPublicoDTO> destacados() {
        PaginaPublicaDTO<ProductoPublicoDTO> pagina = catalogo(null, null, true, PageRequest.of(0, 8));
        return pagina.content();
    }

    public EmpresaPublicaDTO empresa() {
        ConfiguracionEmpresa empresa = empresaRepository.findById(ConfiguracionEmpresa.ID_UNICO).orElse(null);
        String nombre = empresa == null || empresa.getNombreComercial() == null || empresa.getNombreComercial().isBlank()
                ? "POLICALUM" : empresa.getNombreComercial();
        String telefono = empresa == null ? null : empresa.getTelefono();
        String logo = empresa == null ? null : empresa.getLogoUrl();
        if (logo != null && !logo.startsWith("http://") && !logo.startsWith("https://")) {
            logo = "/api/v1/configuracion/logo";
        }
        return new EmpresaPublicaDTO(
                nombre,
                empresa == null ? null : empresa.getDireccionFiscal(),
                telefono,
                empresa == null ? null : empresa.getEmail(),
                whatsapp(telefono),
                logo);
    }

    public List<ProyectoPortafolioResponseDTO> portafolio() {
        return portafolioRepository.findByPublicadoTrueOrderByOrdenAscTituloAsc().stream().map(this::proyecto).toList();
    }

    private ProyectoPortafolioResponseDTO proyecto(ProyectoPortafolio proyecto) {
        return new ProyectoPortafolioResponseDTO(proyecto.getId(), proyecto.getTitulo(), proyecto.getDescripcion(),
                proyecto.getImagenUrl(), proyecto.getOrden(), proyecto.getPublicado(), proyecto.getCreatedAt());
    }

    private Map<UUID, BigDecimal> saldos(List<Producto> productos) {
        if (productos.isEmpty()) return Map.of();
        List<UUID> ids = productos.stream().map(Producto::getId).toList();
        Map<UUID, BigDecimal> saldos = new HashMap<>();
        for (Object[] fila : stockRepository.sumarCantidadPorProductos(ids)) {
            if (fila[0] == null) continue;
            BigDecimal cantidad = fila[1] == null ? BigDecimal.ZERO : new BigDecimal(fila[1].toString());
            saldos.put((UUID) fila[0], cantidad);
        }
        return saldos;
    }

    private ProductoPublicoDTO ficha(Producto producto, Map<UUID, BigDecimal> saldos) {
        Categoria categoria = producto.getCategoria();
        BigDecimal saldo = saldos.getOrDefault(producto.getId(), BigDecimal.ZERO);
        List<FraccionPublicaDTO> fracciones = producto.getFracciones() == null ? List.of() : producto.getFracciones().stream()
                .filter(fraccion -> Boolean.TRUE.equals(fraccion.getActivo()))
                .sorted(Comparator.comparing(CatalogoFraccion::getFactorDescuentoKardex).reversed())
                .map(fraccion -> new FraccionPublicaDTO(fraccion.getId(), fraccion.getNombreFraccion(), fraccion.getFactorDescuentoKardex(), fraccion.getPrecioVenta()))
                .toList();
        return new ProductoPublicoDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getCodigoSku(),
                producto.getUnidad(),
                producto.getPrecioVentaBase(),
                saldo.compareTo(BigDecimal.ZERO) > 0,
                Boolean.TRUE.equals(producto.getDestacado()),
                Boolean.TRUE.equals(producto.getEsFraccionable()),
                categoria == null ? null : categoria.getId(),
                categoria == null ? null : categoria.getNombre(),
                imagen(producto.getImagen()),
                fracciones);
    }

    private static String imagen(String ruta) {
        if (ruta == null || ruta.isBlank()) return null;
        if (ruta.startsWith("http://") || ruta.startsWith("https://")) return ruta;
        return "/api/v1/public/catalogo/imagenes?ruta=" + URLEncoder.encode(ruta, StandardCharsets.UTF_8);
    }

    static String whatsapp(String telefono) {
        if (telefono == null) return null;
        String digitos = telefono.replaceAll("\\D", "");
        if (digitos.length() == 9) return "51" + digitos;
        if (digitos.length() < 8) return null;
        return digitos;
    }
}
