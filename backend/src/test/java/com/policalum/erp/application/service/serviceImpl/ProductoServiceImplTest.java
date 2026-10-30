package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.CatalogoFraccionRequestDTO;
import com.policalum.erp.application.dto.request.ProductoRequestDTO;
import com.policalum.erp.application.mapper.ProductoMapper;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.TipoProducto;
import com.policalum.erp.domain.model.CatalogoFraccion;
import com.policalum.erp.domain.model.Producto;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.CatalogoFraccionRepository;
import com.policalum.erp.infrastructure.repository.ProductoRepository;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

/** Maestro de productos: SKU único, normalización y generación de fracciones (planchas). */
@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock ProductoRepository productoRepository;
    @Mock CatalogoFraccionRepository catalogoFraccionRepository;
    @Mock ProductoMapper productoMapper;
    @Mock FileStorageService fileStorageService;
    @InjectMocks ProductoServiceImpl service;

    UUID id = UUID.randomUUID();
    Producto entidad;

    @BeforeEach
    void setUp() {
        lenient().when(productoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private ProductoRequestDTO request(String sku, boolean fraccionable, List<CatalogoFraccionRequestDTO> fracciones) {
        return new ProductoRequestDTO(null, null, sku, "Policarbonato 6mm", null, TipoProducto.FISICO, new BigDecimal("80.00"),
                new BigDecimal("150.00"), "unid", fraccionable, true, true, "10", fracciones, null, null);
    }

    /** El mapper es un mock: devolvemos una entidad "como la mapearía" MapStruct, sin normalizar. */
    private Producto mapeado(String sku, boolean fraccionable, String precioBase) {
        entidad = Producto.builder().codigoSku(sku).nombre("  Policarbonato 6mm ").unidad(" unid ")
                .esFraccionable(fraccionable).precioVentaBase(precioBase == null ? null : new BigDecimal(precioBase)).build();
        lenient().when(productoMapper.toEntity(any())).thenReturn(entidad);
        return entidad;
    }

    private Producto guardado() {
        ArgumentCaptor<Producto> c = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(c.capture());
        return c.getValue();
    }

    // ---------------------------------------------------------------- create

    @Test
    void create_normalizaSkuNombreYUnidad() {
        mapeado("  PC-6 ", false, "150.00");
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.empty());

        service.create(request("  PC-6 ", false, null));

        Producto p = guardado();
        assertEquals("PC-6", p.getCodigoSku());
        assertEquals("Policarbonato 6mm", p.getNombre());
        assertEquals("UNID", p.getUnidad());
        assertTrue(p.getFracciones().isEmpty(), "no fraccionable: sin fracciones");
    }

    @Test
    void create_fraccionableSinFracciones_generaLos4CortesEstandarConPrecioProporcional() {
        mapeado("PC-6", true, "150.00");
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());

        service.create(request("PC-6", true, null));

        List<CatalogoFraccion> f = guardado().getFracciones();
        assertEquals(List.of("Plancha Completa", "3/4 Plancha", "Media Plancha (1/2)", "1/4 Plancha"),
                f.stream().map(CatalogoFraccion::getNombreFraccion).toList());
        assertEquals(List.of(new BigDecimal("150.00"), new BigDecimal("112.50"), new BigDecimal("75.00"), new BigDecimal("37.50")),
                f.stream().map(CatalogoFraccion::getPrecioVenta).toList());
        assertTrue(f.stream().allMatch(x -> x.getProducto() == entidad && x.getActivo()));
    }

    @Test
    void create_fraccionableSinPrecioBase_fraccionesEnCero() {
        mapeado("PC-6", true, null);
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());

        service.create(request("PC-6", true, List.of()));

        assertTrue(guardado().getFracciones().stream().allMatch(x -> x.getPrecioVenta().signum() == 0));
    }

    @Test
    void create_fraccionesPersonalizadas_respetaPrecioOLoCalculaPorFactor() {
        mapeado("PC-6", true, "150.00");
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());
        List<CatalogoFraccionRequestDTO> fr = List.of(
                new CatalogoFraccionRequestDTO(null, "Tira", new BigDecimal("0.1000"), new BigDecimal("20.00"), null),
                new CatalogoFraccionRequestDTO(null, "Tercio", new BigDecimal("0.3333"), null, false));

        service.create(request("PC-6", true, fr));

        List<CatalogoFraccion> f = guardado().getFracciones();
        assertEquals(2, f.size());
        assertEquals(new BigDecimal("20.00"), f.get(0).getPrecioVenta(), "precio explícito se respeta");
        assertTrue(f.get(0).getActivo(), "activo nulo → true");
        assertEquals(new BigDecimal("50.00"), f.get(1).getPrecioVenta(), "150 × 0.3333 = 49.995 → 50.00");
        assertFalse(f.get(1).getActivo());
    }

    @Test
    void create_noFraccionableConFraccionesEnElDto_lasIgnora() {
        mapeado("PC-6", false, "150.00");
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());

        service.create(request("PC-6", false, List.of(new CatalogoFraccionRequestDTO(null, "x", BigDecimal.ONE, null, true))));

        assertTrue(guardado().getFracciones().isEmpty());
    }

    @Test
    void create_skuDuplicado_lanzaValidationSinGuardar() {
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.of(Producto.builder().id(UUID.randomUUID()).build()));

        assertThrows(ValidationException.class, () -> service.create(request(" PC-6 ", false, null)));
        verify(productoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_sinListaDeFracciones_conservaLasExistentes() {
        Producto p = Producto.builder().id(id).codigoSku("PC-6").nombre("x").esFraccionable(true)
                .precioVentaBase(new BigDecimal("100.00")).build();
        p.addFraccion(CatalogoFraccion.builder().nombreFraccion("vieja").factorDescuentoKardex(new BigDecimal("0.5000"))
                .precioVenta(new BigDecimal("50.00")).build());
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.of(p));

        service.update(id, request("PC-6", true, null));

        verify(productoMapper).updateEntityFromDto(any(), eq(p));
        assertEquals(1, p.getFracciones().size());
        assertEquals("vieja", p.getFracciones().get(0).getNombreFraccion());
    }

    @Test
    void update_conservaElIdYRecalculaElPrecioProporcional() {
        UUID fraccionId = UUID.randomUUID();
        Producto p = Producto.builder().id(id).codigoSku("PC-6").nombre("x").esFraccionable(true)
                .precioVentaBase(new BigDecimal("100.00")).build();
        p.addFraccion(CatalogoFraccion.builder().id(fraccionId).nombreFraccion("Media").factorDescuentoKardex(new BigDecimal("0.5000"))
                .precioVenta(new BigDecimal("50.00")).activo(true).build());
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.of(p));
        doAnswer(invocacion -> {
            Producto destino = invocacion.getArgument(1);
            destino.setPrecioVentaBase(new BigDecimal("200.00"));
            destino.setEsFraccionable(true);
            return null;
        }).when(productoMapper).updateEntityFromDto(any(), any());

        service.update(id, request("PC-6", true, List.of(
                new CatalogoFraccionRequestDTO(id, "Media plancha", new BigDecimal("0.5"), new BigDecimal("50.00"), true))));

        CatalogoFraccion guardada = p.getFracciones().get(0);
        assertEquals(1, p.getFracciones().size());
        assertEquals(fraccionId, guardada.getId());
        assertEquals("Media plancha", guardada.getNombreFraccion());
        assertEquals(new BigDecimal("100.00"), guardada.getPrecioVenta());
    }

    @Test
    void update_precioPersonalizado_noSePisaAlCambiarLaBase() {
        Producto p = Producto.builder().id(id).codigoSku("PC-6").nombre("x").esFraccionable(true)
                .precioVentaBase(new BigDecimal("100.00")).build();
        p.addFraccion(CatalogoFraccion.builder().id(UUID.randomUUID()).nombreFraccion("Media").factorDescuentoKardex(new BigDecimal("0.5000"))
                .precioVenta(new BigDecimal("80.00")).activo(true).build());
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.of(p));
        doAnswer(invocacion -> {
            Producto destino = invocacion.getArgument(1);
            destino.setPrecioVentaBase(new BigDecimal("200.00"));
            destino.setEsFraccionable(true);
            return null;
        }).when(productoMapper).updateEntityFromDto(any(), any());

        service.update(id, request("PC-6", true, List.of(
                new CatalogoFraccionRequestDTO(id, "Media", new BigDecimal("0.5"), new BigDecimal("80.00"), true))));

        assertEquals(new BigDecimal("80.00"), p.getFracciones().get(0).getPrecioVenta());
    }

    @Test
    void update_fraccionConHistorialQueSaleDeLaLista_quedaInactiva() {
        UUID usada = UUID.randomUUID();
        Producto p = Producto.builder().id(id).codigoSku("PC-6").nombre("x").esFraccionable(true)
                .precioVentaBase(new BigDecimal("100.00")).build();
        p.addFraccion(CatalogoFraccion.builder().id(usada).nombreFraccion("Cuarto").factorDescuentoKardex(new BigDecimal("0.2500"))
                .precioVenta(new BigDecimal("25.00")).activo(true).build());
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.of(p));
        when(catalogoFraccionRepository.tieneHistorial(usada)).thenReturn(true);

        service.update(id, request("PC-6", true, List.of(
                new CatalogoFraccionRequestDTO(id, "Completa", BigDecimal.ONE, new BigDecimal("100.00"), true))));

        assertEquals(2, p.getFracciones().size());
        CatalogoFraccion historica = p.getFracciones().stream().filter(f -> usada.equals(f.getId())).findFirst().orElseThrow();
        assertFalse(historica.getActivo());
        assertEquals(usada, historica.getId());
    }

    @Test
    void update_skuDeOtroProducto_lanzaValidation() {
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(Producto.builder().id(id).build()));
        when(productoRepository.findByCodigoSkuIgnoreCase("OTRO")).thenReturn(Optional.of(Producto.builder().id(UUID.randomUUID()).build()));

        assertThrows(ValidationException.class, () -> service.update(id, request("OTRO", false, null)));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void update_inexistente_lanzaNotFound() {
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(id, request("x", false, null)));
    }

    @Test
    void create_urlExterna_seGuardaSinDescargarElArchivo() {
        Producto producto = mapeado("PC-6", false, "150.00");
        producto.setImagen("https://imagenes.com/foto.jpg");
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.empty());

        service.create(request("PC-6", false, null));

        assertEquals("https://imagenes.com/foto.jpg", guardado().getImagen());
        verify(fileStorageService, never()).existe(any());
    }

    @Test
    void create_dataUrl_seRechaza() {
        Producto producto = mapeado("PC-6", false, "150.00");
        producto.setImagen("data:image/png;base64,aaaa");
        when(productoRepository.findByCodigoSkuIgnoreCase(any())).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.create(request("PC-6", false, null)));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void update_alReemplazarArchivoPorUrl_borraLaRutaAnterior() {
        Producto producto = Producto.builder().id(id).codigoSku("PC-6").nombre("x").esFraccionable(false)
                .precioVentaBase(new BigDecimal("100.00")).imagen("productos/2026/10/vieja.png").build();
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(producto));
        when(productoRepository.findByCodigoSkuIgnoreCase("PC-6")).thenReturn(Optional.of(producto));
        doAnswer(invocacion -> {
            Producto destino = invocacion.getArgument(1);
            destino.setImagen("https://imagenes.com/nueva.jpg");
            return null;
        }).when(productoMapper).updateEntityFromDto(any(), any());

        service.update(id, request("PC-6", false, null));

        verify(fileStorageService).eliminar("productos/2026/10/vieja.png");
        assertEquals("https://imagenes.com/nueva.jpg", producto.getImagen());
    }

    // ---------------------------------------------------------------- otros

    @Test
    void isSkuDisponible_nuloOVacio_siempreDisponibleSinConsultar() {
        assertTrue(service.isSkuDisponible(null, null));
        assertTrue(service.isSkuDisponible("  ", null));
        verifyNoInteractions(productoRepository);
    }

    @Test
    void search_terminoEnBlancoSeEnviaComoNulo() {
        Pageable pg = PageRequest.of(0, 10);
        when(productoRepository.search(null, null, TipoProducto.FISICO, true, null, pg)).thenReturn(Page.empty());

        service.search("  ", null, TipoProducto.FISICO, true, null, pg);

        verify(productoRepository).search(null, null, TipoProducto.FISICO, true, null, pg);
    }

    @Test
    void search_terminoSeRecorta() {
        Pageable pg = PageRequest.of(0, 10);
        when(productoRepository.search(eq("poli"), any(), any(), any(), any(), eq(pg))).thenReturn(Page.empty());

        service.search(" poli ", null, null, null, null, pg);

        verify(productoRepository).search("poli", null, null, null, null, pg);
    }

    @Test
    void delete_sinHistorial_borra() {
        Producto p = Producto.builder().id(id).activo(true).build();
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.tieneHistorial(id)).thenReturn(false);

        assertNull(service.delete(id));

        verify(productoRepository).delete(p);
    }

    @Test
    void delete_conHistorial_desactivaYNoBorra() {
        Producto p = Producto.builder().id(id).activo(true).build();
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.tieneHistorial(id)).thenReturn(true);

        service.delete(id);

        assertFalse(p.getActivo());
        verify(productoRepository).save(p);
        verify(productoRepository, never()).delete(any());
    }

    @Test
    void delete_inactivoConHistorial_rechaza() {
        Producto p = Producto.builder().id(id).activo(false).build();
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.of(p));
        when(productoRepository.tieneHistorial(id)).thenReturn(true);

        assertThrows(ValidationException.class, () -> service.delete(id));
        verify(productoRepository, never()).delete(any());
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        when(productoRepository.findWithRelationsById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(id));
    }

    @Test
    void getDashboardStatistics_cuentaPorEstadoYTipo() {
        when(productoRepository.count()).thenReturn(50L);
        when(productoRepository.countByActivo(true)).thenReturn(45L);
        when(productoRepository.countByActivo(false)).thenReturn(5L);
        when(productoRepository.countByEsFraccionable(true)).thenReturn(12L);
        when(productoRepository.countByTipo(TipoProducto.FISICO)).thenReturn(48L);
        when(productoRepository.countByTipo(TipoProducto.SERVICIO)).thenReturn(2L);

        Map<String, Object> d = service.getDashboardStatistics();

        assertEquals(50L, d.get("totalProductos"));
        assertEquals(45L, d.get("productosActivos"));
        assertEquals(5L, d.get("productosInactivos"));
        assertEquals(12L, d.get("productosFraccionables"));
        assertEquals(48L, d.get("productosFisicos"));
        assertEquals(2L, d.get("productosServicios"));
    }
}
