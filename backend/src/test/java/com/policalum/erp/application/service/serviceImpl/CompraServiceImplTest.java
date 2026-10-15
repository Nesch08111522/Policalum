package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.mapper.CompraMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import jakarta.validation.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Módulo 1: compras — cálculo de IGV, recepción (Kardex + costo promedio + CxP) y transiciones de estado. */
@ExtendWith(MockitoExtension.class)
class CompraServiceImplTest {

    @Mock CompraRepository compraRepository;
    @Mock DetalleCompraRepository detalleCompraRepository;
    @Mock FinanzasCuentaPorPagarRepository cuentaPorPagarRepository;
    @Mock ProveedorRepository proveedorRepository;
    @Mock SedeRepository sedeRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock ProductoRepository productoRepository;
    @Mock CatalogoFraccionRepository catalogoFraccionRepository;
    @Mock StockRepository stockRepository;
    @Mock UserRepository userRepository;
    @Mock KardexService kardexService;
    @Mock AuditService auditService;
    @Mock CompraMapper compraMapper;
    @InjectMocks CompraServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    private static final LocalDate HOY = LocalDate.now();

    User admin;
    Sede sede;
    Proveedor proveedor;
    Producto producto;
    Almacen almacen;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").email(EMAIL).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenReturn(Optional.of(admin));

        sede = Sede.builder().id(UUID.randomUUID()).nombre("Centro").build();
        proveedor = Proveedor.builder().id(UUID.randomUUID()).nombreRazon("Aluminios SAC").activo(true).diasCredito(0).build();
        producto = Producto.builder().id(UUID.randomUUID()).nombre("Perfil 3/4").activo(true).build();
        almacen = Almacen.builder().id(UUID.randomUUID()).nombre("Principal").sede(sede).build();

        lenient().when(proveedorRepository.findById(proveedor.getId())).thenReturn(Optional.of(proveedor));
        lenient().when(sedeRepository.findById(sede.getId())).thenReturn(Optional.of(sede));
        lenient().when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        lenient().when(almacenRepository.findById(almacen.getId())).thenReturn(Optional.of(almacen));
        lenient().when(compraRepository.save(any())).thenAnswer(i -> {
            Compra c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
        lenient().when(cuentaPorPagarRepository.save(any())).thenAnswer(i -> {
            FinanzasCuentaPorPagar c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID()); // JPA asigna el id sobre la misma instancia
            return c;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- helpers

    private DetalleCompraCommandDTO linea(String precioConIgv, String cantidad) {
        return new DetalleCompraCommandDTO(producto.getId(), null, almacen.getId(), new BigDecimal(cantidad), new BigDecimal(precioConIgv));
    }

    private CompraRequestDTO request(LocalDate emision, DetalleCompraCommandDTO... lineas) {
        return new CompraRequestDTO(proveedor.getId(), sede.getId(), "F001-123", emision, null, "obs", List.of(lineas));
    }

    private Compra compraEnEstado(EstadoCompra estado) {
        Compra c = new Compra(); // constructor JPA: inicializa colecciones como lo hace Hibernate
        c.setId(UUID.randomUUID());
        c.setNumeroOrdenCompra("OC-2026-0007");
        c.setProveedor(proveedor);
        c.setSede(sede);
        c.setEstadoCompra(estado);
        c.setTotal(new BigDecimal("1180.00"));
        lenient().when(compraRepository.findWithDetailsById(c.getId())).thenReturn(Optional.of(c));
        return c;
    }

    private DetalleCompra detalle(Compra compra, String cantidad, String valorNeto, CatalogoFraccion fraccion) {
        DetalleCompra d = DetalleCompra.builder().id(UUID.randomUUID()).producto(producto).almacen(almacen)
                .cantidad(new BigDecimal(cantidad)).valorUnitarioNeto(new BigDecimal(valorNeto)).fraccion(fraccion).build();
        compra.addDetalle(d);
        return d;
    }

    private Compra compraGuardada() {
        ArgumentCaptor<Compra> captor = ArgumentCaptor.forClass(Compra.class);
        verify(compraRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- crear

    @Test
    void crear_lineaExacta_desglosaIgvYTotalesEnBorrador() {
        when(compraRepository.findMaxCorrelativoByYear(HOY.getYear())).thenReturn(6);

        service.crear(request(HOY, linea("118.00", "10")));

        Compra c = compraGuardada();
        assertEquals(String.format("OC-%d-0007", HOY.getYear()), c.getNumeroOrdenCompra());
        assertEquals(new BigDecimal("1000.00"), c.getSubtotalNeto());
        assertEquals(new BigDecimal("180.00"), c.getTotalIgv());
        assertEquals(new BigDecimal("1180.00"), c.getTotal());
        assertEquals(EstadoCompra.BORRADOR, c.getEstadoCompra());
        assertEquals(EstadoRecepcion.PENDIENTE, c.getEstadoRecepcion());
        assertEquals(Moneda.PEN, c.getMoneda());
        assertSame(admin, c.getUser());
        DetalleCompra d = c.getDetalles().get(0);
        assertEquals(new BigDecimal("100.0000"), d.getValorUnitarioNeto());
        assertEquals(new BigDecimal("18.00"), d.getPorcentajeIgv());
        assertEquals(BigDecimal.ZERO, d.getCantidadRecibida());
        assertSame(c, d.getCompra());
        verify(auditService).registrar(eq("compras"), eq(c.getId()), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_variasLineasConRedondeo_netoMasIgvCuadraConTotal() {
        when(compraRepository.findMaxCorrelativoByYear(anyInt())).thenReturn(null);

        service.crear(request(HOY, linea("118.00", "10"), linea("10.00", "3")));

        Compra c = compraGuardada();
        // 10.00 / 1.18 = 8.4746 → neto 25.42, igv 4.58, subtotal 30.00
        assertEquals(new BigDecimal("1025.42"), c.getSubtotalNeto());
        assertEquals(new BigDecimal("184.58"), c.getTotalIgv());
        assertEquals(new BigDecimal("1210.00"), c.getTotal());
        assertEquals(0, c.getSubtotalNeto().add(c.getTotalIgv()).compareTo(c.getTotal()));
        assertTrue(c.getNumeroOrdenCompra().endsWith("-0001"), "sin compras previas el correlativo arranca en 0001");
        assertEquals(2, c.getDetalles().size());
    }

    @Test
    void crear_proveedorConCredito_calculaFechaVencimiento() {
        proveedor.setDiasCredito(30);
        LocalDate emision = HOY.minusDays(5);

        service.crear(request(emision, linea("118.00", "1")));

        assertEquals(emision.plusDays(30), compraGuardada().getFechaVencimientoPago());
    }

    @Test
    void crear_proveedorContado_sinFechaVencimiento() {
        service.crear(request(HOY, linea("118.00", "1")));

        assertNull(compraGuardada().getFechaVencimientoPago());
    }

    @Test
    void crear_proveedorInactivo_lanzaValidationSinGuardar() {
        proveedor.setActivo(false);

        assertThrows(ValidationException.class, () -> service.crear(request(HOY, linea("118.00", "1"))));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void crear_fechaEmisionFutura_lanzaValidation() {
        assertThrows(ValidationException.class, () -> service.crear(request(HOY.plusDays(1), linea("118.00", "1"))));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void crear_almacenDeOtraSede_lanzaValidation() {
        almacen.setSede(Sede.builder().id(UUID.randomUUID()).build());

        ValidationException ex = assertThrows(ValidationException.class, () -> service.crear(request(HOY, linea("118.00", "1"))));
        assertTrue(ex.getMessage().contains("no pertenece a la sede"));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void crear_productoInactivo_lanzaValidation() {
        producto.setActivo(false);

        assertThrows(ValidationException.class, () -> service.crear(request(HOY, linea("118.00", "1"))));
    }

    @Test
    void crear_fraccionDeOtroProducto_lanzaValidation() {
        UUID fraccionId = UUID.randomUUID();
        CatalogoFraccion ajena = CatalogoFraccion.builder().id(fraccionId).activo(true)
                .producto(Producto.builder().id(UUID.randomUUID()).build()).build();
        when(catalogoFraccionRepository.findWithProductoById(fraccionId)).thenReturn(Optional.of(ajena));
        DetalleCompraCommandDTO l = new DetalleCompraCommandDTO(producto.getId(), fraccionId, almacen.getId(), BigDecimal.ONE, new BigDecimal("50.00"));

        assertThrows(ValidationException.class, () -> service.crear(request(HOY, l)));
    }

    @Test
    void crear_fraccionInactiva_lanzaValidation() {
        UUID fraccionId = UUID.randomUUID();
        CatalogoFraccion inactiva = CatalogoFraccion.builder().id(fraccionId).activo(false).producto(producto).build();
        when(catalogoFraccionRepository.findWithProductoById(fraccionId)).thenReturn(Optional.of(inactiva));
        DetalleCompraCommandDTO l = new DetalleCompraCommandDTO(producto.getId(), fraccionId, almacen.getId(), BigDecimal.ONE, new BigDecimal("50.00"));

        assertThrows(ValidationException.class, () -> service.crear(request(HOY, l)));
    }

    @Test
    void crear_proveedorInexistente_lanzaNotFound() {
        UUID otro = UUID.randomUUID();
        when(proveedorRepository.findById(otro)).thenReturn(Optional.empty());
        CompraRequestDTO r = new CompraRequestDTO(otro, sede.getId(), null, HOY, null, null, List.of(linea("1.18", "1")));

        assertThrows(ResourceNotFoundException.class, () -> service.crear(r));
    }

    // ---------------------------------------------------------------- actualizar

    @Test
    void actualizar_borrador_reemplazaLineasYRecalcula() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        detalle(c, "99", "1.0000", null);
        c.setFechaVencimientoPago(HOY.plusDays(60));

        service.actualizar(c.getId(), request(HOY, linea("236.00", "2")));

        assertEquals(1, c.getDetalles().size(), "las líneas viejas se descartan");
        assertEquals(new BigDecimal("400.00"), c.getSubtotalNeto());
        assertEquals(new BigDecimal("72.00"), c.getTotalIgv());
        assertEquals(new BigDecimal("472.00"), c.getTotal());
        assertNull(c.getFechaVencimientoPago(), "proveedor al contado limpia el vencimiento anterior");
        verify(compraRepository).flush();
        verify(auditService).registrar(eq("compras"), eq(c.getId()), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void actualizar_noBorrador_lanzaIllegalState() {
        Compra c = compraEnEstado(EstadoCompra.RECIBIDA);

        assertThrows(IllegalStateException.class, () -> service.actualizar(c.getId(), request(HOY, linea("1.18", "1"))));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void actualizar_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(compraRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(id, request(HOY, linea("1.18", "1"))));
    }

    // ---------------------------------------------------------------- recibir

    @Test
    void recibir_sinStockPrevio_entradaKardexCostoNuevoYCxpPendiente() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        c.setFechaVencimientoPago(HOY.plusDays(30));
        DetalleCompra d = detalle(c, "10", "100.0000", null);
        when(stockRepository.findByProducto_Id(producto.getId())).thenReturn(List.of());

        service.recibir(c.getId(), new RecepcionCompraRequestDTO("F001-999", null));

        ArgumentCaptor<KardexMovimientoRequestDTO> kx = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(kx.capture());
        assertEquals(TipoMovimiento.ENTRADA, kx.getValue().tipoMovimiento());
        assertEquals(TipoReferencia.COMPRA, kx.getValue().tipoReferencia());
        assertEquals(new BigDecimal("10"), kx.getValue().cantidadMovida());
        assertEquals(c.getId(), kx.getValue().referenciaId());
        assertSame(admin, kx.getValue().user());

        assertEquals(new BigDecimal("10"), d.getCantidadRecibida());
        assertEquals(new BigDecimal("100.0000"), producto.getPrecioCompra());
        verify(detalleCompraRepository).save(d);

        ArgumentCaptor<FinanzasCuentaPorPagar> cxp = ArgumentCaptor.forClass(FinanzasCuentaPorPagar.class);
        verify(cuentaPorPagarRepository).save(cxp.capture());
        assertEquals(new BigDecimal("1180.00"), cxp.getValue().getMontoTotal());
        assertEquals(BigDecimal.ZERO, cxp.getValue().getMontoPagado());
        assertEquals(EstadoCxP.PENDIENTE, cxp.getValue().getEstado());
        assertEquals(TipoReferenciaCxP.COMPRA, cxp.getValue().getTipoReferencia());
        assertEquals(HOY.plusDays(30), cxp.getValue().getFechaVencimiento());
        assertSame(c, cxp.getValue().getCompra());

        assertEquals(EstadoCompra.RECIBIDA, c.getEstadoCompra());
        assertEquals(EstadoRecepcion.RECIBIDO, c.getEstadoRecepcion());
        assertEquals("F001-999", c.getNumeroFacturaProveedor());
    }

    @Test
    void recibir_conStockPrevio_costoPromedioPonderado() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        detalle(c, "10", "100.0000", null);
        producto.setPrecioCompra(new BigDecimal("90.00"));
        when(stockRepository.findByProducto_Id(producto.getId())).thenReturn(List.of(
                Stock.builder().cantidad(new BigDecimal("15")).build(),
                Stock.builder().cantidad(new BigDecimal("5")).build(),
                Stock.builder().cantidad(null).build()));

        service.recibir(c.getId(), null);

        // (20 × 90 + 10 × 100) / 30 = 93.33
        assertEquals(new BigDecimal("93.33"), producto.getPrecioCompra());
        verify(productoRepository).save(producto);
    }

    @Test
    void recibir_productoSinCostoPrevio_tomaCeroComoCostoActual() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        detalle(c, "10", "100.0000", null);
        producto.setPrecioCompra(null);
        when(stockRepository.findByProducto_Id(producto.getId())).thenReturn(List.of(Stock.builder().cantidad(new BigDecimal("10")).build()));

        service.recibir(c.getId(), null);

        assertEquals(new BigDecimal("50.00"), producto.getPrecioCompra());
    }

    @Test
    void recibir_conFraccion_multiplicaPorFactorKardex() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        CatalogoFraccion media = CatalogoFraccion.builder().id(UUID.randomUUID()).producto(producto).factorDescuentoKardex(new BigDecimal("0.5")).build();
        detalle(c, "4", "40.0000", media);
        when(stockRepository.findByProducto_Id(producto.getId())).thenReturn(List.of());

        service.recibir(c.getId(), null);

        ArgumentCaptor<KardexMovimientoRequestDTO> kx = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(kx.capture());
        assertEquals(0, new BigDecimal("2").compareTo(kx.getValue().cantidadMovida()));
    }

    @Test
    void recibir_variasLineas_unMovimientoKardexPorLineaYUnaSolaCxp() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        detalle(c, "1", "10.0000", null);
        detalle(c, "2", "10.0000", null);
        when(stockRepository.findByProducto_Id(any())).thenReturn(List.of());

        service.recibir(c.getId(), null);

        verify(kardexService, times(2)).registrarMovimiento(any());
        verify(cuentaPorPagarRepository, times(1)).save(any());
    }

    @Test
    void recibir_noBorrador_lanzaIllegalStateSinTocarKardex() {
        Compra c = compraEnEstado(EstadoCompra.RECIBIDA);
        detalle(c, "1", "1.0000", null);

        assertThrows(IllegalStateException.class, () -> service.recibir(c.getId(), null));
        verifyNoInteractions(kardexService, cuentaPorPagarRepository);
    }

    @Test
    void recibir_sinDetalles_lanzaIllegalState() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);

        assertThrows(IllegalStateException.class, () -> service.recibir(c.getId(), null));
        verifyNoInteractions(kardexService);
    }

    @Test
    void recibir_errorEnKardex_propagaYNoCreaCxp() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        detalle(c, "1", "1.0000", null);
        when(kardexService.registrarMovimiento(any())).thenThrow(new IllegalStateException("stock bloqueado"));

        assertThrows(IllegalStateException.class, () -> service.recibir(c.getId(), null));
        verifyNoInteractions(cuentaPorPagarRepository);
        assertEquals(EstadoCompra.BORRADOR, c.getEstadoCompra());
    }

    // ---------------------------------------------------------------- anular / eliminar

    @Test
    void anular_borrador_marcaAnuladaConMotivoYUsuario() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);

        service.anular(c.getId(), new AnularCompraRequestDTO("Precio errado"));

        assertEquals(EstadoCompra.ANULADA, c.getEstadoCompra());
        assertEquals("Precio errado", c.getMotivoAnulacion());
        assertSame(admin, c.getAnuladoPor());
        assertNotNull(c.getFechaAnulacion());
        verify(auditService).registrar(eq("compras"), eq(c.getId()), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void anular_recibida_lanzaIllegalStateSugiriendoDevolucion() {
        Compra c = compraEnEstado(EstadoCompra.RECIBIDA);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.anular(c.getId(), new AnularCompraRequestDTO("x")));
        assertTrue(ex.getMessage().contains("devolución"));
        verify(compraRepository, never()).save(any());
    }

    @Test
    void anular_yaAnulada_lanzaIllegalState() {
        Compra c = compraEnEstado(EstadoCompra.ANULADA);

        assertThrows(IllegalStateException.class, () -> service.anular(c.getId(), new AnularCompraRequestDTO("x")));
    }

    @Test
    void eliminar_borrador_borraYAudita() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);

        service.eliminar(c.getId());

        verify(compraRepository).delete(c);
        verify(auditService).registrar(eq("compras"), eq(c.getId()), eq(AccionAudit.DELETE), anyMap());
    }

    @Test
    void eliminar_noBorrador_lanzaIllegalState() {
        Compra c = compraEnEstado(EstadoCompra.ANULADA);

        assertThrows(IllegalStateException.class, () -> service.eliminar(c.getId()));
        verify(compraRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- consultas

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(compraRepository.findWithDetailsById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(id));
    }

    @Test
    void obtener_existente_mapeaConDetalles() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);

        service.obtener(c.getId());

        verify(compraMapper).toResponseDto(c);
    }

    @Test
    void buscar_delegaFiltrosAlRepositorioYMapea() {
        Compra c = compraEnEstado(EstadoCompra.BORRADOR);
        Pageable p = PageRequest.of(0, 10);
        when(compraRepository.search(proveedor.getId(), sede.getId(), EstadoCompra.BORRADOR, HOY, HOY, p))
                .thenReturn(new PageImpl<>(List.of(c)));

        assertEquals(1, service.buscar(proveedor.getId(), sede.getId(), EstadoCompra.BORRADOR, HOY, HOY, p).getTotalElements());
        verify(compraMapper).toListadoDto(c);
    }

    @Test
    void dashboard_cuentaPorEstadoYCxp() {
        when(compraRepository.count()).thenReturn(10L);
        when(compraRepository.countByEstadoCompra(EstadoCompra.BORRADOR)).thenReturn(3L);
        when(compraRepository.countByEstadoCompra(EstadoCompra.RECIBIDA)).thenReturn(6L);
        when(compraRepository.countByEstadoCompra(EstadoCompra.ANULADA)).thenReturn(1L);
        when(cuentaPorPagarRepository.countByEstado(EstadoCxP.PENDIENTE)).thenReturn(4L);
        when(cuentaPorPagarRepository.countByEstado(EstadoCxP.PAGADO)).thenReturn(2L);

        Map<String, Object> d = service.dashboard();

        assertEquals(10L, d.get("totalCompras"));
        assertEquals(3L, d.get("comprasBorrador"));
        assertEquals(6L, d.get("comprasRecibidas"));
        assertEquals(1L, d.get("comprasAnuladas"));
        assertEquals(4L, d.get("cxpPendientes"));
        assertEquals(2L, d.get("cxpPagadas"));
    }
}
