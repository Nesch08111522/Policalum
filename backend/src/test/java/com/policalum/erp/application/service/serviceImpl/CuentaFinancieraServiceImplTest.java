package com.policalum.erp.application.service.serviceImpl;

import java.time.LocalDate;
import org.springframework.data.domain.*;
import com.policalum.erp.exception.ResourceNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.CuentaFinancieraRequestDTO;
import com.policalum.erp.application.dto.request.RegistrarSaldoCuentaRequestDTO;
import com.policalum.erp.application.mapper.CuentaFinancieraMapper;
import com.policalum.erp.application.mapper.SaldoCuentaMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CuentaFinancieraServiceImplTest {

    @Mock CuentaFinancieraRepository cuentaRepository;
    @Mock SaldoCuentaFinancieraRepository saldoRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock CuentaFinancieraMapper mapper;
    @Mock SaldoCuentaMapper saldoMapper;
    @Mock AuditService auditService;
    @InjectMocks CuentaFinancieraServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;
    Sede centro, norte;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL)
                .rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").activa(true).build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").activa(true).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenAnswer(i -> Optional.of(admin));
        lenient().when(sedeRepository.findById(centro.getId())).thenReturn(Optional.of(centro));
        lenient().when(sedeRepository.findById(norte.getId())).thenReturn(Optional.of(norte));
        lenient().when(mapper.toEntity(any())).thenAnswer(i -> new CuentaFinanciera());
        lenient().when(cuentaRepository.save(any())).thenAnswer(i -> {
            CuentaFinanciera c = i.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            return c;
        });
        lenient().when(saldoMapper.toEntity(any())).thenAnswer(i -> new SaldoCuentaFinanciera());
        lenient().when(saldoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crear_asignaSedeMetodosYAudita() {
        // given
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of());
        ArgumentCaptor<CuentaFinanciera> captor = ArgumentCaptor.forClass(CuentaFinanciera.class);

        // when
        service.crear(request(" Caja Centro ", centro, MetodoPago.EFECTIVO));

        // then
        verify(cuentaRepository).save(captor.capture());
        CuentaFinanciera c = captor.getValue();
        assertEquals("Caja Centro", c.getNombre());
        assertSame(centro, c.getSede());
        assertEquals(Set.of(MetodoPago.EFECTIVO), c.getMetodosPago());
        assertTrue(c.getActiva());
        assertSame(admin, c.getCreadoPor());
        verify(auditService).registrar(eq("finanzas_cuentas"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void crear_metodoYaEnCuentaCorporativa_lanzaIllegalState() {
        // given: BCP corporativa ya recibe YAPE; no puede haber otra cuenta con YAPE en Centro
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(existente("BCP", null, MetodoPago.YAPE)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.crear(request("Yape Centro", centro, MetodoPago.YAPE, MetodoPago.PLIN)));
        assertTrue(ex.getMessage().contains("YAPE") && ex.getMessage().contains("BCP"));
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void crear_mismoMetodoEnOtraSede_sePermite() {
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(existente("Caja Norte", norte, MetodoPago.EFECTIVO)));

        assertDoesNotThrow(() -> service.crear(request("Caja Centro", centro, MetodoPago.EFECTIVO)));
        verify(cuentaRepository).save(any());
    }

    @Test
    void crear_sinMetodos_noValidaConflictos() {
        service.crear(request("Ahorros", null));

        verify(cuentaRepository, never()).findByActivaTrueOrderByNombreAsc();
        verify(cuentaRepository).save(any());
    }

    @Test
    void actualizar_noChocaConsigoMisma() {
        CuentaFinanciera actual = existente("Caja Centro", centro, MetodoPago.EFECTIVO);
        when(cuentaRepository.findDetalleById(actual.getId())).thenReturn(Optional.of(actual));
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(actual));

        service.actualizar(actual.getId(), request("Caja Centro principal", centro, MetodoPago.EFECTIVO));

        assertEquals("Caja Centro principal", actual.getNombre());
        verify(auditService).registrar(eq("finanzas_cuentas"), eq(actual.getId()), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void desactivar_cuentaYaInactiva_lanzaIllegalState() {
        CuentaFinanciera c = existente("Vieja", null);
        c.setActiva(false);
        when(cuentaRepository.findDetalleById(c.getId())).thenReturn(Optional.of(c));

        assertThrows(IllegalStateException.class, () -> service.desactivar(c.getId()));
        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void desactivar_activa_laMarcaInactivaSinBorrar() {
        CuentaFinanciera c = existente("BCP", null, MetodoPago.TRANSFERENCIA);
        when(cuentaRepository.findDetalleById(c.getId())).thenReturn(Optional.of(c));

        service.desactivar(c.getId());

        assertFalse(c.getActiva());
        verify(cuentaRepository, never()).delete(any());
    }

    @Test
    void registrarSaldo_sinFecha_usaAhoraYRedondea() {
        CuentaFinanciera c = existente("BCP", null);
        when(cuentaRepository.findDetalleById(c.getId())).thenReturn(Optional.of(c));
        ArgumentCaptor<SaldoCuentaFinanciera> captor = ArgumentCaptor.forClass(SaldoCuentaFinanciera.class);
        LocalDateTime antes = LocalDateTime.now();

        service.registrarSaldo(c.getId(), new RegistrarSaldoCuentaRequestDTO(new BigDecimal("1520.5"), null, "  "));

        verify(saldoRepository).save(captor.capture());
        SaldoCuentaFinanciera s = captor.getValue();
        assertEquals(new BigDecimal("1520.50"), s.getSaldo());
        assertFalse(s.getFechaCorte().isBefore(antes));
        assertNull(s.getObservacion());
        assertSame(c, s.getCuenta());
        assertSame(admin, s.getRegistradoPor());
    }

    @Test
    void registrarSaldo_fechaFutura_lanzaIllegalArgument() {
        CuentaFinanciera c = existente("BCP", null);
        when(cuentaRepository.findDetalleById(c.getId())).thenReturn(Optional.of(c));

        assertThrows(IllegalArgumentException.class, () -> service.registrarSaldo(c.getId(),
                new RegistrarSaldoCuentaRequestDTO(BigDecimal.TEN, LocalDateTime.now().plusDays(1), null)));
        verify(saldoRepository, never()).save(any());
    }

    @Test
    void crear_usuarioNoAdmin_lanzaAccessDenied() {
        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());
        assertThrows(AccessDeniedException.class, () -> service.crear(request("X", null)));
        verifyNoInteractions(cuentaRepository);
    }

    // ===================== HELPERS =====================

    private CuentaFinancieraRequestDTO request(String nombre, Sede sede, MetodoPago... metodos) {
        return new CuentaFinancieraRequestDTO(nombre, TipoCuentaFinanciera.EFECTIVO, null, null,
                sede != null ? sede.getId() : null, new HashSet<>(Arrays.asList(metodos)));
    }

    private CuentaFinanciera existente(String nombre, Sede sede, MetodoPago... metodos) {
        return CuentaFinanciera.builder().id(UUID.randomUUID()).nombre(nombre).tipo(TipoCuentaFinanciera.BANCO)
                .sede(sede).metodosPago(new HashSet<>(Arrays.asList(metodos))).activa(true).build();
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtener_incluyeUltimoSaldoRegistrado() {
        CuentaFinanciera c = existente("BCP", centro, MetodoPago.TRANSFERENCIA);
        SaldoCuentaFinanciera ultimo = new SaldoCuentaFinanciera();
        when(cuentaRepository.findDetalleById(c.getId())).thenReturn(Optional.of(c));
        when(saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(c.getId())).thenReturn(Optional.of(ultimo));

        service.obtener(c.getId());

        verify(mapper).toResponseDto(c, ultimo);
    }

    @Test
    void obtener_sinSaldos_mapeaConUltimoNulo() {
        CuentaFinanciera c = existente("Caja", centro);
        when(cuentaRepository.findDetalleById(c.getId())).thenReturn(Optional.of(c));
        when(saldoRepository.findFirstByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(c.getId())).thenReturn(Optional.empty());

        service.obtener(c.getId());

        verify(mapper).toResponseDto(c, null);
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(cuentaRepository.findDetalleById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(id));
    }

    @Test
    void obtener_noAdmin_lanzaAccessDenied() {
        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());

        assertThrows(AccessDeniedException.class, () -> service.obtener(UUID.randomUUID()));
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void listar_soloActivas_filtraPorSedeYExcluyeCuentasSinSede() {
        CuentaFinanciera enCentro = existente("Caja Centro", centro);
        CuentaFinanciera enNorte = existente("Caja Norte", norte);
        CuentaFinanciera global = existente("BCP", null);
        when(cuentaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of(enCentro, enNorte, global));

        List<?> r = service.listar(centro.getId(), false);

        assertEquals(1, r.size());
        verify(mapper).toResponseDto(eq(enCentro), any());
        verify(mapper, never()).toResponseDto(eq(enNorte), any());
        verify(mapper, never()).toResponseDto(eq(global), any());
        verify(cuentaRepository, never()).findAllByOrderByNombreAsc();
    }

    @Test
    void listar_incluirInactivasSinSede_devuelveTodas() {
        CuentaFinanciera inactiva = existente("Vieja", norte);
        inactiva.setActiva(false);
        when(cuentaRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(existente("A", centro), inactiva, existente("B", null)));

        assertEquals(3, service.listar(null, true).size());
        verify(cuentaRepository, never()).findByActivaTrueOrderByNombreAsc();
    }

    @Test
    void historialSaldos_cuentaExistente_paginaYMapea() {
        UUID id = UUID.randomUUID();
        Pageable p = PageRequest.of(0, 10);
        SaldoCuentaFinanciera s = new SaldoCuentaFinanciera();
        when(cuentaRepository.existsById(id)).thenReturn(true);
        when(saldoRepository.findByCuenta_IdOrderByFechaCorteDescCreatedAtDesc(id, p)).thenReturn(new PageImpl<>(List.of(s)));

        assertEquals(1, service.historialSaldos(id, p).getTotalElements());
        verify(saldoMapper).toResponseDto(s);
    }

    @Test
    void historialSaldos_cuentaInexistente_lanzaNotFoundSinConsultarSaldos() {
        UUID id = UUID.randomUUID();
        when(cuentaRepository.existsById(id)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.historialSaldos(id, PageRequest.of(0, 10)));
        verifyNoInteractions(saldoRepository);
    }
}
