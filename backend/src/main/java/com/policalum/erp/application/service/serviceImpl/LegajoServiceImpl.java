package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.LegajoRequestDTO;
import com.policalum.erp.application.dto.response.ArchivoDescargaDTO;
import com.policalum.erp.application.dto.response.LegajoDashboardDTO;
import com.policalum.erp.application.dto.response.LegajoDocumentoResponseDTO;
import com.policalum.erp.application.dto.response.LegajoListadoDTO;
import com.policalum.erp.application.dto.response.LegajoResponseDTO;
import com.policalum.erp.application.mapper.LegajoMapper;
import com.policalum.erp.application.service.AuditService;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.LegajoService;
import com.policalum.erp.domain.enums.AccionAudit;
import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.enums.SistemaPension;
import com.policalum.erp.domain.enums.TipoDocumentoLegajo;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.RrhhLegajoDocumento;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDigitalRepository;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDocumentoRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LegajoServiceImpl implements LegajoService {

    private static final String TABLA = "rrhh_legajo_digital";
    private static final String TABLA_DOCS = "rrhh_legajo_documentos";
    private static final String SUB_CARPETA = "legajos";
    static final BigDecimal TASA_ONP_DEFAULT = new BigDecimal("13.00");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final RrhhLegajoDigitalRepository legajoRepository;
    private final RrhhLegajoDocumentoRepository documentoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final LegajoMapper mapper;
    private final AuditService auditService;

    // ============ LEGAJO ============

    @Override
    public LegajoResponseDTO crear(LegajoRequestDTO request) {
        exigirAdmin(usuarioActual());
        validarDatos(request);

        User empleado = userRepository.findById(request.empleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado: " + request.empleadoId()));
        if (!Boolean.TRUE.equals(empleado.getActivo())) {
            throw new IllegalStateException("El empleado " + nombre(empleado) + " está inactivo");
        }
        if (legajoRepository.existsByUser_Id(empleado.getId())) {
            throw new IllegalStateException("El empleado " + nombre(empleado) + " ya tiene legajo");
        }

        RrhhLegajoDigital legajo = mapper.toEntity(request);
        legajo.setUser(empleado);
        aplicarCamposNormalizados(legajo, request);
        legajo.setActivo(request.activo() == null ? Boolean.TRUE : request.activo());

        RrhhLegajoDigital guardado = legajoRepository.save(legajo);
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.INSERT, snapshot(guardado));
        log.info("Legajo creado: {} - empleado {}", guardado.getId(), empleado.getEmail());
        return mapper.toResponseDto(guardado, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public LegajoResponseDTO obtenerPorEmpleado(UUID empleadoId) {
        exigirAdmin(usuarioActual());
        return respuestaCompleta(legajoDe(empleadoId));
    }

    @Override
    @Transactional(readOnly = true)
    public LegajoResponseDTO obtenerMio() {
        User actual = usuarioActual();
        RrhhLegajoDigital legajo = legajoRepository.findDetalleByUserId(actual.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usted no tiene legajo registrado"));
        return respuestaCompleta(legajo);
    }

    @Override
    public LegajoResponseDTO actualizar(UUID empleadoId, LegajoRequestDTO request) {
        exigirAdmin(usuarioActual());
        if (!empleadoId.equals(request.empleadoId())) {
            throw new IllegalArgumentException("El empleadoId del cuerpo no coincide con el de la URL");
        }
        validarDatos(request);

        RrhhLegajoDigital legajo = legajoDe(empleadoId);
        Map<String, Object> antes = snapshot(legajo);

        mapper.updateEntityFromDto(request, legajo);
        aplicarCamposNormalizados(legajo, request);
        if (request.activo() != null) legajo.setActivo(request.activo());

        RrhhLegajoDigital guardado = legajoRepository.save(legajo);
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("antes", antes);
        datos.put("despues", snapshot(guardado));
        auditService.registrar(TABLA, guardado.getId(), AccionAudit.UPDATE, datos);
        log.info("Legajo actualizado: {} - empleado {}", guardado.getId(), empleadoId);
        return respuestaCompleta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LegajoListadoDTO> buscar(UUID sedeId, String tipoContrato, SistemaPension sistemaPension,
                                         Boolean activo, String texto, Pageable pageable) {
        exigirAdmin(usuarioActual());
        String tipo = tipoContrato == null || tipoContrato.isBlank() ? null : tipoContrato.trim().toUpperCase(Locale.ROOT);
        String patron = texto == null || texto.isBlank() ? "%" : "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
        return legajoRepository.search(sedeId, tipo, sistemaPension, activo, patron, pageable)
                .map(mapper::toListadoDto);
    }

    // ============ DOCUMENTOS ============

    @Override
    public LegajoDocumentoResponseDTO subirDocumento(UUID legajoId, MultipartFile archivo, String tipo, String descripcion) {
        User admin = usuarioActual();
        exigirAdmin(admin);
        TipoDocumentoLegajo tipoDoc = parseTipo(tipo);
        if (descripcion != null && descripcion.length() > 255) {
            throw new IllegalArgumentException("La descripción no puede superar 255 caracteres");
        }
        RrhhLegajoDigital legajo = legajoRepository.findById(legajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Legajo no encontrado: " + legajoId));

        // Valida extensión/tamaño y guarda en disco. Si luego la transacción falla, se borra.
        String ruta = fileStorageService.guardar(archivo, SUB_CARPETA);
        borrarArchivoSiHayRollback(ruta);

        RrhhLegajoDocumento documento = RrhhLegajoDocumento.builder()
                .legajo(legajo)
                .tipo(tipoDoc)
                .nombreOriginal(nombreArchivo(archivo))
                .rutaArchivo(ruta)
                .contentType(archivo.getContentType())
                .tamanoBytes(archivo.getSize())
                .descripcion(descripcion == null || descripcion.isBlank() ? null : descripcion.trim())
                .subidoPor(admin)
                .build();
        RrhhLegajoDocumento guardado = documentoRepository.save(documento);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("legajoId", legajoId.toString());
        datos.put("tipo", tipoDoc.name());
        datos.put("nombreOriginal", guardado.getNombreOriginal());
        auditService.registrar(TABLA_DOCS, guardado.getId(), AccionAudit.INSERT, datos);
        log.info("Documento {} subido al legajo {}: {}", tipoDoc, legajoId, guardado.getNombreOriginal());
        return mapper.toDocumentoDto(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LegajoDocumentoResponseDTO> listarDocumentos(UUID legajoId) {
        RrhhLegajoDigital legajo = legajoRepository.findById(legajoId)
                .orElseThrow(() -> new ResourceNotFoundException("Legajo no encontrado: " + legajoId));
        exigirAdminODueno(usuarioActual(), legajo);
        return documentoRepository.findByLegajo_IdOrderByCreatedAtDesc(legajoId).stream()
                .map(mapper::toDocumentoDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoDescargaDTO descargarDocumento(UUID legajoId, UUID documentoId) {
        User actual = usuarioActual();
        RrhhLegajoDocumento documento = documentoDe(legajoId, documentoId);
        exigirAdminODueno(actual, documento.getLegajo());

        Resource resource = fileStorageService.cargar(documento.getRutaArchivo());

        // Documentos sensibles (DNI, antecedentes): toda descarga queda registrada
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("operacion", "DESCARGA");
        datos.put("legajoId", legajoId.toString());
        datos.put("tipo", documento.getTipo().name());
        auditService.registrar(TABLA_DOCS, documento.getId(), AccionAudit.ACCESS, datos);

        return new ArchivoDescargaDTO(resource, documento.getNombreOriginal(), documento.getContentType());
    }

    @Override
    public void eliminarDocumento(UUID legajoId, UUID documentoId) {
        exigirAdmin(usuarioActual());
        RrhhLegajoDocumento documento = documentoDe(legajoId, documentoId);
        String ruta = documento.getRutaArchivo();

        documentoRepository.delete(documento);
        borrarArchivoTrasCommit(ruta);

        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("legajoId", legajoId.toString());
        datos.put("tipo", documento.getTipo().name());
        datos.put("nombreOriginal", documento.getNombreOriginal());
        auditService.registrar(TABLA_DOCS, documentoId, AccionAudit.DELETE, datos);
        log.info("Documento {} eliminado del legajo {}", documentoId, legajoId);
    }

    // ============ DASHBOARD ============

    @Override
    @Transactional(readOnly = true)
    public LegajoDashboardDTO dashboard(UUID sedeId) {
        exigirAdmin(usuarioActual());
        List<RrhhLegajoDigital> todos = legajoRepository.findTodos(sedeId);
        List<RrhhLegajoDigital> activos = todos.stream().filter(l -> Boolean.TRUE.equals(l.getActivo())).toList();

        Map<String, Long> porContrato = new TreeMap<>();
        Map<String, Long> porPension = new TreeMap<>();
        BigDecimal planilla = BigDecimal.ZERO;
        long sinSistema = 0;
        for (RrhhLegajoDigital l : activos) {
            porContrato.merge(l.getTipoContrato() != null ? l.getTipoContrato() : "SIN_REGISTRAR", 1L, Long::sum);
            porPension.merge(l.getSistemaPension() != null ? l.getSistemaPension().name() : "SIN_REGISTRAR", 1L, Long::sum);
            if (l.getSistemaPension() == null) sinSistema++;
            planilla = planilla.add(l.getSueldoBase() != null ? l.getSueldoBase() : BigDecimal.ZERO);
        }
        BigDecimal promedio = activos.isEmpty() ? BigDecimal.ZERO.setScale(2)
                : planilla.divide(BigDecimal.valueOf(activos.size()), 2, RoundingMode.HALF_UP);

        Set<UUID> conContrato = new HashSet<>(documentoRepository.findLegajoIdsConTipo(TipoDocumentoLegajo.CONTRATO));
        Set<UUID> conDni = new HashSet<>(documentoRepository.findLegajoIdsConTipo(TipoDocumentoLegajo.DNI));
        long sinContrato = activos.stream().filter(l -> !conContrato.contains(l.getId())).count();
        long sinDni = activos.stream().filter(l -> !conDni.contains(l.getId())).count();

        return new LegajoDashboardDTO(sedeId, todos.size(), activos.size(), todos.size() - activos.size(),
                porContrato, porPension, planilla.setScale(2, RoundingMode.HALF_UP), promedio,
                sinContrato, sinDni, sinSistema,
                legajoRepository.contarUsuariosActivosSinLegajo(sedeId),
                documentoRepository.contarPorSede(sedeId));
    }

    // ============ VALIDACIONES ============

    /**
     * Reglas de negocio (además de las anotaciones Jakarta del DTO):
     * sueldo > 0, comisión 0-100, fecha no futura, y coherencia sistema/tasa de pensión.
     */
    void validarDatos(LegajoRequestDTO r) {
        if (r.sueldoBase() == null || r.sueldoBase().signum() <= 0) {
            throw new IllegalArgumentException("El sueldo base debe ser mayor a cero");
        }
        if (r.porcentajeComision() != null
                && (r.porcentajeComision().signum() < 0 || r.porcentajeComision().compareTo(CIEN) > 0)) {
            throw new IllegalArgumentException("El porcentaje de comisión debe estar entre 0 y 100");
        }
        if (r.fechaIngreso() == null || r.fechaIngreso().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de ingreso es obligatoria y no puede ser futura");
        }
        normalizarTipoContrato(r.tipoContrato());
        resolverTasaPension(r.sistemaPension(), r.tasaPension());
    }

    /** Devuelve la tasa a guardar según el sistema, o lanza IllegalArgumentException si es incoherente. */
    static BigDecimal resolverTasaPension(SistemaPension sistema, BigDecimal tasa) {
        if (sistema == null) {
            throw new IllegalArgumentException("El sistema de pensión es obligatorio (ONP, AFP o NINGUNO)");
        }
        if (tasa != null && (tasa.signum() < 0 || tasa.compareTo(CIEN) > 0)) {
            throw new IllegalArgumentException("La tasa de pensión debe estar entre 0 y 100");
        }
        return switch (sistema) {
            case ONP -> tasa == null || tasa.signum() == 0 ? TASA_ONP_DEFAULT : tasa;
            case AFP -> {
                if (tasa == null || tasa.signum() <= 0) {
                    throw new IllegalArgumentException("Para AFP la tasa de pensión es obligatoria y mayor a 0");
                }
                yield tasa;
            }
            case NINGUNO -> {
                if (tasa != null && tasa.signum() != 0) {
                    throw new IllegalArgumentException("Con sistema NINGUNO la tasa debe ser vacía o 0");
                }
                yield null;
            }
        };
    }

    private static String normalizarTipoContrato(String tipo) {
        String t = tipo == null ? "" : tipo.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("PLANILLA", "RECIBO_HONORARIOS", "CAS").contains(t)) {
            throw new IllegalArgumentException("Tipo de contrato inválido: PLANILLA, RECIBO_HONORARIOS o CAS");
        }
        return t;
    }

    private void aplicarCamposNormalizados(RrhhLegajoDigital legajo, LegajoRequestDTO r) {
        legajo.setTipoContrato(normalizarTipoContrato(r.tipoContrato()));
        legajo.setTasaPension(resolverTasaPension(r.sistemaPension(), r.tasaPension()));
        legajo.setPorcentajeComision(r.porcentajeComision() != null ? r.porcentajeComision() : BigDecimal.ZERO);
    }

    private TipoDocumentoLegajo parseTipo(String tipo) {
        try {
            return TipoDocumentoLegajo.valueOf(tipo.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Tipo de documento inválido: '" + tipo + "'. Permitidos: "
                    + List.of(TipoDocumentoLegajo.values()));
        }
    }

    // ============ HELPERS ============

    private RrhhLegajoDigital legajoDe(UUID empleadoId) {
        return legajoRepository.findDetalleByUserId(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("El empleado " + empleadoId + " no tiene legajo"));
    }

    private RrhhLegajoDocumento documentoDe(UUID legajoId, UUID documentoId) {
        return documentoRepository.findByIdAndLegajo_Id(documentoId, legajoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Documento " + documentoId + " no encontrado en el legajo " + legajoId));
    }

    private LegajoResponseDTO respuestaCompleta(RrhhLegajoDigital legajo) {
        List<LegajoDocumentoResponseDTO> docs = documentoRepository.findByLegajo_IdOrderByCreatedAtDesc(legajo.getId())
                .stream().map(mapper::toDocumentoDto).toList();
        return mapper.toResponseDto(legajo, docs);
    }

    private Map<String, Object> snapshot(RrhhLegajoDigital l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("empleadoId", l.getUser() != null ? l.getUser().getId().toString() : null);
        m.put("tipoContrato", l.getTipoContrato());
        m.put("fechaIngreso", l.getFechaIngreso() != null ? l.getFechaIngreso().toString() : null);
        m.put("sueldoBase", l.getSueldoBase());
        m.put("porcentajeComision", l.getPorcentajeComision());
        m.put("sistemaPension", l.getSistemaPension() != null ? l.getSistemaPension().name() : null);
        m.put("tasaPension", l.getTasaPension());
        m.put("activo", l.getActivo());
        return m;
    }

    private String nombreArchivo(MultipartFile archivo) {
        String n = archivo.getOriginalFilename();
        if (n == null || n.isBlank()) return "documento";
        // quita rutas que algunos navegadores envían (C:\fakepath\...)
        n = n.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.length() > 255 ? n.substring(n.length() - 255) : n;
    }

    private void borrarArchivoSiHayRollback(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) fileStorageService.eliminar(ruta);
            }
        });
    }

    private void borrarArchivoTrasCommit(String ruta) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorageService.eliminar(ruta);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileStorageService.eliminar(ruta);
            }
        });
    }

    private String nombre(User user) {
        return user.getNombre() + " " + user.getApellido();
    }

    private boolean esAdmin(User user) {
        return user.getRol() != null && user.getRol().getNombre() == RolNombre.ROLE_ADMIN;
    }

    private void exigirAdmin(User user) {
        if (!esAdmin(user)) {
            throw new AccessDeniedException("Solo un administrador puede gestionar legajos");
        }
    }

    private void exigirAdminODueno(User user, RrhhLegajoDigital legajo) {
        if (!esAdmin(user) && !legajo.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Solo puede acceder a los documentos de su propio legajo");
        }
    }

    private User usuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }
}
