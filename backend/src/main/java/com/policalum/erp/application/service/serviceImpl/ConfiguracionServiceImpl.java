package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.EmpresaRequestDTO;
import com.policalum.erp.application.dto.request.MarcaRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosFinanzasRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosInventarioRequestDTO;
import com.policalum.erp.application.dto.request.ParametrosReportesRequestDTO;
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
import com.policalum.erp.application.service.ConfiguracionService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.TipoComprobante;
import com.policalum.erp.domain.model.ConfiguracionEmpresa;
import com.policalum.erp.domain.model.ConfiguracionFinanzas;
import com.policalum.erp.domain.model.ConfiguracionInventario;
import com.policalum.erp.domain.model.ConfiguracionReportes;
import com.policalum.erp.domain.model.ConfiguracionVentas;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.SerieComprobante;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.FileStorageException;
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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ConfiguracionServiceImpl implements ConfiguracionService {

    static final String TABLA_EMPRESA = "configuracion_empresa";
    static final String TABLA_VENTAS = "configuracion_ventas";
    static final String TABLA_INVENTARIO = "configuracion_inventario";
    static final String TABLA_FINANZAS = "configuracion_finanzas";
    static final String TABLA_REPORTES = "configuracion_reportes";
    static final String TABLA_SERIES = "series_comprobante";

    private static final Long ID = ConfiguracionEmpresa.ID_UNICO;
    /** Prefijos válidos de RUC: persona natural (10), no domiciliado (15/16/17), persona jurídica (20). */
    private static final Set<String> PREFIJOS_RUC = Set.of("10", "15", "16", "17", "20");
    private static final int[] FACTORES_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private final ConfiguracionEmpresaRepository empresaRepository;
    private final ConfiguracionVentasRepository ventasRepository;
    private final ConfiguracionInventarioRepository inventarioRepository;
    private final ConfiguracionFinanzasRepository finanzasRepository;
    private final ConfiguracionReportesRepository reportesRepository;
    private final SerieComprobanteRepository serieRepository;
    private final SedeRepository sedeRepository;
    private final UserRepository userRepository;
    private final ConfiguracionMapper mapper;
    private final SerieComprobanteMapper serieMapper;
    private final AuditService auditService;
    private final FileStorageService fileStorageService;

    private static final long LOGO_MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> LOGO_EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");
    private static final java.util.regex.Pattern LOGO_VALIDO = java.util.regex.Pattern.compile("https?://\\S+|empresa/[A-Za-z0-9._/-]+");

    // ================================================================ empresa

    @Override
    @Transactional(readOnly = true)
    public EmpresaResponseDTO obtenerEmpresa() {
        return mapper.toResponseDto(empresaRepository.findById(ID).orElseGet(ConfiguracionServiceImpl::empresaPorDefecto));
    }

    @Override
    public EmpresaResponseDTO actualizarEmpresa(EmpresaRequestDTO r) {
        User admin = exigirAdmin();
        validarRuc(r.ruc());

        Optional<ConfiguracionEmpresa> actual = empresaRepository.findParaActualizar(ID);
        ConfiguracionEmpresa e = actual.orElseGet(ConfiguracionServiceImpl::empresaPorDefecto);
        Map<String, Object> antes = datosEmpresa(e);
        String logoActual = e.getLogoUrl();

        mapper.updateEntityFromDto(r, e);
        e.setId(ID);
        e.setRuc(r.ruc().trim());
        e.setRazonSocial(r.razonSocial().trim());
        e.setNombreComercial(vacioANull(r.nombreComercial()) != null ? r.nombreComercial().trim() : e.getRazonSocial());
        e.setDireccionFiscal(r.direccionFiscal().trim());
        e.setUbigeo(r.ubigeo().trim());
        e.setTelefono(vacioANull(r.telefono()));
        e.setEmail(vacioANull(r.email()) != null ? r.email().trim().toLowerCase(Locale.ROOT) : null);
        e.setLogoUrl(logoActual);

        Map<String, Object> despues = datosEmpresa(e);
        if (actual.isPresent() && antes.equals(despues)) {
            return mapper.toResponseDto(e);       // sin cambios: no se audita ni se toca updatedAt
        }
        e.setActualizadoPor(admin);
        ConfiguracionEmpresa guardada = empresaRepository.save(e);
        auditar(TABLA_EMPRESA, actual.isPresent() ? AccionAudit.UPDATE : AccionAudit.INSERT, antes, despues);
        log.info("Datos de empresa actualizados: RUC {}", guardada.getRuc());
        return mapper.toResponseDto(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracionPublicaDTO publica() {
        ConfiguracionEmpresa e = empresaRepository.findById(ID).orElseGet(ConfiguracionServiceImpl::empresaPorDefecto);
        return new ConfiguracionPublicaDTO(e.getNombreComercial(), e.getLogoUrl());
    }

    @Override
    public ConfiguracionPublicaDTO actualizarMarca(MarcaRequestDTO request) {
        User admin = exigirAdmin();
        String nombre = request.nombreComercial() == null ? "" : request.nombreComercial().trim();
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre comercial es obligatorio");
        }
        String logo = request.logoUrl() == null ? null : vacioANull(request.logoUrl());
        if (logo != null && !LOGO_VALIDO.matcher(logo).matches()) {
            throw new IllegalArgumentException("El logo debe ser una URL http(s) o un archivo de la empresa");
        }

        Optional<ConfiguracionEmpresa> actual = empresaRepository.findParaActualizar(ID);
        ConfiguracionEmpresa e = actual.orElseGet(ConfiguracionServiceImpl::empresaPorDefecto);
        Map<String, Object> antes = datosEmpresa(e);
        String logoAnterior = e.getLogoUrl();
        e.setId(ID);
        e.setNombreComercial(nombre);
        if (request.logoUrl() != null) {
            e.setLogoUrl(logo);
        }
        if (e.getNombreComercial() == null) {
            e.setNombreComercial(ConfiguracionEmpresa.NOMBRE_COMERCIAL_DEFAULT);
        }
        Map<String, Object> despues = datosEmpresa(e);
        if (actual.isPresent() && antes.equals(despues)) {
            return new ConfiguracionPublicaDTO(e.getNombreComercial(), e.getLogoUrl());
        }
        e.setActualizadoPor(admin);
        ConfiguracionEmpresa guardada = empresaRepository.save(e);
        auditar(TABLA_EMPRESA, actual.isPresent() ? AccionAudit.UPDATE : AccionAudit.INSERT, antes, despues);
        if (request.logoUrl() != null && logoAnterior != null && !logoAnterior.equals(logo)
                && logoAnterior.startsWith("empresa/") && !logoAnterior.contains("..")) {
            fileStorageService.eliminar(logoAnterior);
        }
        if (logo != null && logo.startsWith("empresa/")) {
            fileStorageService.descartarVariantes(logo);
        }
        return new ConfiguracionPublicaDTO(guardada.getNombreComercial(), guardada.getLogoUrl());
    }

    @Override
    public String guardarLogo(MultipartFile archivo) {
        exigirAdmin();
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona un archivo de imagen");
        }
        if (archivo.getSize() > LOGO_MAX_BYTES) {
            throw new IllegalArgumentException("La imagen no puede superar 2 MB");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        int punto = nombre.lastIndexOf('.');
        String extension = punto >= 0 ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        if (!LOGO_EXTENSIONES.contains(extension)) {
            throw new IllegalArgumentException("La imagen debe ser JPG, PNG o WEBP");
        }
        String tipo = archivo.getContentType() != null ? archivo.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!tipo.isBlank() && !tipo.startsWith("image/")) {
            throw new FileStorageException("El archivo no es una imagen");
        }
        return fileStorageService.guardarFijo(archivo, "empresa/logo");
    }

    @Override
    @Transactional(readOnly = true)
    public Resource cargarLogo() {
        ConfiguracionEmpresa e = empresaRepository.findById(ID).orElseGet(ConfiguracionServiceImpl::empresaPorDefecto);
        String ruta = e.getLogoUrl();
        if (ruta == null || ruta.isBlank() || ruta.contains("..") || !ruta.startsWith("empresa/")) {
            throw new ResourceNotFoundException("La empresa no tiene un logo guardado en el sistema");
        }
        return fileStorageService.cargar(ruta);
    }

    // ================================================================ parámetros

    @Override
    @Transactional(readOnly = true)
    public ParametrosResponseDTO obtenerParametros() {
        return new ParametrosResponseDTO(ventas(), inventario(), finanzas(), reportes());
    }

    @Override
    public ParametrosResponseDTO actualizarParametros(ParametrosRequestDTO r) {
        User admin = exigirAdmin();
        if (r == null || (r.ventas() == null && r.inventario() == null && r.finanzas() == null && r.reportes() == null)) {
            throw new IllegalArgumentException("Envíe al menos un grupo de parámetros (ventas, inventario, finanzas o reportes)");
        }
        // Validaciones cruzadas antes de bloquear nada: o se aplica todo o nada.
        if (r.ventas() != null) validarVentas(r.ventas());
        if (r.finanzas() != null) validarFinanzas(r.finanzas());

        if (r.ventas() != null) actualizarVentas(r.ventas(), admin);
        if (r.inventario() != null) actualizarInventario(r.inventario(), admin);
        if (r.finanzas() != null) actualizarFinanzas(r.finanzas(), admin);
        if (r.reportes() != null) actualizarReportes(r.reportes(), admin);
        return new ParametrosResponseDTO(ventas(), inventario(), finanzas(), reportes());
    }

    @Override
    @Transactional(readOnly = true)
    public ParametrosVentasDTO ventas() {
        return ventasRepository.findById(ID).map(mapper::toResponseDto)
                .orElseGet(() -> faltante(TABLA_VENTAS, ParametrosVentasDTO.DEFAULTS));
    }

    @Override
    @Transactional(readOnly = true)
    public ParametrosInventarioDTO inventario() {
        return inventarioRepository.findById(ID).map(mapper::toResponseDto)
                .orElseGet(() -> faltante(TABLA_INVENTARIO, ParametrosInventarioDTO.DEFAULTS));
    }

    @Override
    @Transactional(readOnly = true)
    public ParametrosFinanzasDTO finanzas() {
        return finanzasRepository.findById(ID).map(mapper::toResponseDto)
                .orElseGet(() -> faltante(TABLA_FINANZAS, ParametrosFinanzasDTO.DEFAULTS));
    }

    @Override
    @Transactional(readOnly = true)
    public ParametrosReportesDTO reportes() {
        return reportesRepository.findById(ID).map(mapper::toResponseDto)
                .orElseGet(() -> faltante(TABLA_REPORTES, ParametrosReportesDTO.DEFAULTS));
    }

    private void actualizarVentas(ParametrosVentasRequestDTO r, User admin) {
        Optional<ConfiguracionVentas> actual = ventasRepository.findParaActualizar(ID);
        ConfiguracionVentas c = actual.orElseGet(() -> ConfiguracionVentas.builder().id(ID).build());
        ParametrosVentasDTO antes = actual.map(mapper::toResponseDto).orElse(ParametrosVentasDTO.DEFAULTS);
        ParametrosVentasDTO despues = new ParametrosVentasDTO(r.vigenciaCotizacionDias(), r.preformaMinutosDefault(),
                r.preformaMinutosMin(), r.preformaMinutosMax());
        if (actual.isPresent() && antes.equals(despues)) return;
        mapper.updateEntityFromDto(r, c);
        c.setActualizadoPor(admin);
        ventasRepository.save(c);
        auditar(TABLA_VENTAS, actual.isPresent() ? AccionAudit.UPDATE : AccionAudit.INSERT, aMapa(antes), aMapa(despues));
        log.info("Parámetros de ventas actualizados: {}", despues);
    }

    private void actualizarInventario(ParametrosInventarioRequestDTO r, User admin) {
        Optional<ConfiguracionInventario> actual = inventarioRepository.findParaActualizar(ID);
        ConfiguracionInventario c = actual.orElseGet(() -> ConfiguracionInventario.builder().id(ID).build());
        ParametrosInventarioDTO antes = actual.map(mapper::toResponseDto).orElse(ParametrosInventarioDTO.DEFAULTS);
        BigDecimal umbral = r.umbralJustificacionAjuste().setScale(2, RoundingMode.HALF_UP);
        ParametrosInventarioDTO despues = new ParametrosInventarioDTO(umbral, r.minCaracteresJustificacion());
        if (actual.isPresent() && antes.minCaracteresJustificacion() == despues.minCaracteresJustificacion()
                && antes.umbralJustificacionAjuste().compareTo(umbral) == 0) return;
        mapper.updateEntityFromDto(r, c);
        c.setUmbralJustificacionAjuste(umbral);
        c.setActualizadoPor(admin);
        inventarioRepository.save(c);
        auditar(TABLA_INVENTARIO, actual.isPresent() ? AccionAudit.UPDATE : AccionAudit.INSERT, aMapa(antes), aMapa(despues));
        log.info("Parámetros de inventario actualizados: {}", despues);
    }

    private void actualizarFinanzas(ParametrosFinanzasRequestDTO r, User admin) {
        Optional<ConfiguracionFinanzas> actual = finanzasRepository.findParaActualizar(ID);
        ConfiguracionFinanzas c = actual.orElseGet(() -> ConfiguracionFinanzas.builder().id(ID).build());
        ParametrosFinanzasDTO antes = actual.map(mapper::toResponseDto).orElse(ParametrosFinanzasDTO.DEFAULTS);
        ParametrosFinanzasDTO despues = new ParametrosFinanzasDTO(r.mesesHistoricosProyeccion(),
                r.mesesProyeccionDefault(), r.mesesProyeccionMax());
        if (actual.isPresent() && antes.equals(despues)) return;
        mapper.updateEntityFromDto(r, c);
        c.setActualizadoPor(admin);
        finanzasRepository.save(c);
        auditar(TABLA_FINANZAS, actual.isPresent() ? AccionAudit.UPDATE : AccionAudit.INSERT, aMapa(antes), aMapa(despues));
        log.info("Parámetros de finanzas actualizados: {}", despues);
    }

    private void actualizarReportes(ParametrosReportesRequestDTO r, User admin) {
        Optional<ConfiguracionReportes> actual = reportesRepository.findParaActualizar(ID);
        ConfiguracionReportes c = actual.orElseGet(() -> ConfiguracionReportes.builder().id(ID).build());
        ParametrosReportesDTO antes = actual.map(mapper::toResponseDto).orElse(ParametrosReportesDTO.DEFAULTS);
        ParametrosReportesDTO despues = new ParametrosReportesDTO(r.maxFilasExportacion());
        if (actual.isPresent() && antes.equals(despues)) return;
        mapper.updateEntityFromDto(r, c);
        c.setActualizadoPor(admin);
        reportesRepository.save(c);
        auditar(TABLA_REPORTES, actual.isPresent() ? AccionAudit.UPDATE : AccionAudit.INSERT, aMapa(antes), aMapa(despues));
        log.info("Parámetros de reportes actualizados: {}", despues);
    }

    private static void validarVentas(ParametrosVentasRequestDTO v) {
        if (v.preformaMinutosMin() > v.preformaMinutosMax()) {
            throw new IllegalArgumentException("preformaMinutosMin no puede ser mayor que preformaMinutosMax");
        }
        if (v.preformaMinutosDefault() < v.preformaMinutosMin() || v.preformaMinutosDefault() > v.preformaMinutosMax()) {
            throw new IllegalArgumentException("preformaMinutosDefault debe estar entre " + v.preformaMinutosMin()
                    + " y " + v.preformaMinutosMax());
        }
    }

    private static void validarFinanzas(ParametrosFinanzasRequestDTO f) {
        if (f.mesesProyeccionDefault() > f.mesesProyeccionMax()) {
            throw new IllegalArgumentException("mesesProyeccionDefault no puede ser mayor que mesesProyeccionMax ("
                    + f.mesesProyeccionMax() + ")");
        }
    }

    // ================================================================ series

    @Override
    @Transactional(readOnly = true)
    public List<SerieComprobanteResponseDTO> listarSeries(UUID sedeId) {
        return serieRepository.listar(sedeId).stream().map(serieMapper::toResponseDto).toList();
    }

    /**
     * Upsert por (sede, tipo). Una serie que hoy pertenece a otra sede/tipo no se puede tomar: primero hay que
     * cambiarla allí (en esta misma solicitud, antes en la lista, o en otra). Cuando exista SUNAT (M2) aquí se
     * bloqueará cambiar la serie de un tipo que ya emitió comprobantes.
     */
    @Override
    public List<SerieComprobanteResponseDTO> actualizarSeries(SeriesRequestDTO r) {
        User admin = exigirAdmin();
        Set<String> clavesVistas = new HashSet<>();
        Set<String> seriesVistas = new HashSet<>();
        for (SerieComprobanteRequestDTO s : r.series()) {
            String serie = normalizarSerie(s.serie());
            validarFormatoSerie(s.tipoComprobante(), serie);
            if (!clavesVistas.add(s.sedeId() + "|" + s.tipoComprobante())) {
                throw new IllegalArgumentException("La sede " + s.sedeId() + " tiene " + s.tipoComprobante()
                        + " repetido en la solicitud");
            }
            if (!seriesVistas.add(serie)) {
                throw new IllegalArgumentException("La serie " + serie + " está repetida en la solicitud");
            }
        }

        Map<UUID, Sede> sedes = new HashMap<>();
        for (SerieComprobanteRequestDTO s : r.series()) {
            Sede sede = sedes.computeIfAbsent(s.sedeId(), id -> sedeRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada: " + id)));
            if (!Boolean.TRUE.equals(sede.getActiva()) && !Boolean.FALSE.equals(s.activa())) {
                throw new IllegalArgumentException("La sede " + sede.getNombre() + " está inactiva: solo se pueden desactivar sus series");
            }
            guardarSerie(s, sede, admin);
        }
        return listarSeries(null);
    }

    private void guardarSerie(SerieComprobanteRequestDTO s, Sede sede, User admin) {
        String serie = normalizarSerie(s.serie());
        Optional<SerieComprobante> actual = serieRepository.findParaActualizar(sede.getId(), s.tipoComprobante());
        serieRepository.findBySerie(serie)
                .filter(otra -> actual.isEmpty() || !otra.getId().equals(actual.get().getId()))
                .ifPresent(otra -> {
                    throw new IllegalStateException("La serie " + serie + " ya está asignada a "
                            + otra.getTipoComprobante() + " de la sede " + nombreSede(otra) + "; libérela primero");
                });

        if (actual.isPresent()) {
            SerieComprobante c = actual.get();
            boolean activa = s.activa() != null ? s.activa() : Boolean.TRUE.equals(c.getActiva());
            if (serie.equals(c.getSerie()) && activa == Boolean.TRUE.equals(c.getActiva())) return;
            Map<String, Object> antes = datosSerie(c);
            serieMapper.updateEntityFromDto(s, c);
            c.setSerie(serie);
            c.setActiva(activa);
            c.setActualizadoPor(admin);
            SerieComprobante guardada = serieRepository.save(c);
            auditService.registrar(TABLA_SERIES, guardada.getId(), AccionAudit.UPDATE,
                    cambio(antes, datosSerie(guardada)));
            log.info("Serie {} actualizada: {} {} -> {}", s.tipoComprobante(), sede.getNombre(), antes.get("serie"), serie);
        } else {
            SerieComprobante c = serieMapper.toEntity(s);
            c.setSede(sede);
            c.setSerie(serie);
            c.setActiva(s.activa() == null || s.activa());
            c.setActualizadoPor(admin);
            SerieComprobante guardada = serieRepository.save(c);
            auditService.registrar(TABLA_SERIES, guardada.getId(), AccionAudit.INSERT, cambio(null, datosSerie(guardada)));
            log.info("Serie {} creada: {} {}", s.tipoComprobante(), sede.getNombre(), serie);
        }
    }

    static String normalizarSerie(String serie) {
        return serie == null ? null : serie.trim().toUpperCase(Locale.ROOT);
    }

    /** BOLETA = B + 3, FACTURA = F + 3 (formato SUNAT electrónico), TICKET = 4 sin prefijo B/F. */
    static void validarFormatoSerie(TipoComprobante tipo, String serie) {
        if (serie == null || !serie.matches("[A-Z0-9]{4}")) {
            throw new IllegalArgumentException("La serie debe tener 4 letras o números: " + serie);
        }
        switch (tipo) {
            case BOLETA -> {
                if (serie.charAt(0) != 'B') throw new IllegalArgumentException("La serie de BOLETA debe empezar con B (ej. B001): " + serie);
            }
            case FACTURA -> {
                if (serie.charAt(0) != 'F') throw new IllegalArgumentException("La serie de FACTURA debe empezar con F (ej. F001): " + serie);
            }
            case TICKET -> {
                if (serie.charAt(0) == 'B' || serie.charAt(0) == 'F') {
                    throw new IllegalArgumentException("La serie de TICKET no puede empezar con B ni F (reservadas para boletas y facturas): " + serie);
                }
            }
        }
    }

    // ================================================================ inicialización

    @Override
    public void inicializarSiFalta() {
        if (!empresaRepository.existsById(ID)) {
            empresaRepository.save(empresaPorDefecto());
            registrarInicial(TABLA_EMPRESA);
        }
        if (!ventasRepository.existsById(ID)) {
            ventasRepository.save(ConfiguracionVentas.builder().id(ID).build());
            registrarInicial(TABLA_VENTAS);
        }
        if (!inventarioRepository.existsById(ID)) {
            inventarioRepository.save(ConfiguracionInventario.builder().id(ID).build());
            registrarInicial(TABLA_INVENTARIO);
        }
        if (!finanzasRepository.existsById(ID)) {
            finanzasRepository.save(ConfiguracionFinanzas.builder().id(ID).build());
            registrarInicial(TABLA_FINANZAS);
        }
        if (!reportesRepository.existsById(ID)) {
            reportesRepository.save(ConfiguracionReportes.builder().id(ID).build());
            registrarInicial(TABLA_REPORTES);
        }
        seriesDeFabrica();
    }

    /** B001, F001 y T001 por sede, solo si esa sede todavía no tiene la serie de ese comprobante. */
    private void seriesDeFabrica() {
        for (Sede sede : sedeRepository.findAll()) {
            asegurarSerie(sede, TipoComprobante.BOLETA, "B001");
            asegurarSerie(sede, TipoComprobante.FACTURA, "F001");
            asegurarSerie(sede, TipoComprobante.TICKET, "T001");
        }
    }

    private void asegurarSerie(Sede sede, TipoComprobante tipo, String codigo) {
        if (serieRepository.findParaActualizar(sede.getId(), tipo).isPresent()) return;
        if (serieRepository.findBySerie(codigo).isPresent()) {
            log.info("La serie {} ya existe y no se duplica para {}.", codigo, sede.getNombre());
            return;
        }
        SerieComprobante serie = SerieComprobante.builder()
                .sede(sede)
                .tipoComprobante(tipo)
                .serie(codigo)
                .activa(true)
                .build();
        serieRepository.save(serie);
        registrarInicial(TABLA_SERIES);
        log.info("Serie {} ({}) creada para {}.", codigo, tipo, sede.getNombre());
    }

    private void registrarInicial(String tabla) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("evento", "VALORES_DE_FABRICA");
        auditService.registrar(tabla, registroId(tabla), AccionAudit.INSERT, null, "SISTEMA", d);
        log.info("Configuración inicial creada con valores de fábrica: {}", tabla);
    }

    // ================================================================ utilidades

    /**
     * RUC: 11 dígitos, prefijo 10/15/16/17/20 y dígito verificador módulo 11 (factores 5432765432).
     *
     * @throws IllegalArgumentException si no cumple
     */
    static void validarRuc(String ruc) {
        String v = ruc == null ? "" : ruc.trim();
        if (!v.matches("\\d{11}")) throw new IllegalArgumentException("El RUC debe tener 11 dígitos");
        if (!PREFIJOS_RUC.contains(v.substring(0, 2))) {
            throw new IllegalArgumentException("El RUC debe empezar con 10, 15, 16, 17 o 20");
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) suma += (v.charAt(i) - '0') * FACTORES_RUC[i];
        int digito = 11 - (suma % 11);
        if (digito == 10) digito = 0;
        else if (digito == 11) digito = 1;
        if (digito != v.charAt(10) - '0') throw new IllegalArgumentException("RUC inválido: dígito verificador incorrecto");
    }

    /** Id estable para auditar tablas de una sola fila (registro_id es UUID). */
    static UUID registroId(String tabla) {
        return UUID.nameUUIDFromBytes((tabla + ":" + ID).getBytes(StandardCharsets.UTF_8));
    }

    private void auditar(String tabla, AccionAudit accion, Map<String, Object> antes, Map<String, Object> despues) {
        auditService.registrar(tabla, registroId(tabla), accion, cambio(antes, despues));
    }

    private static Map<String, Object> cambio(Map<String, Object> antes, Map<String, Object> despues) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("antes", antes);
        d.put("despues", despues);
        return d;
    }

    private static Map<String, Object> aMapa(Record r) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (var c : r.getClass().getRecordComponents()) {
            try {
                Object v = c.getAccessor().invoke(r);
                m.put(c.getName(), v instanceof BigDecimal b ? b.toPlainString() : v);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException("No se pudo leer " + c.getName(), ex);
            }
        }
        return m;
    }

    private static Map<String, Object> datosEmpresa(ConfiguracionEmpresa e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruc", e.getRuc());
        m.put("razonSocial", e.getRazonSocial());
        m.put("nombreComercial", e.getNombreComercial());
        m.put("direccionFiscal", e.getDireccionFiscal());
        m.put("ubigeo", e.getUbigeo());
        m.put("telefono", e.getTelefono());
        m.put("email", e.getEmail());
        m.put("logoUrl", e.getLogoUrl());
        return m;
    }

    private static Map<String, Object> datosSerie(SerieComprobante s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sedeId", s.getSede() != null ? String.valueOf(s.getSede().getId()) : null);
        m.put("tipoComprobante", s.getTipoComprobante() != null ? s.getTipoComprobante().name() : null);
        m.put("serie", s.getSerie());
        m.put("activa", s.getActiva());
        return m;
    }

    private static String nombreSede(SerieComprobante s) {
        return s.getSede() != null ? s.getSede().getNombre() : "?";
    }

    private static ConfiguracionEmpresa empresaPorDefecto() {
        return ConfiguracionEmpresa.builder().id(ID).nombreComercial(ConfiguracionEmpresa.NOMBRE_COMERCIAL_DEFAULT).build();
    }

    private static <T> T faltante(String tabla, T defaults) {
        log.warn("Falta la fila de {}: se usan los valores de fábrica", tabla);
        return defaults;
    }

    private static String vacioANull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private User exigirAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
        if (u.getRol() == null || u.getRol().getNombre() != RolNombre.ROLE_ADMIN) {
            throw new AccessDeniedException("Solo un administrador puede modificar la configuración");
        }
        return u;
    }
}
