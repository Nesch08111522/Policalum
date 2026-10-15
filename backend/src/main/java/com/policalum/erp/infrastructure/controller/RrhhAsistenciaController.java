package com.policalum.erp.infrastructure.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.policalum.erp.application.dto.request.AsistenciaQrDTO;
import com.policalum.erp.application.dto.request.RrhhAsistenciaRequestDTO;
import com.policalum.erp.application.dto.response.*;
import com.policalum.erp.application.service.RrhhAsistenciaService;
import com.policalum.erp.application.service.TotpService;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhAsistenciaRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "RRHH")
@RestController
@RequestMapping("/api/v1/rrhh/asistencias")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RrhhAsistenciaController {

    private final RrhhAsistenciaService asistenciaService;
    private final TotpService totpService;
    private final UserRepository userRepository;
    private final RrhhAsistenciaRepository asistenciaRepository;

    // ============================================
    // QR PÚBLICO (ADMIN)
    // ============================================
    @Value("${totp.secret-key-publico}")
    private String secretKeyPublico;
    @GetMapping("/qr-publico")
    public ResponseEntity<QrPublicoDTO> generarQrPublico() {



        long window = totpService.currentWindow();
        String codigo = totpService.code(secretKeyPublico, window);
        String payload = "POLICALUM|PUBLICO|" + window + "|" + codigo;


            long segundosRestantes = totpService.secondsRemaining();
            long expiraEn = Instant.now().getEpochSecond() + segundosRestantes;

            QrPublicoDTO response = new QrPublicoDTO(
                    payload,
                    "Sistema POLICALUM",
                    segundosRestantes,
                    expiraEn
            );



        return ResponseEntity.ok(response);
    }
    // ============================================
    // CONFIGURACIÓN TOTP
    // ============================================
    @GetMapping("/qr-config/{empleadoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QrPublicoDTO> generarQrConfiguracion(@PathVariable UUID empleadoId) {
        User empleado = userRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        String secret = empleado.getTotpSecret();
        if (secret == null) {
            secret = totpService.generateSecret();
            empleado.setTotpSecret(secret);
            empleado.setTotpEnabled(true);
            userRepository.save(empleado);
        }

        long window = totpService.currentWindow();
        String codigo = totpService.code(secret, window);
        String payload = "POLICALUM|CONFIG|" + window + "|" + codigo;

        long segundosRestantes = totpService.secondsRemaining();
        long expiraEn = Instant.now().getEpochSecond() + segundosRestantes;

        QrPublicoDTO response = new QrPublicoDTO(
                payload,
                empleado.getNombre() + " " + empleado.getApellido(),
                segundosRestantes,
                expiraEn
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/configurar-totp/{empleadoId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TotpConfigDTO> configurarTOTP(@PathVariable UUID empleadoId) {
        User empleado = userRepository.findById(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        String secret = totpService.generateSecret();
        empleado.setTotpSecret(secret);
        empleado.setTotpEnabled(true);
        userRepository.save(empleado);

        String otpAuthUrl = String.format(
                "otpauth://totp/POLICALUM:%s?secret=%s&issuer=POLICALUM&period=60",
                empleado.getEmail(), secret
        );

        return ResponseEntity.ok(new TotpConfigDTO(
                empleadoId,
                secret,
                otpAuthUrl,
                "Escanea este QR con Google Authenticator"
        ));
    }

    // ============================================
    // MARCACIÓN POR QR (MULTIFUNCIONAL)
    // ============================================
    @PostMapping("/marcar-qr")
    public ResponseEntity<RrhhAsistenciaResponseDTO> marcarPorQr(@Valid @RequestBody AsistenciaQrDTO qrRequest) {
        RrhhAsistenciaResponseDTO response = asistenciaService.procesarMarcacionQr(qrRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================
    // MARCACIÓN MANUAL (SOLO ADMIN)
    // ============================================
    @PostMapping("/manual")
    public ResponseEntity<RrhhAsistenciaResponseDTO> registrarManual(
            @Valid @RequestBody RrhhAsistenciaRequestDTO manualRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!user.getRol().getNombre().name().equals("ROLE_ADMIN")) {
            throw new SecurityException("Solo el administrador puede registrar asistencias manualmente");
        }

        RrhhAsistenciaResponseDTO response = asistenciaService.procesarMarcacionManual(manualRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================
    // LISTAR CON FILTROS (ADMIN/MULTI)
    // ============================================
    @GetMapping("/listar")
    public ResponseEntity<Page<RrhhAsistenciaResponseDTO>> listarAsistencias(
            @RequestParam(required = false) UUID empleadoId,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Si es MULTIFUNCIONAL, forzar su propio ID
        if (user.getRol().getNombre().name().equals("ROLE_MULTIFUNCIONAL")) {
            empleadoId = user.getId();
        }

        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        Page<RrhhAsistenciaResponseDTO> response = asistenciaService.listarAsistencias(
                empleadoId, inicio, fin, pageable);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // MIS ASISTENCIAS (MULTIFUNCIONAL)
    // ============================================
    @GetMapping("/mis-asistencias")
    public ResponseEntity<Page<RrhhAsistenciaResponseDTO>> misAsistencias(
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @PageableDefault(size = 20, sort = "fechaHora", direction = Sort.Direction.DESC) Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        LocalDateTime inicio = fechaInicio != null ? fechaInicio.atStartOfDay() : null;
        LocalDateTime fin = fechaFin != null ? fechaFin.atTime(23, 59, 59) : null;

        Page<RrhhAsistenciaResponseDTO> response = asistenciaService.misAsistencias(
                user.getId(), inicio, fin, pageable);

        return ResponseEntity.ok(response);
    }

    // ============================================
    // BUSCAR POR ID
    // ============================================
    @GetMapping("/{id}")
    public ResponseEntity<RrhhAsistenciaResponseDTO> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(asistenciaService.buscarPorId(id));
    }

    // ============================================
    // ACTUALIZAR (SOLO ADMIN)
    // ============================================
    @PutMapping("/{id}")
    public ResponseEntity<RrhhAsistenciaResponseDTO> actualizarManual(
            @PathVariable UUID id,
            @Valid @RequestBody RrhhAsistenciaRequestDTO updateRequest) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!user.getRol().getNombre().name().equals("ROLE_ADMIN")) {
            throw new SecurityException("Solo el administrador puede actualizar asistencias");
        }

        RrhhAsistenciaResponseDTO response = asistenciaService.actualizarMarcacionManual(id, updateRequest);
        return ResponseEntity.ok(response);
    }

    // ============================================
    //  ELIMINAR (SOLO ADMIN)
    // ============================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarMarcacion(@PathVariable UUID id) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!user.getRol().getNombre().name().equals("ROLE_ADMIN")) {
            throw new SecurityException("Solo el administrador puede eliminar asistencias");
        }

        asistenciaService.eliminarMarcacion(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // DASHBOARD
    // ============================================
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardAsistenciaDTO> obtenerDashboard() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UUID empleadoId = null;
        if (user.getRol().getNombre().name().equals("ROLE_MULTIFUNCIONAL")) {
            empleadoId = user.getId();
        }

        DashboardAsistenciaDTO dashboard = asistenciaService.obtenerDashboard(empleadoId);
        return ResponseEntity.ok(dashboard);
    }

    // ============================================
    // ÚLTIMA MARCACIÓN
    // ============================================
    @GetMapping("/ultima-marcacion")
    public ResponseEntity<RrhhAsistenciaResponseDTO> obtenerUltimaMarcacion() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        RrhhAsistenciaResponseDTO response = asistenciaService.obtenerUltimaMarcacion(user.getId());
        return ResponseEntity.ok(response);
    }

    // ============================================
    //  ESTADO DEL DÍA
    // ============================================
    @GetMapping("/estado-dia")
    public ResponseEntity<AsistenciaEstadoDTO> obtenerEstadoDelDia() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        AsistenciaEstadoDTO response = asistenciaService.obtenerEstadoDelDia(user.getId());
        return ResponseEntity.ok(response);
    }

    // ============================================
// ✅ MARCACIÓN POR QR REAL (CON AUTENTICACIÓN)
// ============================================
    @PostMapping("/marcar-qr-real")
    public ResponseEntity<RrhhAsistenciaResponseDTO> marcarPorQrReal(
            @Valid @RequestBody AsistenciaQrDTO qrRequest) {

        // 1. Obtener el empleado autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User empleado = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado no encontrado"));

        // 2. Verificar que el ID del empleado coincida con el autenticado
        if (!empleado.getId().equals(qrRequest.empleadoId())) {
            throw new SecurityException("ID de empleado no coincide con el usuario autenticado");
        }

        // 3. Procesar la marcación (usa el mismo servicio que /marcar-qr)
        RrhhAsistenciaResponseDTO response = asistenciaService.procesarMarcacionQr(qrRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/empleados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserInfoDTO>> obtenerEmpleados() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!admin.getRol().getNombre().name().equals("ROLE_ADMIN")) {
            throw new SecurityException("Solo el administrador puede ver la lista de empleados");
        }

        List<User> usuarios = userRepository.findAll();

        List<UserInfoDTO> response = usuarios.stream()
                .filter(User::getActivo)
                .map(user -> new UserInfoDTO(
                        user.getId(),
                        user.getNombre(),
                        user.getApellido(),
                        user.getEmail(),
                        user.getNombre() + " " + user.getApellido()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}