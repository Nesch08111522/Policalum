package com.policalum.erp.infrastructure.loader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.policalum.erp.application.dto.request.KardexMovimientoRequestDTO;
import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.dto.request.ServicioColaboradorRequestDTO;
import com.policalum.erp.application.dto.request.ServicioRequestDTO;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.ProductoService;
import com.policalum.erp.application.service.ServicioService;
import com.policalum.erp.domain.enums.TipoMovimiento;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.enums.TipoReferencia;
import com.policalum.erp.domain.model.Almacen;
import com.policalum.erp.domain.model.Categoria;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.AlmacenRepository;
import com.policalum.erp.infrastructure.repository.CategoriaRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.ServicioRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Si una tabla ya tiene filas, no la toca. No duplica al admin ni a Sede Principal
 * y no crea ROLE_CLIENTE.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class CatalogoSeeder implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final ServicioRepository servicioRepository;
    private final AlmacenRepository almacenRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final ProductoService productoService;
    private final ServicioService servicioService;
    private final KardexService kardexService;

    @Override
    @Transactional
    public void run(String... args) {
        sembrarCategorias();
        sembrarAlmacen();
        quitarImagenesDeRelleno();
        sembrarProductos();
        sembrarServicio();
    }

    private void sembrarCategorias() {
        if (categoriaRepository.count() > 0) {
            log.info("Categorías ya tienen filas: no se insertan ni se modifican.");
            return;
        }
        guardarCategoria("Policarbonatos", "policarbonatos");
        guardarCategoria("Perfiles", "perfiles");
        guardarCategoria("Accesorios", "accesorios");
        log.info("Categorías iniciales insertadas: Policarbonatos, Perfiles y Accesorios.");
    }

    private void sembrarProductos() {
        if (productoRepository.count() > 0) {
            log.info("Productos ya tienen filas: no se insertan ni se modifican.");
            return;
        }
        if (categoriaRepository.count() == 0) {
            log.warn("No hay categorías: no se insertan productos.");
            return;
        }
        Almacen almacen = almacenRepository.findAll().stream()
                .filter(item -> !Boolean.FALSE.equals(item.getActivo()))
                .findFirst()
                .orElse(null);
        User operador = userRepository.findByEmail("admin@policalum.com").orElse(null);
        if (almacen == null) {
            log.info("No hay almacén activo: los productos se crean sin stock inicial.");
        } else if (operador == null) {
            log.info("No está el usuario admin: los productos se crean sin movimiento de kardex.");
        }

        for (Semilla semilla : semillas()) {
            Categoria categoria = categoriaRepository.findBySlugIgnoreCase(semilla.slug())
                    .or(() -> categoriaRepository.findAll().stream().findFirst())
                    .orElseThrow();
            var creado = productoService.create(new ProductoRequestDTO(
                    categoria.getId(),
                    null,
                    semilla.sku(),
                    semilla.nombre(),
                    semilla.descripcion(),
                    TipoProducto.FISICO,
                    semilla.compra(),
                    semilla.venta(),
                    semilla.unidad(),
                    semilla.fraccionable(),
                    true,
                    true,
                    "10",
                    null,
                    semilla.imagen()));
            if (almacen != null && operador != null && semilla.stock().signum() > 0) {
                Producto producto = productoRepository.findById(creado.id()).orElseThrow();
                kardexService.registrarMovimiento(new KardexMovimientoRequestDTO(
                        producto,
                        almacen,
                        null,
                        TipoReferencia.AJUSTE,
                        TipoMovimiento.ENTRADA,
                        semilla.stock(),
                        BigDecimal.ZERO,
                        operador,
                        "Stock inicial del catálogo. SKU " + semilla.sku()));
            }
        }
        log.info("Catálogo inicial insertado: {} productos.", semillas().size());
    }

    private void sembrarServicio() {
        if (servicioRepository.count() > 0) {
            log.info("Servicios ya tienen filas: no se insertan ni se modifican.");
            return;
        }
        List<ServicioColaboradorRequestDTO> equipo = new ArrayList<>();
        userRepository.findByEmail("empleado@policalum.com").ifPresent(usuario ->
                equipo.add(new ServicioColaboradorRequestDTO(usuario.getId(), "INSTALO")));
        userRepository.findByEmail("admin@policalum.com").ifPresent(usuario ->
                equipo.add(new ServicioColaboradorRequestDTO(usuario.getId(), "AYUDO")));
        servicioService.create(new ServicioRequestDTO(
                "Servicio de corte",
                "Corte de planchas de policarbonato y perfiles a medida.",
                new BigDecimal("25.00"),
                true,
                equipo));
        log.info("Servicio inicial insertado: Servicio de corte. Colaboradores reales: {}.", equipo.size());
    }

    private void sembrarAlmacen() {
        if (almacenRepository.count() > 0) {
            log.info("Almacenes ya tienen filas: no se insertan ni se modifican.");
            return;
        }
        Sede sede = sedeRepository.findAll().stream()
                .filter(item -> !Boolean.FALSE.equals(item.getActiva()))
                .findFirst()
                .orElse(null);
        if (sede == null) {
            log.info("No hay sede activa: no se crea el almacén principal.");
            return;
        }
        almacenRepository.save(Almacen.builder()
                .sede(sede)
                .nombre("Almacén principal")
                .descripcion("Almacén de " + sede.getNombre() + ". Sin stock hasta el primer ingreso.")
                .activo(true)
                .build());
        log.info("Almacén principal creado para {}.", sede.getNombre());
    }

    /** Las fotos de placehold.co no son del catálogo. Se dejan en blanco para mostrar la inicial del nombre. */
    private void quitarImagenesDeRelleno() {
        List<Producto> tocados = new ArrayList<>();
        for (Producto producto : productoRepository.findAll()) {
            String imagen = producto.getImagen();
            if (imagen != null && imagen.startsWith("https://placehold.co")) {
                producto.setImagen(null);
                tocados.add(producto);
            }
        }
        if (tocados.isEmpty()) return;
        productoRepository.saveAll(tocados);
        log.info("Se quitaron {} imágenes de relleno del catálogo.", tocados.size());
    }

    private void guardarCategoria(String nombre, String slug) {
        Categoria categoria = Categoria.builder().nombre(nombre).slug(slug).activa(true).build();
        categoriaRepository.save(categoria);
    }

    private static List<Semilla> semillas() {
        return List.of(
                new Semilla("PC-4", "Policarbonato alveolar 4 mm", "Plancha alveolar de 4 mm para techos ligeros.", "policarbonatos", "45.00", "89.00", "PLANCHA", true, "24"),
                new Semilla("PC-6", "Policarbonato alveolar 6 mm", "Plancha alveolar de 6 mm, el corte habitual de obra.", "policarbonatos", "62.00", "120.00", "PLANCHA", true, "18"),
                new Semilla("PC-8", "Policarbonato alveolar 8 mm", "Plancha alveolar de 8 mm para cubiertas.", "policarbonatos", "78.00", "149.00", "PLANCHA", true, "12"),
                new Semilla("PC-10", "Policarbonato alveolar 10 mm", "Plancha alveolar de 10 mm.", "policarbonatos", "95.00", "179.00", "PLANCHA", true, "10"),
                new Semilla("PCS-2", "Policarbonato sólido 2 mm", "Lámina sólida de 2 mm.", "policarbonatos", "55.00", "110.00", "PLANCHA", true, "8"),
                new Semilla("PCS-3", "Policarbonato sólido 3 mm", "Lámina sólida de 3 mm.", "policarbonatos", "70.00", "135.00", "PLANCHA", true, "8"),
                new Semilla("PCS-4", "Policarbonato sólido 4 mm", "Lámina sólida de 4 mm.", "policarbonatos", "88.00", "168.00", "PLANCHA", true, "6"),
                new Semilla("PF-H", "Perfil H de unión", "Perfil H para unir planchas.", "perfiles", "8.00", "16.00", "BARRA", false, "40"),
                new Semilla("PF-U", "Perfil U de cierre", "Perfil U para el canto de la plancha.", "perfiles", "7.00", "14.00", "BARRA", false, "40"),
                new Semilla("PF-R", "Perfil de remate", "Remate de aluminio para el borde.", "perfiles", "6.50", "13.00", "BARRA", false, "30"),
                new Semilla("PF-ANG", "Perfil angular", "Ángulo de aluminio para esquinas.", "perfiles", "9.00", "18.00", "BARRA", false, "20"),
                new Semilla("PF-CLIP", "Clip de fijación", "Clip para sujetar la plancha al perfil.", "perfiles", "1.50", "3.50", "UNID", false, "100"),
                new Semilla("AC-TOR", "Tornillo autoperforante", "Tornillo con punta para estructura metálica.", "accesorios", "0.15", "0.40", "UNID", false, "500"),
                new Semilla("AC-ARAN", "Arandela de neopreno", "Arandela selladora para fijaciones.", "accesorios", "0.08", "0.25", "UNID", false, "500"),
                new Semilla("AC-SIL", "Silicona neutra", "Cartucho de silicona neutra para policarbonato.", "accesorios", "12.00", "24.00", "UNID", false, "36"),
                new Semilla("AC-CINT", "Cinta de aluminio", "Cinta para sellar los alvéolos.", "accesorios", "18.00", "36.00", "UNID", false, "20"),
                new Semilla("AC-TAPA", "Tapa de policarbonato", "Tapa de cierre para perfil.", "accesorios", "4.00", "9.00", "UNID", false, "50"),
                new Semilla("AC-ESC", "Escuadra de aluminio", "Escuadra para armar marcos.", "accesorios", "3.50", "8.00", "UNID", false, "40")
        );
    }

    private record Semilla(String sku, String nombre, String descripcion, String slug, BigDecimal compra,
                           BigDecimal venta, String unidad, boolean fraccionable, BigDecimal stock, String imagen) {
        Semilla(String sku, String nombre, String descripcion, String slug, String compra, String venta,
                String unidad, boolean fraccionable, String stock) {
            this(sku, nombre, descripcion, slug, new BigDecimal(compra), new BigDecimal(venta), unidad, fraccionable,
                    new BigDecimal(stock), null);
        }
    }
}
