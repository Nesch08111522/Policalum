package com.policalum.erp.application.service.serviceImpl;

import org.springframework.data.domain.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.application.dto.response.CotizacionResponseDTO;
import com.policalum.erp.application.dto.response.CotizacionListadoDTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.policalum.erp.application.dto.request.*;
import com.policalum.erp.application.dto.response.ParametrosVentasDTO;
import com.policalum.erp.application.dto.response.VentaProcesadaResponseDTO;
import com.policalum.erp.application.mapper.*;
import com.policalum.erp.application.service.*;
import com.policalum.erp.application.service.support.RangosConsulta;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;

@ExtendWith(MockitoExtension.class)
class CotizacionServiceImplTest {
 @Mock CotizacionRepository cotizacionRepository; @Mock DetalleCotizacionRepository detalleRepository; @Mock SedeRepository sedeRepository; @Mock ClienteRepository clienteRepository; @Mock ProductoRepository productoRepository; @Mock ServicioRepository servicioRepository; @Mock CatalogoFraccionRepository fraccionRepository; @Mock UserRepository userRepository; @Mock CotizacionMapper cotizacionMapper; @Mock DetalleCotizacionMapper detalleMapper; @Mock VentaService ventaService; @Mock VentaRepository ventaRepository; @Mock AuditService auditService; @Mock ConfiguracionService configuracionService; @InjectMocks CotizacionServiceImpl service;
 UUID cotizacionId=UUID.randomUUID(), sedeId=UUID.randomUUID(), productoId=UUID.randomUUID(); User usuario; Sede sede;
    /**
     * Stubs compartidos: no todos los tests los usan (p. ej. create() no consulta getAuthorities,
     * y los tests que fallan por estado no llegan a buscar la sede ni los detalles). Con la
     * estrictez por defecto de MockitoExtension eso lanzaba UnnecessaryStubbingException, por
     * eso van con lenient(). Los stubs propios de cada test siguen siendo estrictos.
     */
    @BeforeEach
    void setUp() {
        lenient().when(configuracionService.ventas()).thenReturn(ParametrosVentasDTO.DEFAULTS);
        usuario = User.builder().id(UUID.randomUUID()).nombre("Admin").apellido("Prueba").email("test@policalum.com").activo(true).build();
        sede = Sede.builder().id(sedeId).nombre("Central").build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("test@policalum.com");
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue("test@policalum.com")).thenReturn(Optional.of(usuario));
        lenient().when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(sede));
        lenient().when(detalleRepository.findByCotizacion_Id(any())).thenReturn(List.of());
    }
 @AfterEach void tearDown(){ SecurityContextHolder.clearContext(); }
 @Test void create_calculaTotalCorrectamente(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.EMITIDA); when(cotizacionMapper.toEntity(any())).thenReturn(c); when(cotizacionRepository.save(any())).thenAnswer(i->i.getArgument(0)); when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto())); when(detalleMapper.toEntity(any())).thenReturn(new DetalleCotizacion()); ArgumentCaptor<DetalleCotizacion> captor=ArgumentCaptor.forClass(DetalleCotizacion.class);
  // when
  service.create(request(LocalDate.now().plusDays(3)));
  // then
  verify(detalleRepository).save(captor.capture()); assertEquals(new BigDecimal("236.00"),c.getTotal()); assertEquals(new BigDecimal("36.00"),captor.getValue().getIgvLinea()); }
 @Test void create_productoFraccionableRequiereFraccion(){ // given
  Producto p=producto();p.setEsFraccionable(true); when(cotizacionMapper.toEntity(any())).thenReturn(cotizacion(EstadoCotizacion.EMITIDA));when(cotizacionRepository.save(any())).thenAnswer(i->i.getArgument(0));when(productoRepository.findById(productoId)).thenReturn(Optional.of(p));
  // when / then
  assertTrue(assertThrows(IllegalArgumentException.class,()->service.create(request(null))).getMessage().contains("fracción")); }
 @Test void create_fechaVigenciaPorDefectoEs15Dias(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.EMITIDA); configurarCreate(c);
  // when
  service.create(request(null));
  // then
  assertEquals(LocalDate.now().plusDays(15),c.getFechaVigencia()); }
 @Test void create_fechaVigenciaEnElPasadoLanzaExcepcion(){ // given
  when(cotizacionMapper.toEntity(any())).thenReturn(cotizacion(EstadoCotizacion.EMITIDA));
  // when / then
  assertThrows(IllegalArgumentException.class,()->service.create(request(LocalDate.now().minusDays(1)))); }
 @Test void update_soloPermiteEstadoEmitida(){ // given
  when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(cotizacion(EstadoCotizacion.ACEPTADA)));
  // when / then
  assertThrows(IllegalStateException.class,()->service.update(cotizacionId,request(null))); }
 @Test void aceptar_cotizacionEmitida_cambiaEstadoAAceptada(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.EMITIDA);when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(c));
  // when
  service.aceptar(cotizacionId);
  // then
  assertEquals(EstadoCotizacion.ACEPTADA,c.getEstado()); }
 @Test void aceptar_cotizacionVencida_cambiaEstadoAVencidaYLanzaExcepcion(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.EMITIDA);c.setFechaVigencia(LocalDate.now().minusDays(1));when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(c));
  // when / then
  assertThrows(IllegalStateException.class,()->service.aceptar(cotizacionId));assertEquals(EstadoCotizacion.VENCIDA,c.getEstado()); }
 @Test void rechazar_guardaMotivoEnObservaciones(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.EMITIDA);c.setObservaciones("Inicial");when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(c));
  // when
  service.rechazar(cotizacionId,new RechazarCotizacionRequestDTO("Cliente pospone"));
  // then
  assertEquals(EstadoCotizacion.RECHAZADA,c.getEstado());assertTrue(c.getObservaciones().contains("Cliente pospone")); }
 @Test void convertir_soloPermiteEstadoAceptada(){ // given
  when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(cotizacion(EstadoCotizacion.EMITIDA)));
  // when
  IllegalStateException e=assertThrows(IllegalStateException.class,()->service.convertir(cotizacionId,conversion()));
  // then
  assertTrue(e.getMessage().contains("EMITIDA"));assertTrue(e.getMessage().contains("ACEPTADA")); }
 @Test void convertir_cotizacionAceptada_llamaVentaServiceYVincula(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.ACEPTADA);DetalleCotizacion d=new DetalleCotizacion();d.setProducto(producto());d.setCantidad(BigDecimal.ONE);UUID ventaId=UUID.randomUUID();Venta venta=new Venta();venta.setId(ventaId);when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(c));when(detalleRepository.findByCotizacion_Id(cotizacionId)).thenReturn(List.of(d));when(ventaService.registrar(any())).thenReturn(new VentaProcesadaResponseDTO(ventaId,"B001-1",BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("118"),"COMPLETADA"));when(ventaRepository.findById(ventaId)).thenReturn(Optional.of(venta));when(cotizacionRepository.save(c)).thenReturn(c);
  // when
  service.convertir(cotizacionId,conversion());
  // then
  verify(ventaService).registrar(any());verify(cotizacionRepository).save(c);assertEquals(EstadoCotizacion.CONVERTIDA,c.getEstado());assertSame(venta,c.getVenta()); }
 @Test void delete_soloPermiteEstadoEmitida(){ // given
  when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(cotizacion(EstadoCotizacion.ACEPTADA)));
  // when / then
  assertThrows(IllegalStateException.class,()->service.delete(cotizacionId)); }
 @Test void delete_eliminaDetallesAntesDeCotizacion(){ // given
  Cotizacion c=cotizacion(EstadoCotizacion.EMITIDA);when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(c));
  // when
  service.delete(cotizacionId);
  // then
  InOrder order=inOrder(detalleRepository,cotizacionRepository);order.verify(detalleRepository).deleteByCotizacion_Id(cotizacionId);order.verify(cotizacionRepository).delete(c); }
 private void configurarCreate(Cotizacion c){when(cotizacionMapper.toEntity(any())).thenReturn(c);when(cotizacionRepository.save(any())).thenAnswer(i->i.getArgument(0));when(productoRepository.findById(productoId)).thenReturn(Optional.of(producto()));when(detalleMapper.toEntity(any())).thenReturn(new DetalleCotizacion());}
 private CotizacionRequestDTO request(LocalDate fecha){return new CotizacionRequestDTO(sedeId,null,fecha,"",List.of(new DetalleCotizacionCommandDTO(productoId,null,null,new BigDecimal("2"))));}
 private ConvertirCotizacionRequestDTO conversion(){return new ConvertirCotizacionRequestDTO(UUID.randomUUID(),UUID.randomUUID(),TipoComprobante.BOLETA,CanalVenta.LOCAL,null,List.of(new PagoVentaCommandDTO(MetodoPago.EFECTIVO,new BigDecimal("118"),null)));}
 private Producto producto(){return Producto.builder().id(productoId).nombre("Plancha").precioVentaBase(new BigDecimal("118.00")).esFraccionable(false).activo(true).build();}
 private Cotizacion cotizacion(EstadoCotizacion estado){return Cotizacion.builder().id(cotizacionId).sede(sede).user(usuario).estado(estado).total(BigDecimal.ZERO).fechaVigencia(LocalDate.now().plusDays(5)).createdAt(LocalDateTime.now()).build();}

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void findById_armaRespuestaConLineasYUsuario() {
        Cotizacion c = cotizacion(EstadoCotizacion.EMITIDA);
        c.setCliente(Cliente.builder().id(UUID.randomUUID()).nombre("Constructora Lima").build());
        when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.of(c));
        when(detalleRepository.findByCotizacion_Id(cotizacionId)).thenReturn(List.of(
                DetalleCotizacion.builder().id(UUID.randomUUID()).producto(producto()).cantidad(new BigDecimal("2")).build()));

        CotizacionResponseDTO r = service.findById(cotizacionId);

        assertEquals("Admin Prueba", r.usuarioNombre());
        assertEquals("Central", r.sedeNombre());
        assertEquals("Constructora Lima", r.clienteNombre());
        assertEquals(1, r.lineas().size());
        assertNull(r.ventaId());
    }

    @Test
    void findById_inexistente_lanzaNotFound() {
        when(cotizacionRepository.findById(cotizacionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(cotizacionId));
    }

    @Test
    void search_mapeaListadoConClienteOpcional() {
        Pageable p = PageRequest.of(0, 10);
        Cotizacion c = cotizacion(EstadoCotizacion.EMITIDA);
        when(cotizacionRepository.buscar(null, sedeId, EstadoCotizacion.EMITIDA, RangosConsulta.DESDE_ABIERTO, RangosConsulta.HASTA_ABIERTO, p)).thenReturn(new PageImpl<>(List.of(c)));

        Page<CotizacionListadoDTO> r = service.search(null, sedeId, EstadoCotizacion.EMITIDA, null, null, p);

        assertEquals(1, r.getTotalElements());
        assertNull(r.getContent().get(0).clienteId());
        assertEquals(sedeId, r.getContent().get(0).sedeId());
    }

    @Test
    void search_desdePosteriorAHasta_lanzaIllegalArgument() {
        LocalDateTime ahora = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () -> service.search(null, null, null, ahora, ahora.minusHours(1), PageRequest.of(0, 10)));
        verifyNoInteractions(cotizacionRepository);
    }

    @Test
    void vencidas_marcaComoVencidasLasEmitidasConVigenciaPasada() {
        Cotizacion a = cotizacion(EstadoCotizacion.EMITIDA);
        Cotizacion b = Cotizacion.builder().id(UUID.randomUUID()).sede(sede).user(usuario).estado(EstadoCotizacion.EMITIDA)
                .total(BigDecimal.TEN).fechaVigencia(LocalDate.now().minusDays(1)).build();
        when(cotizacionRepository.findByEstadoAndFechaVigenciaBefore(EstadoCotizacion.EMITIDA, LocalDate.now())).thenReturn(List.of(a, b));
        when(cotizacionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        List<CotizacionListadoDTO> r = service.vencidas();

        assertEquals(2, r.size());
        assertEquals(EstadoCotizacion.VENCIDA, a.getEstado());
        assertEquals(EstadoCotizacion.VENCIDA, b.getEstado());
        assertTrue(r.stream().allMatch(x -> x.estado() == EstadoCotizacion.VENCIDA));
        verify(cotizacionRepository, times(2)).save(any());
    }

    @Test
    void vencidas_ninguna_noGuarda() {
        when(cotizacionRepository.findByEstadoAndFechaVigenciaBefore(any(), any())).thenReturn(List.of());

        assertTrue(service.vencidas().isEmpty());
        verify(cotizacionRepository, never()).save(any());
    }

    @Test
    void dashboard_cuentaCadaEstado() {
        when(cotizacionRepository.countByEstado(EstadoCotizacion.EMITIDA)).thenReturn(7L);
        when(cotizacionRepository.countByEstado(EstadoCotizacion.PENDIENTE)).thenReturn(4L);
        when(cotizacionRepository.countByEstado(EstadoCotizacion.ACEPTADA)).thenReturn(3L);
        when(cotizacionRepository.countByEstado(EstadoCotizacion.CONVERTIDA)).thenReturn(2L);
        when(cotizacionRepository.countByEstado(EstadoCotizacion.VENCIDA)).thenReturn(1L);

        Map<String, Object> d = service.dashboard();

        assertEquals(7L, d.get("emitidas"));
        assertEquals(4L, d.get("pendientesWeb"));
        assertEquals(3L, d.get("aceptadas"));
        assertEquals(2L, d.get("convertidas"));
        assertEquals(1L, d.get("vencidas"));
    }
}
