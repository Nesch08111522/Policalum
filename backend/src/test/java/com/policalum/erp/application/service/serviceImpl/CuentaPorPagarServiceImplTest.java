package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.PagoCxPRequestDTO;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.domain.enums.*;
import com.policalum.erp.domain.model.*;
import com.policalum.erp.infrastructure.repository.*;
import jakarta.validation.ValidationException;
import com.policalum.erp.application.dto.response.CuentaPorPagarResponseDTO;
import com.policalum.erp.application.dto.response.ResumenCxPDashboardDTO;
import com.policalum.erp.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/** Módulo 6: cada pago de una CxP debe quedar registrado como abono con fecha y método. */
@ExtendWith(MockitoExtension.class)
class CuentaPorPagarServiceImplTest {

    @Mock FinanzasCuentaPorPagarRepository cuentaRepository;
    @Mock CompraRepository compraRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock PagoCuentaPorPagarRepository pagoRepository;
    @InjectMocks CuentaPorPagarServiceImpl service;

    private static final String EMAIL = "admin@policalum.com";
    User admin;
    FinanzasCuentaPorPagar cxp;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL).build();
        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenReturn(Optional.of(admin));
        cxp = FinanzasCuentaPorPagar.builder().id(UUID.randomUUID()).montoTotal(new BigDecimal("1000.00"))
                .montoPagado(new BigDecimal("400.00")).estado(EstadoCxP.PAGADO_PARCIAL).tipoReferencia(TipoReferenciaCxP.COMPRA).build();
        lenient().when(cuentaRepository.findByIdParaActualizar(cxp.getId())).thenReturn(Optional.of(cxp));
        lenient().when(cuentaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registrarPago_parcial_guardaAbonoConMetodoYFecha() {
        ArgumentCaptor<PagoCuentaPorPagar> captor = ArgumentCaptor.forClass(PagoCuentaPorPagar.class);

        service.registrarPago(cxp.getId(), new PagoCxPRequestDTO(new BigDecimal("250.00"), MetodoPago.YAPE, "OP-123", null));

        verify(pagoRepository).save(captor.capture());
        PagoCuentaPorPagar abono = captor.getValue();
        assertEquals(new BigDecimal("250.00"), abono.getMonto());
        assertEquals(MetodoPago.YAPE, abono.getMetodoPago());
        assertEquals("OP-123", abono.getReferencia());
        assertNotNull(abono.getFechaPago());
        assertSame(admin, abono.getRegistradoPor());
        assertSame(cxp, abono.getCuentaPorPagar());
        assertEquals(new BigDecimal("650.00"), cxp.getMontoPagado());
        assertEquals(EstadoCxP.PAGADO_PARCIAL, cxp.getEstado());
    }

    @Test
    void registrarPago_saldoCompleto_marcaPagadoYUsaBloqueo() {
        service.registrarPago(cxp.getId(), new PagoCxPRequestDTO(new BigDecimal("600.00"), MetodoPago.TRANSFERENCIA, null, null));

        assertEquals(EstadoCxP.PAGADO, cxp.getEstado());
        assertNotNull(cxp.getFechaPago());
        verify(cuentaRepository).findByIdParaActualizar(cxp.getId());
        verify(cuentaRepository, never()).findById(any());
        verify(pagoRepository).save(any());
    }

    @Test
    void registrarPago_excedeSaldo_noGuardaAbono() {
        assertThrows(ValidationException.class, () -> service.registrarPago(cxp.getId(),
                new PagoCxPRequestDTO(new BigDecimal("600.01"), MetodoPago.EFECTIVO, null, null)));
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void registrarPago_yaPagada_lanzaIllegalState() {
        cxp.setEstado(EstadoCxP.PAGADO);
        assertThrows(IllegalStateException.class, () -> service.registrarPago(cxp.getId(),
                new PagoCxPRequestDTO(BigDecimal.ONE, MetodoPago.EFECTIVO, null, null)));
        verifyNoInteractions(pagoRepository);
    }

    // ===================== CONSULTAS Y DASHBOARD (auditoría de cobertura) =====================

    private static final LocalDate HOY = LocalDate.now();

    private FinanzasCuentaPorPagar cuenta(EstadoCxP estado, String total, String pagado, LocalDate vence) {
        return FinanzasCuentaPorPagar.builder().id(UUID.randomUUID()).estado(estado).tipoReferencia(TipoReferenciaCxP.COMPRA)
                .montoTotal(new BigDecimal(total)).montoPagado(pagado == null ? null : new BigDecimal(pagado))
                .fechaVencimiento(vence).build();
    }

    @Test
    void obtener_calculaSaldoYMarcaVencidaSiPasoLaFechaYNoEstaPagada() {
        FinanzasCuentaPorPagar c = cuenta(EstadoCxP.PAGADO_PARCIAL, "1000.00", "300.00", HOY.minusDays(1));
        c.setProveedor(Proveedor.builder().id(UUID.randomUUID()).nombreRazon("Aluminios SAC").numeroDocumento("20123456789").build());
        c.setPagadoPor(admin);
        when(cuentaRepository.findById(c.getId())).thenReturn(Optional.of(c));

        CuentaPorPagarResponseDTO dto = service.obtener(c.getId());

        assertEquals(new BigDecimal("700.00"), dto.saldoPendiente());
        assertTrue(dto.vencida());
        assertEquals("Aluminios SAC", dto.proveedorNombre());
        assertEquals("Miguel Dueño", dto.pagadoPorNombre());
        assertNull(dto.sedeId());
        assertNull(dto.compraId());
    }

    @Test
    void obtener_venceHoy_noEstaVencida() {
        FinanzasCuentaPorPagar c = cuenta(EstadoCxP.PENDIENTE, "100.00", null, HOY);
        when(cuentaRepository.findById(c.getId())).thenReturn(Optional.of(c));

        CuentaPorPagarResponseDTO dto = service.obtener(c.getId());

        assertFalse(dto.vencida());
        assertEquals(BigDecimal.ZERO, dto.montoPagado(), "montoPagado nulo se expone como cero");
        assertEquals(new BigDecimal("100.00"), dto.saldoPendiente());
    }

    @Test
    void obtener_pagadaConFechaPasada_noEstaVencida() {
        FinanzasCuentaPorPagar c = cuenta(EstadoCxP.PAGADO, "100.00", "100.00", HOY.minusDays(30));
        when(cuentaRepository.findById(c.getId())).thenReturn(Optional.of(c));

        assertFalse(service.obtener(c.getId()).vencida());
    }

    @Test
    void obtener_inexistente_lanzaNotFound() {
        UUID id = UUID.randomUUID();
        when(cuentaRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtener(id));
    }

    @Test
    void obtenerPorCompra_existente_exponeNumeroDeOrden() {
        Compra compra = new Compra();
        compra.setId(UUID.randomUUID());
        compra.setNumeroOrdenCompra("OC-2026-0003");
        FinanzasCuentaPorPagar c = cuenta(EstadoCxP.PENDIENTE, "50.00", "0", null);
        c.setCompra(compra);
        when(cuentaRepository.findByCompra_Id(compra.getId())).thenReturn(Optional.of(c));

        CuentaPorPagarResponseDTO dto = service.obtenerPorCompra(compra.getId());

        assertEquals("OC-2026-0003", dto.compraNumeroOC());
        assertFalse(dto.vencida(), "sin fecha de vencimiento nunca está vencida");
    }

    @Test
    void obtenerPorCompra_sinCxp_lanzaNotFound() {
        UUID compraId = UUID.randomUUID();
        when(cuentaRepository.findByCompra_Id(compraId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorCompra(compraId));
    }

    @Test
    void buscar_pasaFiltrosAlRepositorio() {
        UUID sedeId = UUID.randomUUID(), proveedorId = UUID.randomUUID();
        Pageable p = PageRequest.of(0, 20);
        when(cuentaRepository.search(sedeId, proveedorId, EstadoCxP.PENDIENTE, true, p))
                .thenReturn(new PageImpl<>(List.of(cuenta(EstadoCxP.PENDIENTE, "10.00", "0", null))));

        assertEquals(1, service.buscar(sedeId, proveedorId, EstadoCxP.PENDIENTE, true, p).getTotalElements());
    }

    @Test
    void vencidas_consultaConSoloVencidasSinPaginar() {
        UUID sedeId = UUID.randomUUID();
        when(cuentaRepository.search(eq(sedeId), isNull(), isNull(), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(cuenta(EstadoCxP.PENDIENTE, "10.00", "0", HOY.minusDays(2)))));

        List<CuentaPorPagarResponseDTO> r = service.vencidas(sedeId);

        assertEquals(1, r.size());
        assertTrue(r.get(0).vencida());
        ArgumentCaptor<Pageable> pc = ArgumentCaptor.forClass(Pageable.class);
        verify(cuentaRepository).search(eq(sedeId), isNull(), isNull(), eq(true), pc.capture());
        assertTrue(pc.getValue().isUnpaged());
    }

    @Test
    void proximasAVencer_incluyeHastaElLimiteInclusiveYExcluyeSinFechaOPosteriores() {
        FinanzasCuentaPorPagar enLimite = cuenta(EstadoCxP.PENDIENTE, "10.00", "0", HOY.plusDays(7));
        FinanzasCuentaPorPagar manana = cuenta(EstadoCxP.PENDIENTE, "20.00", "0", HOY.plusDays(1));
        FinanzasCuentaPorPagar despues = cuenta(EstadoCxP.PENDIENTE, "30.00", "0", HOY.plusDays(8));
        FinanzasCuentaPorPagar sinFecha = cuenta(EstadoCxP.PENDIENTE, "40.00", "0", null);
        when(cuentaRepository.search(isNull(), isNull(), eq(EstadoCxP.PENDIENTE), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(enLimite, manana, despues, sinFecha)));

        List<CuentaPorPagarResponseDTO> r = service.proximasAVencer(null, 7);

        assertEquals(List.of(enLimite.getId(), manana.getId()), r.stream().map(CuentaPorPagarResponseDTO::id).toList());
    }

    @Test
    void proximasAVencer_diasCero_soloLasQueVencenHoyOAntes() {
        FinanzasCuentaPorPagar hoy = cuenta(EstadoCxP.PENDIENTE, "10.00", "0", HOY);
        FinanzasCuentaPorPagar manana = cuenta(EstadoCxP.PENDIENTE, "10.00", "0", HOY.plusDays(1));
        when(cuentaRepository.search(any(), any(), any(), any(), any())).thenReturn(new PageImpl<>(List.of(hoy, manana)));

        assertEquals(1, service.proximasAVencer(null, 0).size());
    }

    @Test
    void dashboard_cuentaEstadosYMontosExcluyendoPagadasDelSaldo() {
        when(cuentaRepository.search(any(), isNull(), isNull(), isNull(), any())).thenReturn(new PageImpl<>(List.of(
                cuenta(EstadoCxP.PENDIENTE, "1000.00", null, HOY.minusDays(3)),          // vencida: 1000
                cuenta(EstadoCxP.PAGADO_PARCIAL, "500.00", "200.00", HOY.minusDays(1)),  // vencida: 300
                cuenta(EstadoCxP.PAGADO_PARCIAL, "400.00", "100.00", HOY.plusDays(5)),
                cuenta(EstadoCxP.PAGADO, "250.00", "250.00", HOY.minusDays(10)))));     // pagada: no suma saldo ni vencido

        ResumenCxPDashboardDTO d = service.dashboard(null);

        assertEquals(4, d.totalCuentas());
        assertEquals(1, d.pendientes());
        assertEquals(2, d.pagadasParcial());
        assertEquals(1, d.pagadas());
        assertEquals(2, d.vencidas());
        assertEquals(new BigDecimal("1600.00"), d.montoTotalPorPagar());
        assertEquals(new BigDecimal("550.00"), d.montoTotalPagado());
        assertEquals(new BigDecimal("1300.00"), d.montoTotalVencido());
    }

    @Test
    void dashboard_sinCuentas_todoEnCero() {
        when(cuentaRepository.search(any(), any(), any(), any(), any())).thenReturn(Page.empty());

        ResumenCxPDashboardDTO d = service.dashboard(UUID.randomUUID());

        assertEquals(0, d.totalCuentas());
        assertEquals(0, d.montoTotalPorPagar().signum());
        assertEquals(0, d.montoTotalVencido().signum());
    }
}
