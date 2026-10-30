package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.VentaDetalleResponseDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.service.KardexService;
import com.policalum.erp.application.service.support.RangosConsulta;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Ventas: IGV por línea, salida automática de Kardex, cuadre de pagos y anulación con reversión. */
@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock VentaRepository ventaRepository;
    @Mock DetalleVentaRepository detalleRepository;
    @Mock PagoVentaRepository pagoRepository;
    @Mock SedeRepository sedeRepository;
    @Mock AlmacenRepository almacenRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProductoRepository productoRepository;
    @Mock ServicioRepository servicioRepository;
    @Mock CatalogoFraccionRepository fraccionRepository;
    @Mock TurnoCajaRepository turnoRepository;
    @Mock UserRepository userRepository;
    @Mock KardexService kardexService;
    @Mock KardexRepository kardexRepository;
    @Mock DevolucionRepository devolucionRepository;
    @InjectMocks VentaServiceImpl service;

    private static final String EMAIL = "vendedor@policalum.com";

    Authentication auth;
    User vendedor;
    Sede sede;
    Almacen almacen;
    TurnoCaja turno;
    Producto producto;
    Servicio servicio;

    @BeforeEach
    void setUp() {
        vendedor = User.builder().id(UUID.randomUUID()).nombre("Ana").apellido("Ruiz").email(EMAIL).build();
        SecurityContext context = mock(SecurityContext.class);
        auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_VENDEDOR"))).when(auth).getAuthorities();
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenReturn(Optional.of(vendedor));

        sede = Sede.builder().id(UUID.randomUUID()).nombre("Centro").build();
        almacen = Almacen.builder().id(UUID.randomUUID()).sede(sede).build();
        turno = TurnoCaja.builder().id(UUID.randomUUID()).user(vendedor).sede(sede).estado(EstadoTurno.ABIERTO).build();
        producto = Producto.builder().id(UUID.randomUUID()).nombre("Vidrio 6mm").activo(true).esFraccionable(false)
                .precioVentaBase(new BigDecimal("59.00")).build();
        servicio = Servicio.builder().id(UUID.randomUUID()).nombre("Instalación").activo(true).precio(new BigDecimal("25.00")).build();

        lenient().when(sedeRepository.findById(sede.getId())).thenReturn(Optional.of(sede));
        lenient().when(almacenRepository.findById(almacen.getId())).thenReturn(Optional.of(almacen));
        lenient().when(turnoRepository.findById(turno.getId())).thenReturn(Optional.of(turno));
        lenient().when(productoRepository.findById(producto.getId())).thenReturn(Optional.of(producto));
        lenient().when(servicioRepository.findById(servicio.getId())).thenReturn(Optional.of(servicio));
        lenient().when(ventaRepository.save(any())).thenAnswer(i -> {
            Venta v = i.getArgument(0);
            if (v.getId() == null) v.setId(UUID.randomUUID());
            return v;
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------------------------------------------------------------- helpers

    private LineaVentaCommandDTO lineaProducto(String cantidad) {
        return new LineaVentaCommandDTO(producto.getId(), null, null, new BigDecimal(cantidad));
    }

    private LineaVentaCommandDTO lineaServicio(String cantidad) {
        return new LineaVentaCommandDTO(null, null, servicio.getId(), new BigDecimal(cantidad));
    }

    private PagoVentaCommandDTO pago(MetodoPago m, String monto) {
        return new PagoVentaCommandDTO(m, new BigDecimal(monto), null);
    }

    private RegistrarVentaRequestDTO request(List<LineaVentaCommandDTO> lineas, List<PagoVentaCommandDTO> pagos) {
        return new RegistrarVentaRequestDTO(sede.getId(), almacen.getId(), null, turno.getId(), TipoComprobante.BOLETA,
                null, null, null, null, lineas, pagos);
    }

    private Venta ventaRegistrada(EstadoVenta estado, User duenio) {
        Venta v = Venta.builder().id(UUID.randomUUID()).serieCorrelativo("B001-15").estado(estado).user(duenio).sede(sede)
                .subtotalNeto(new BigDecimal("100.00")).totalIgv(new BigDecimal("18.00")).total(new BigDecimal("118.00")).build();
        lenient().when(ventaRepository.findByIdParaActualizar(v.getId())).thenReturn(Optional.of(v));
        return v;
    }

    private KardexMovimiento movimiento(TipoMovimiento tipo, String cantidad) {
        return KardexMovimiento.builder().id(UUID.randomUUID()).producto(producto).almacen(almacen)
                .tipoMovimiento(tipo).cantidadMovida(new BigDecimal(cantidad)).build();
    }

    // ---------------------------------------------------------------- registrar

    @Test
    void registrar_productoPrecioExacto_desglosaIgvYRegistraSalidaKardex() {
        VentaProcesadaResponseDTO r = service.registrar(request(List.of(lineaProducto("2")), List.of(pago(MetodoPago.EFECTIVO, "118.00"))));

        assertEquals(new BigDecimal("100.00"), r.subtotalNeto());
        assertEquals(new BigDecimal("18.00"), r.totalIgv());
        assertEquals(new BigDecimal("118.00"), r.total());
        assertEquals("COMPLETADA", r.estado());

        ArgumentCaptor<KardexMovimientoRequestDTO> kx = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(kx.capture());
        assertEquals(TipoMovimiento.SALIDA, kx.getValue().tipoMovimiento());
        assertEquals(TipoReferencia.VENTA, kx.getValue().tipoReferencia());
        assertEquals(0, new BigDecimal("2").compareTo(kx.getValue().cantidadMovida()));
        assertSame(almacen, kx.getValue().almacen());
        assertEquals(r.id(), kx.getValue().referenciaId());

        ArgumentCaptor<DetalleVenta> det = ArgumentCaptor.forClass(DetalleVenta.class);
        verify(detalleRepository).save(det.capture());
        assertEquals(new BigDecimal("59.00"), det.getValue().getPrecioUnitarioConIgv());
        assertEquals(new BigDecimal("18.00"), det.getValue().getPorcentajeIgv());
        verify(pagoRepository).save(any(PagoVenta.class));
    }

    @Test
    void registrar_productoMasServicio_servicioNoMueveKardexYTotalesCuadran() {
        VentaProcesadaResponseDTO r = service.registrar(request(List.of(lineaProducto("2"), lineaServicio("1")),
                List.of(pago(MetodoPago.EFECTIVO, "100.00"), pago(MetodoPago.YAPE, "43.00"))));

        // 25.00 / 1.18 = 21.1864 → neto total 121.19, igv 21.81
        assertEquals(new BigDecimal("121.19"), r.subtotalNeto());
        assertEquals(new BigDecimal("21.81"), r.totalIgv());
        assertEquals(new BigDecimal("143.00"), r.total());
        assertEquals(0, r.subtotalNeto().add(r.totalIgv()).compareTo(r.total()));
        verify(kardexService, times(1)).registrarMovimiento(any());
        verify(pagoRepository, times(2)).save(any(PagoVenta.class));
    }

    @Test
    void registrar_conFraccion_usaPrecioDeFraccionYFactorKardex() {
        producto.setEsFraccionable(true);
        CatalogoFraccion media = CatalogoFraccion.builder().id(UUID.randomUUID()).producto(producto).activo(true)
                .factorDescuentoKardex(new BigDecimal("0.50")).precioVenta(new BigDecimal("35.40")).build();
        when(fraccionRepository.findWithProductoById(media.getId())).thenReturn(Optional.of(media));
        LineaVentaCommandDTO l = new LineaVentaCommandDTO(producto.getId(), media.getId(), null, new BigDecimal("3"));

        VentaProcesadaResponseDTO r = service.registrar(request(List.of(l), List.of(pago(MetodoPago.EFECTIVO, "106.20"))));

        assertEquals(new BigDecimal("106.20"), r.total());
        ArgumentCaptor<KardexMovimientoRequestDTO> kx = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService).registrarMovimiento(kx.capture());
        assertEquals(0, new BigDecimal("1.5").compareTo(kx.getValue().cantidadMovida()));
    }

    @Test
    void registrar_canalNulo_asumeLocalYEstadoSunatPendiente() {
        service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00"))));

        ArgumentCaptor<Venta> v = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository, atLeastOnce()).save(v.capture());
        assertEquals(CanalVenta.LOCAL, v.getValue().getCanalVenta());
        assertEquals(EstadoSunat.PENDIENTE, v.getValue().getEstadoSunat());
        assertSame(turno, v.getValue().getTurnoCaja());
    }

    @Test
    void registrar_pagosNoCuadranConTotal_lanzaIllegalArgumentSinGuardarPagos() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.registrar(
                request(List.of(lineaProducto("2")), List.of(pago(MetodoPago.EFECTIVO, "117.99")))));
        assertTrue(ex.getMessage().contains("pagos"));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrar_turnoCerrado_lanzaIllegalStateSinCrearVenta() {
        turno.setEstado(EstadoTurno.CERRADO);

        assertThrows(IllegalStateException.class, () -> service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")))));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void registrar_turnoDeOtroUsuario_lanzaIllegalState() {
        turno.setUser(User.builder().id(UUID.randomUUID()).build());

        assertThrows(IllegalStateException.class, () -> service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")))));
        verifyNoInteractions(kardexService);
    }

    @Test
    void registrar_turnoDeOtraSede_lanzaIllegalState() {
        turno.setSede(Sede.builder().id(UUID.randomUUID()).build());

        assertThrows(IllegalStateException.class, () -> service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")))));
    }

    @Test
    void registrar_almacenDeOtraSede_lanzaIllegalArgument() {
        almacen.setSede(Sede.builder().id(UUID.randomUUID()).build());

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")))));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void registrar_lineaConProductoYServicio_lanzaIllegalArgument() {
        LineaVentaCommandDTO ambos = new LineaVentaCommandDTO(producto.getId(), null, servicio.getId(), BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(ambos), List.of(pago(MetodoPago.EFECTIVO, "1.00")))));
    }

    @Test
    void registrar_lineaSinProductoNiServicio_lanzaIllegalArgument() {
        LineaVentaCommandDTO vacia = new LineaVentaCommandDTO(null, null, null, BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(vacia), List.of(pago(MetodoPago.EFECTIVO, "1.00")))));
    }

    @Test
    void registrar_productoFraccionableSinFraccion_lanzaIllegalArgument() {
        producto.setEsFraccionable(true);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")))));
        verifyNoInteractions(kardexService);
    }

    @Test
    void registrar_fraccionInactiva_lanzaIllegalArgument() {
        CatalogoFraccion inactiva = CatalogoFraccion.builder().id(UUID.randomUUID()).producto(producto).activo(false).build();
        when(fraccionRepository.findWithProductoById(inactiva.getId())).thenReturn(Optional.of(inactiva));

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(
                List.of(new LineaVentaCommandDTO(producto.getId(), inactiva.getId(), null, BigDecimal.ONE)), List.of(pago(MetodoPago.EFECTIVO, "1.00")))));
    }

    @Test
    void registrar_servicioConFraccion_lanzaIllegalArgument() {
        LineaVentaCommandDTO l = new LineaVentaCommandDTO(null, UUID.randomUUID(), servicio.getId(), BigDecimal.ONE);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(l), List.of(pago(MetodoPago.EFECTIVO, "25.00")))));
    }

    @Test
    void registrar_productoInactivo_lanzaIllegalArgument() {
        producto.setActivo(false);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")))));
    }

    @Test
    void registrar_servicioInactivo_lanzaIllegalArgument() {
        servicio.setActivo(false);

        assertThrows(IllegalArgumentException.class, () -> service.registrar(request(List.of(lineaServicio("1")), List.of(pago(MetodoPago.EFECTIVO, "25.00")))));
    }

    @Test
    void registrar_clienteInexistente_lanzaNotFound() {
        UUID clienteId = UUID.randomUUID();
        when(clienteRepository.findById(clienteId)).thenReturn(Optional.empty());
        RegistrarVentaRequestDTO r = new RegistrarVentaRequestDTO(sede.getId(), almacen.getId(), clienteId, turno.getId(),
                TipoComprobante.FACTURA, null, null, null, null, List.of(lineaProducto("1")), List.of(pago(MetodoPago.EFECTIVO, "59.00")));

        assertThrows(ResourceNotFoundException.class, () -> service.registrar(r));
    }

    // ---------------------------------------------------------------- anular

    @Test
    void anular_propiaConSalidas_reingresaSoloSalidasYMarcaAnulada() {
        Venta v = ventaRegistrada(EstadoVenta.COMPLETADA, vendedor);
        when(kardexRepository.findByReferenciaIdAndTipoReferencia(v.getId(), TipoReferencia.VENTA))
                .thenReturn(List.of(movimiento(TipoMovimiento.SALIDA, "2"), movimiento(TipoMovimiento.ENTRADA, "1")));

        VentaProcesadaResponseDTO r = service.anular(v.getId(), new AnularVentaRequestDTO("  cliente desistió "));

        ArgumentCaptor<KardexMovimientoRequestDTO> kx = ArgumentCaptor.forClass(KardexMovimientoRequestDTO.class);
        verify(kardexService, times(1)).registrarMovimiento(kx.capture());
        assertEquals(TipoMovimiento.ENTRADA, kx.getValue().tipoMovimiento());
        assertEquals(TipoReferencia.DEVOLUCION, kx.getValue().tipoReferencia());
        assertEquals(new BigDecimal("2"), kx.getValue().cantidadMovida());
        assertTrue(kx.getValue().observacion().endsWith("cliente desistió"), "el motivo se guarda recortado");
        assertEquals(EstadoVenta.ANULADA, v.getEstado());
        assertEquals("ANULADA", r.estado());
        verify(ventaRepository).findByIdParaActualizar(v.getId());
    }

    @Test
    void anular_adminVentaAjena_permitido() {
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        Venta v = ventaRegistrada(EstadoVenta.COMPLETADA, User.builder().id(UUID.randomUUID()).build());
        when(kardexRepository.findByReferenciaIdAndTipoReferencia(v.getId(), TipoReferencia.VENTA)).thenReturn(List.of(movimiento(TipoMovimiento.SALIDA, "1")));

        service.anular(v.getId(), new AnularVentaRequestDTO("error"));

        assertEquals(EstadoVenta.ANULADA, v.getEstado());
    }

    @Test
    void anular_vendedorVentaAjena_lanzaIllegalStateSinReversion() {
        Venta v = ventaRegistrada(EstadoVenta.COMPLETADA, User.builder().id(UUID.randomUUID()).build());

        assertThrows(IllegalStateException.class, () -> service.anular(v.getId(), new AnularVentaRequestDTO("x")));
        verifyNoInteractions(kardexService);
        assertEquals(EstadoVenta.COMPLETADA, v.getEstado());
    }

    @Test
    void anular_yaAnulada_lanzaIllegalState() {
        Venta v = ventaRegistrada(EstadoVenta.ANULADA, vendedor);

        assertThrows(IllegalStateException.class, () -> service.anular(v.getId(), new AnularVentaRequestDTO("x")));
        verifyNoInteractions(kardexService);
    }

    @Test
    void anular_conDevolucionVigente_lanzaIllegalStateParaNoDuplicarStock() {
        Venta v = ventaRegistrada(EstadoVenta.COMPLETADA, vendedor);
        when(devolucionRepository.existsByVenta_IdAndEstadoIn(eq(v.getId()), anyCollection())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> service.anular(v.getId(), new AnularVentaRequestDTO("x")));
        verifyNoInteractions(kardexService);
    }

    @Test
    void anular_sinMovimientosKardex_lanzaIllegalState() {
        Venta v = ventaRegistrada(EstadoVenta.COMPLETADA, vendedor);
        when(kardexRepository.findByReferenciaIdAndTipoReferencia(v.getId(), TipoReferencia.VENTA)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.anular(v.getId(), new AnularVentaRequestDTO("x")));
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void anular_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(ventaRepository.findByIdParaActualizar(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.anular(id, new AnularVentaRequestDTO("x")));
    }

    // ---------------------------------------------------------------- consultas

    @Test
    void obtener_armaDetalleConLineasYPagos() {
        Venta v = ventaRegistrada(EstadoVenta.COMPLETADA, vendedor);
        when(ventaRepository.findDetalleById(v.getId())).thenReturn(Optional.of(v));
        when(detalleRepository.findByVenta_Id(v.getId())).thenReturn(List.of(
                DetalleVenta.builder().id(UUID.randomUUID()).producto(producto).cantidad(new BigDecimal("2")).subtotal(new BigDecimal("118.00")).build(),
                DetalleVenta.builder().id(UUID.randomUUID()).servicio(servicio).cantidad(BigDecimal.ONE).subtotal(new BigDecimal("25.00")).build()));
        when(pagoRepository.findByVenta_Id(v.getId())).thenReturn(List.of(
                PagoVenta.builder().id(UUID.randomUUID()).metodoPago(MetodoPago.YAPE).monto(new BigDecimal("143.00")).build()));

        VentaDetalleResponseDTO d = service.obtener(v.getId());

        assertEquals("Ana Ruiz", d.usuarioNombre());
        assertEquals(2, d.lineas().size());
        assertEquals("Vidrio 6mm", d.lineas().get(0).productoNombre());
        assertNull(d.lineas().get(1).productoId());
        assertEquals("Instalación", d.lineas().get(1).servicioNombre());
        assertEquals(1, d.pagos().size());
        assertNull(d.clienteId());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(ventaRepository.findDetalleById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(id));
    }

    @Test
    void buscar_desdePosteriorAHasta_lanzaIllegalArgument() {
        LocalDateTime ahora = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> service.buscar(null, null, null, null, null, ahora, ahora.minusDays(1), PageRequest.of(0, 10)));
        verifyNoInteractions(ventaRepository);
    }

    @Test
    void buscar_mapeaClienteOpcional() {
        Venta sinCliente = ventaRegistrada(EstadoVenta.COMPLETADA, vendedor);
        Venta conCliente = ventaRegistrada(EstadoVenta.COMPLETADA, vendedor);
        conCliente.setCliente(Cliente.builder().id(UUID.randomUUID()).nombre("Constructora Lima").build());
        Pageable p = PageRequest.of(0, 10);
        when(ventaRepository.buscar(sede.getId(), null, null, null, null, RangosConsulta.DESDE_ABIERTO, RangosConsulta.HASTA_ABIERTO, p)).thenReturn(new PageImpl<>(List.of(sinCliente, conCliente)));

        var page = service.buscar(sede.getId(), null, null, null, null, null, null, p);

        assertNull(page.getContent().get(0).clienteNombre());
        assertEquals("Constructora Lima", page.getContent().get(1).clienteNombre());
    }
}
