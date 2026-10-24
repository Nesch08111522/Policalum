package com.policalum.erp.application.service.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.policalum.erp.application.dto.request.EmpresaRequestDTO;
import com.policalum.erp.application.dto.request.MarcaRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosFinanzasRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosInventarioRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosVentasRequestDTO;
import com.policalum.erp.application.dto.request.SerieComprobanteRequestDTO;
import com.policalum.erp.application.dto.request.SeriesRequestDTO;
import com.policalum.erp.application.dto.response.ConfiguracionPublicaDTO;
import com.policalum.erp.application.dto.response.EmpresaResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosFinanzasDTO;
import com.policalum.erp.application.dto.response.ParametrosInventarioDTO;
import com.policalum.erp.application.dto.response.ParametrosReportesDTO;
import com.policalum.erp.application.dto.response.ParametrosResponseDTO;
import com.policalum.erp.application.dto.response.ParametrosVentasDTO;
import com.policalum.erp.application.dto.response.SerieComprobanteResponseDTO;
import com.policalum.erp.application.mapper.ConfiguracionMapper;
import com.policalum.erp.application.mapper.SerieComprobanteMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.model.ConfiguracionEmpresa;
import com.policalum.erp.domain.model.ConfiguracionFinanzas;
import com.policalum.erp.domain.model.ConfiguracionInventario;
import com.policalum.erp.domain.model.ConfiguracionReportes;
import com.policalum.erp.domain.model.ConfiguracionVentas;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.SerieComprobante;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.ConfiguracionEmpresaRepository;
import com.policalum.erp.infrastructure.repository.ConfiguracionFinanzasRepository;
import com.policalum.erp.infrastructure.repository.ConfiguracionInventarioRepository;
import com.policalum.erp.infrastructure.repository.ConfiguracionReportesRepository;
import com.policalum.erp.infrastructure.repository.ConfiguracionVentasRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.SerieComprobanteRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ConfiguracionServiceImplTest {

    private static final String EMAIL = "admin@policalum.com";
    /** RUC real con dígito verificador válido (2010007097 → 0). */
    private static final String RUC_VALIDO = "20100070970";

    @Mock ConfiguracionEmpresaRepository empresaRepository;
    @Mock ConfiguracionVentasRepository ventasRepository;
    @Mock ConfiguracionInventarioRepository inventarioRepository;
    @Mock ConfiguracionFinanzasRepository finanzasRepository;
    @Mock ConfiguracionReportesRepository reportesRepository;
    @Mock SerieComprobanteRepository serieRepository;
    @Mock SedeRepository sedeRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock FileStorageService fileStorageService;
    @Spy ConfiguracionMapper mapper = Mappers.getMapper(ConfiguracionMapper.class);
    @Spy SerieComprobanteMapper serieMapper = Mappers.getMapper(SerieComprobanteMapper.class);
    @InjectMocks ConfiguracionServiceImpl service;

    User admin;
    Sede centro;
    Sede norte;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(UUID.randomUUID()).nombre("Miguel").apellido("Dueño").email(EMAIL).activo(true)
                .rol(Rol.builder().nombre(RolNombre.ROLE_ADMIN).build()).build();
        centro = Sede.builder().id(UUID.randomUUID()).nombre("Centro").activa(true).build();
        norte = Sede.builder().id(UUID.randomUUID()).nombre("Norte").activa(true).build();

        SecurityContext context = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(EMAIL);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByEmailAndActivoTrue(EMAIL)).thenReturn(Optional.of(admin));
        lenient().when(sedeRepository.findById(centro.getId())).thenReturn(Optional.of(centro));
        lenient().when(sedeRepository.findById(norte.getId())).thenReturn(Optional.of(norte));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    // ================================================================ valores de fábrica

    @Test
    void lectores_sinFila_devuelvenValoresIdenticosALasConstantesAnteriores() {
        assertEquals(new ParametrosVentasDTO(15, 120, 5, 4320), service.ventas());
        assertEquals(new ParametrosInventarioDTO(new BigDecimal("500.00"), 30), service.inventario());
        assertEquals(new ParametrosFinanzasDTO(3, 3, 12), service.finanzas());
        assertEquals(new ParametrosReportesDTO(50_000), service.reportes());
    }

    @Test
    void lectores_conFila_devuelvenLoGuardado() {
        when(ventasRepository.findById(1L)).thenReturn(Optional.of(ConfiguracionVentas.builder().id(1L)
                .vigenciaCotizacionDias(30).preformaMinutosDefault(60).preformaMinutosMin(10).preformaMinutosMax(600).build()));
        when(reportesRepository.findById(1L)).thenReturn(Optional.of(ConfiguracionReportes.builder().id(1L).maxFilasExportacion(20_000).build()));

        assertEquals(new ParametrosVentasDTO(30, 60, 10, 600), service.ventas());
        assertEquals(20_000, service.reportes().maxFilasExportacion());
    }

    @Test
    void inicializarSiFalta_creaSoloLasFilasFaltantesYAuditaComoSistema() {
        when(empresaRepository.existsById(1L)).thenReturn(true);
        when(ventasRepository.existsById(1L)).thenReturn(false);
        when(inventarioRepository.existsById(1L)).thenReturn(false);
        when(finanzasRepository.existsById(1L)).thenReturn(true);
        when(reportesRepository.existsById(1L)).thenReturn(false);

        service.inicializarSiFalta();

        verify(empresaRepository, never()).save(any());
        verify(finanzasRepository, never()).save(any());
        ArgumentCaptor<ConfiguracionVentas> ventas = ArgumentCaptor.forClass(ConfiguracionVentas.class);
        verify(ventasRepository).save(ventas.capture());
        assertEquals(1L, ventas.getValue().getId());
        verify(inventarioRepository).save(any());
        verify(reportesRepository).save(any());
        verify(auditService).registrar(eq("configuracion_ventas"), eq(ConfiguracionServiceImpl.registroId("configuracion_ventas")),
                eq(AccionAudit.INSERT), isNull(), eq("SISTEMA"), anyMap());
        verify(auditService, times(3)).registrar(anyString(), any(), eq(AccionAudit.INSERT), isNull(), eq("SISTEMA"), anyMap());
    }

    // ================================================================ empresa

    @Test
    void obtenerEmpresa_sinConfigurar_devuelveNombrePorDefectoYConfiguradaFalse() {
        EmpresaResponseDTO r = service.obtenerEmpresa();

        assertFalse(r.configurada());
        assertEquals(ConfiguracionEmpresa.NOMBRE_COMERCIAL_DEFAULT, r.nombreComercial());
        assertNull(r.ruc());
    }

    @Test
    void actualizarEmpresa_normalizaGuardaYAuditaAntesYDespues() {
        ConfiguracionEmpresa actual = ConfiguracionEmpresa.builder().id(1L).nombreComercial("POLICALUM").build();
        when(empresaRepository.findParaActualizar(1L)).thenReturn(Optional.of(actual));
        when(empresaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EmpresaResponseDTO r = service.actualizarEmpresa(new EmpresaRequestDTO(" " + RUC_VALIDO + " ",
                "  Policalum S.A.C. ", "  ", "Av. España 123, Trujillo", "130101", "   ",
                "  Ventas@Policalum.COM ", "https://cdn.policalum.pe/logo.png"));

        assertTrue(r.configurada());
        assertEquals(RUC_VALIDO, r.ruc());
        assertEquals("Policalum S.A.C.", r.razonSocial());
        assertEquals("Policalum S.A.C.", r.nombreComercial(), "nombre comercial vacío = razón social");
        assertNull(r.telefono());
        assertEquals("ventas@policalum.com", r.email());
        assertEquals("Miguel Dueño", r.actualizadoPor());
        assertSame(admin, actual.getActualizadoPor());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> datos = ArgumentCaptor.forClass(Map.class);
        verify(auditService).registrar(eq("configuracion_empresa"), eq(ConfiguracionServiceImpl.registroId("configuracion_empresa")),
                eq(AccionAudit.UPDATE), datos.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> antes = (Map<String, Object>) datos.getValue().get("antes");
        @SuppressWarnings("unchecked")
        Map<String, Object> despues = (Map<String, Object>) datos.getValue().get("despues");
        assertNull(antes.get("ruc"));
        assertEquals(RUC_VALIDO, despues.get("ruc"));
    }

    @Test
    void actualizarEmpresa_sinCambios_noGuardaNiAudita() {
        ConfiguracionEmpresa actual = ConfiguracionEmpresa.builder().id(1L).ruc(RUC_VALIDO).razonSocial("Policalum S.A.C.")
                .nombreComercial("POLICALUM").direccionFiscal("Av. España 123").ubigeo("130101").build();
        when(empresaRepository.findParaActualizar(1L)).thenReturn(Optional.of(actual));

        service.actualizarEmpresa(new EmpresaRequestDTO(RUC_VALIDO, "Policalum S.A.C.", "POLICALUM",
                "Av. España 123", "130101", null, null, null));

        verify(empresaRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void actualizarEmpresa_rucConDigitoVerificadorIncorrecto_lanza400SinTocarBd() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.actualizarEmpresa(
                new EmpresaRequestDTO("20100070971", "X S.A.C.", null, "Calle 1", "130101", null, null, null)));

        assertTrue(ex.getMessage().contains("dígito verificador"));
        verify(empresaRepository, never()).findParaActualizar(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void validarRuc_prefijosYDigitoVerificador() {
        assertDoesNotThrow(() -> ConfiguracionServiceImpl.validarRuc(RUC_VALIDO));
        assertDoesNotThrow(() -> ConfiguracionServiceImpl.validarRuc("10467793549"));   // persona natural
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionServiceImpl.validarRuc("30100070970"));   // prefijo
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionServiceImpl.validarRuc("2010007097"));    // 10 dígitos
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionServiceImpl.validarRuc("2010007097A"));
        assertThrows(IllegalArgumentException.class, () -> ConfiguracionServiceImpl.validarRuc(null));
    }

    @Test
    void actualizarEmpresa_usuarioMultifuncional_lanza403() {
        admin.setRol(Rol.builder().nombre(RolNombre.ROLE_MULTIFUNCIONAL).build());

        assertThrows(AccessDeniedException.class, () -> service.actualizarEmpresa(
                new EmpresaRequestDTO(RUC_VALIDO, "X", null, "Calle 1", "130101", null, null, null)));
        verifyNoInteractions(empresaRepository, auditService);
    }

    @Test
    void publica_soloExponeNombreComercialYLogo() {
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(ConfiguracionEmpresa.builder().id(1L).ruc(RUC_VALIDO)
                .razonSocial("Policalum S.A.C.").nombreComercial("Policalum").logoUrl("https://x/logo.png").build()));

        ConfiguracionPublicaDTO r = service.publica();

        assertEquals(new ConfiguracionPublicaDTO("Policalum", "https://x/logo.png"), r);
        assertEquals(2, ConfiguracionPublicaDTO.class.getRecordComponents().length, "no agregar datos internos al DTO público");
    }

    @Test
    void actualizarMarca_cambiaNombreYLogoSinTocarElRuc() {
        ConfiguracionEmpresa actual = ConfiguracionEmpresa.builder().id(1L).nombreComercial("POLICALUM").ruc(null).build();
        when(empresaRepository.findParaActualizar(1L)).thenReturn(Optional.of(actual));
        when(empresaRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ConfiguracionPublicaDTO marca = service.actualizarMarca(new MarcaRequestDTO("  Poli Calum  ", "empresa/2026/10/logo.png"));

        assertEquals("Poli Calum", marca.nombreComercial());
        assertEquals("empresa/2026/10/logo.png", marca.logoUrl());
        assertNull(actual.getRuc());
        verify(auditService).registrar(eq("configuracion_empresa"), eq(ConfiguracionServiceImpl.registroId("configuracion_empresa")),
                eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void actualizarMarca_reemplazaElArchivoAnterior() {
        ConfiguracionEmpresa actual = ConfiguracionEmpresa.builder().id(1L).nombreComercial("POLICALUM")
                .logoUrl("empresa/logo.png").build();
        when(empresaRepository.findParaActualizar(1L)).thenReturn(Optional.of(actual));
        when(empresaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.actualizarMarca(new MarcaRequestDTO("POLICALUM", "empresa/logo.webp"));

        assertEquals("empresa/logo.webp", actual.getLogoUrl());
        verify(fileStorageService).eliminar("empresa/logo.png");
        verify(fileStorageService).descartarVariantes("empresa/logo.webp");
    }

    @Test
    void actualizarEmpresa_conservaElLogoAunqueElFormularioTraigaUnaUrl() {
        ConfiguracionEmpresa actual = ConfiguracionEmpresa.builder().id(1L).nombreComercial("POLICALUM")
                .logoUrl("empresa/logo.png").ruc(null).build();
        when(empresaRepository.findParaActualizar(1L)).thenReturn(Optional.of(actual));
        when(empresaRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        service.actualizarEmpresa(new EmpresaRequestDTO(RUC_VALIDO, "Policalum S.A.C.", "POLICALUM",
                "Av. España 123", "130101", null, null, "https://cdn.ejemplo/logo.png"));

        assertEquals("empresa/logo.png", actual.getLogoUrl());
        verify(fileStorageService, never()).eliminar(any());
    }

    @Test
    void actualizarMarca_sinCambios_noGuarda() {
        ConfiguracionEmpresa actual = ConfiguracionEmpresa.builder().id(1L).nombreComercial("POLICALUM").build();
        when(empresaRepository.findParaActualizar(1L)).thenReturn(Optional.of(actual));

        service.actualizarMarca(new MarcaRequestDTO("POLICALUM", null));

        verify(empresaRepository, never()).save(any());
        verify(auditService, never()).registrar(anyString(), any(), any(), anyMap());
    }

    // ================================================================ parámetros

    @Test
    void actualizarParametros_sinNingunGrupo_lanza400() {
        assertThrows(IllegalArgumentException.class,
                () -> service.actualizarParametros(new ParametrosRequestDTO(null, null, null, null)));
        verifyNoInteractions(auditService);
    }

    @Test
    void actualizarParametros_preformaDefaultFueraDeRango_lanza400SinBloquearNada() {
        ParametrosRequestDTO r = new ParametrosRequestDTO(new ParametrosVentasRequestDTO(15, 4, 5, 4320),
                new ParametrosInventarioRequestDTO(new BigDecimal("800"), 40), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.actualizarParametros(r));
        verify(ventasRepository, never()).findParaActualizar(any());
        verify(inventarioRepository, never()).findParaActualizar(any());   // todo o nada
        verifyNoInteractions(auditService);
    }

    @Test
    void actualizarParametros_proyeccionDefaultMayorQueMaximo_lanza400() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarParametros(
                new ParametrosRequestDTO(null, null, new ParametrosFinanzasRequestDTO(3, 13, 12), null)));
        verify(finanzasRepository, never()).findParaActualizar(any());
    }

    @Test
    void actualizarParametros_soloInventario_actualizaEseGrupoYAudita() {
        ConfiguracionInventario inv = ConfiguracionInventario.builder().id(1L)
                .umbralJustificacionAjuste(new BigDecimal("500.00")).minCaracteresJustificacion(30).build();
        when(inventarioRepository.findParaActualizar(1L)).thenReturn(Optional.of(inv));
        when(inventarioRepository.findById(1L)).thenReturn(Optional.of(inv));
        when(inventarioRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ParametrosResponseDTO r = service.actualizarParametros(new ParametrosRequestDTO(null,
                new ParametrosInventarioRequestDTO(new BigDecimal("1000.5"), 50), null, null));

        assertEquals(0, new BigDecimal("1000.50").compareTo(r.inventario().umbralJustificacionAjuste()));
        assertEquals(2, inv.getUmbralJustificacionAjuste().scale());
        assertEquals(50, r.inventario().minCaracteresJustificacion());
        assertEquals(ParametrosVentasDTO.DEFAULTS, r.ventas());
        assertSame(admin, inv.getActualizadoPor());
        verify(ventasRepository, never()).findParaActualizar(any());
        verify(finanzasRepository, never()).findParaActualizar(any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> datos = ArgumentCaptor.forClass(Map.class);
        verify(auditService).registrar(eq("configuracion_inventario"), any(), eq(AccionAudit.UPDATE), datos.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> despues = (Map<String, Object>) datos.getValue().get("despues");
        assertEquals("1000.50", despues.get("umbralJustificacionAjuste"));
        assertEquals(50, despues.get("minCaracteresJustificacion"));
    }

    @Test
    void actualizarParametros_mismoValorConOtraEscala_noAudita() {
        ConfiguracionInventario inv = ConfiguracionInventario.builder().id(1L)
                .umbralJustificacionAjuste(new BigDecimal("500.00")).minCaracteresJustificacion(30).build();
        when(inventarioRepository.findParaActualizar(1L)).thenReturn(Optional.of(inv));

        service.actualizarParametros(new ParametrosRequestDTO(null,
                new ParametrosInventarioRequestDTO(new BigDecimal("500"), 30), null, null));

        verify(inventarioRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void actualizarParametros_filaInexistente_laCreaYAuditaInsert() {
        when(ventasRepository.findParaActualizar(1L)).thenReturn(Optional.empty());
        when(ventasRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.actualizarParametros(new ParametrosRequestDTO(new ParametrosVentasRequestDTO(20, 60, 10, 600), null, null, null));

        ArgumentCaptor<ConfiguracionVentas> c = ArgumentCaptor.forClass(ConfiguracionVentas.class);
        verify(ventasRepository).save(c.capture());
        assertEquals(1L, c.getValue().getId());
        assertEquals(20, c.getValue().getVigenciaCotizacionDias());
        verify(auditService).registrar(eq("configuracion_ventas"), any(), eq(AccionAudit.INSERT), anyMap());
    }

    // ================================================================ series

    @Test
    void actualizarSeries_nuevaSerie_seGuardaEnMayusculasYSeAudita() {
        when(serieRepository.findParaActualizar(centro.getId(), TipoComprobante.BOLETA)).thenReturn(Optional.empty());
        when(serieRepository.findBySerie("B001")).thenReturn(Optional.empty());
        when(serieRepository.save(any())).thenAnswer(i -> {
            SerieComprobante s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        when(serieRepository.listar(null)).thenReturn(List.of());

        service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, " b001 ".trim(), null))));

        ArgumentCaptor<SerieComprobante> c = ArgumentCaptor.forClass(SerieComprobante.class);
        verify(serieRepository).save(c.capture());
        assertEquals("B001", c.getValue().getSerie());
        assertTrue(c.getValue().getActiva());
        assertSame(centro, c.getValue().getSede());
        assertSame(admin, c.getValue().getActualizadoPor());
        verify(auditService).registrar(eq("series_comprobante"), eq(c.getValue().getId()), eq(AccionAudit.INSERT), anyMap());
    }

    @Test
    void actualizarSeries_formatoNoCorrespondeAlTipo_lanza400() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, "F001", null)))));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.TICKET, "B001", null)))));
        assertDoesNotThrow(() -> ConfiguracionServiceImpl.validarFormatoSerie(TipoComprobante.TICKET, "T001"));
        assertDoesNotThrow(() -> ConfiguracionServiceImpl.validarFormatoSerie(TipoComprobante.FACTURA, "FC01"));
        verify(serieRepository, never()).save(any());
    }

    @Test
    void actualizarSeries_repetidasEnLaSolicitud_lanza400() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, "B001", null),
                new SerieComprobanteRequestDTO(norte.getId(), TipoComprobante.BOLETA, "b001", null)))));
        assertThrows(IllegalArgumentException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, "B001", null),
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, "B002", null)))));
        verify(serieRepository, never()).save(any());
    }

    @Test
    void actualizarSeries_serieAsignadaAOtraSede_lanza409() {
        SerieComprobante deNorte = SerieComprobante.builder().id(UUID.randomUUID()).sede(norte)
                .tipoComprobante(TipoComprobante.BOLETA).serie("B002").activa(true).build();
        when(serieRepository.findParaActualizar(centro.getId(), TipoComprobante.BOLETA)).thenReturn(Optional.empty());
        when(serieRepository.findBySerie("B002")).thenReturn(Optional.of(deNorte));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, "B002", null)))));

        assertTrue(ex.getMessage().contains("Norte"));
        verify(serieRepository, never()).save(any());
    }

    @Test
    void actualizarSeries_existente_cambiaSerieYAuditaUpdate() {
        SerieComprobante actual = SerieComprobante.builder().id(UUID.randomUUID()).sede(centro)
                .tipoComprobante(TipoComprobante.FACTURA).serie("F001").activa(true).build();
        when(serieRepository.findParaActualizar(centro.getId(), TipoComprobante.FACTURA)).thenReturn(Optional.of(actual));
        when(serieRepository.findBySerie("F002")).thenReturn(Optional.empty());
        when(serieRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(serieRepository.listar(null)).thenReturn(List.of(actual));

        List<SerieComprobanteResponseDTO> r = service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.FACTURA, "F002", null))));

        assertEquals("F002", actual.getSerie());
        assertTrue(actual.getActiva(), "activa null no cambia el estado");
        assertEquals(1, r.size());
        assertEquals("Centro", r.get(0).sedeNombre());
        verify(auditService).registrar(eq("series_comprobante"), eq(actual.getId()), eq(AccionAudit.UPDATE), anyMap());
    }

    @Test
    void actualizarSeries_sedeInactiva_soloPermiteDesactivar() {
        centro.setActiva(false);

        assertThrows(IllegalArgumentException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(centro.getId(), TipoComprobante.BOLETA, "B001", null)))));
        verify(serieRepository, never()).save(any());
    }

    @Test
    void actualizarSeries_sedeInexistente_lanza404() {
        UUID otra = UUID.randomUUID();
        when(sedeRepository.findById(otra)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizarSeries(new SeriesRequestDTO(List.of(
                new SerieComprobanteRequestDTO(otra, TipoComprobante.BOLETA, "B001", null)))));
    }

    @Test
    void registroId_esEstablePorTabla() {
        assertEquals(ConfiguracionServiceImpl.registroId("configuracion_ventas"),
                ConfiguracionServiceImpl.registroId("configuracion_ventas"));
        assertNotEquals(ConfiguracionServiceImpl.registroId("configuracion_ventas"),
                ConfiguracionServiceImpl.registroId("configuracion_finanzas"));
    }

    // ===================== Completado en auditoría de cobertura =====================
    @Test
    void obtenerParametros_sinFilas_agrupaLosCuatroGruposConValoresDeFabrica() {
        ParametrosResponseDTO p = service.obtenerParametros();

        assertEquals(ParametrosVentasDTO.DEFAULTS, p.ventas());
        assertEquals(ParametrosInventarioDTO.DEFAULTS, p.inventario());
        assertEquals(ParametrosFinanzasDTO.DEFAULTS, p.finanzas());
        assertEquals(ParametrosReportesDTO.DEFAULTS, p.reportes());
    }

    @Test
    void obtenerParametros_conFilaDeReportes_reflejaLoGuardado() {
        when(reportesRepository.findById(1L)).thenReturn(Optional.of(ConfiguracionReportes.builder().id(1L).maxFilasExportacion(2_000).build()));

        assertEquals(new ParametrosReportesDTO(2_000), service.obtenerParametros().reportes());
    }

    @Test
    void listarSeries_mapeaConSede() {
        SerieComprobante b = SerieComprobante.builder().id(UUID.randomUUID()).sede(centro)
                .tipoComprobante(TipoComprobante.BOLETA).serie("B001").activa(true).build();
        when(serieRepository.listar(centro.getId())).thenReturn(List.of(b));

        List<SerieComprobanteResponseDTO> r = service.listarSeries(centro.getId());

        assertEquals(1, r.size());
        assertEquals("B001", r.get(0).serie());
        assertEquals(centro.getId(), r.get(0).sedeId());
        assertEquals(TipoComprobante.BOLETA, r.get(0).tipoComprobante());
    }

    @Test
    void listarSeries_sinSede_listaTodas() {
        when(serieRepository.listar(null)).thenReturn(List.of());

        assertTrue(service.listarSeries(null).isEmpty());
    }
}
