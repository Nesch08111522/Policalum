package com.policalum.erp.application.service.serviceImpl;

import com.policalum.erp.application.dto.request.PerfilActualizarRequestDTO;
import com.policalum.erp.application.dto.request.PerfilContrasenaRequestDTO;
import com.policalum.erp.application.dto.response.PerfilResponseDTO;
import com.policalum.erp.application.service.FileStorageService;
import com.policalum.erp.application.service.TotpService;
import com.policalum.erp.domain.enums.SistemaPension;
import com.policalum.erp.domain.model.RrhhLegajoDigital;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.exception.FileStorageException;
import com.policalum.erp.exception.ResourceNotFoundException;
import com.policalum.erp.infrastructure.repository.RrhhLegajoDigitalRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.infrastructure.security.JwtUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PerfilServiceImpl {

    private static final long FOTO_MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> FOTO_EXTENSIONES = Set.of("jpg", "jpeg", "png", "webp");

    private final UserRepository userRepository;
    private final SedeRepository sedeRepository;
    private final RrhhLegajoDigitalRepository legajoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TotpService totpService;
    private final FileStorageService fileStorageService;

    public PerfilServiceImpl(UserRepository userRepository, SedeRepository sedeRepository,
                             RrhhLegajoDigitalRepository legajoRepository, PasswordEncoder passwordEncoder,
                             JwtUtil jwtUtil, TotpService totpService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.sedeRepository = sedeRepository;
        this.legajoRepository = legajoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.totpService = totpService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public PerfilResponseDTO actual() {
        return respuesta(usuario(), null, null, null);
    }

    @Transactional
    public PerfilResponseDTO actualizar(PerfilActualizarRequestDTO request) {
        User user = usuario();
        String email = request.email().trim();
        userRepository.findByEmail(email)
                .filter(otro -> !otro.getId().equals(user.getId()))
                .ifPresent(otro -> { throw new IllegalArgumentException("Ese correo ya está registrado."); });

        user.setNombre(request.nombre().trim());
        user.setApellido(request.apellido().trim());
        user.setEmail(email);
        user.setCelular(request.celular().trim());
        user.setDocumento(vacio(request.documento()));
        user.setBiografia(vacio(request.biografia()));
        aplicarFoto(user, request.foto());
        aplicarSede(user, request.sedeId());
        aplicarLaboral(user, request.sueldoBase(), request.diaCobro());
        userRepository.save(user);
        String token = email.equalsIgnoreCase(correoActual()) ? null : jwtUtil.generateToken(user);
        return respuesta(user, token, null, null);
    }

    /** Guarda la foto en disco y deja solo la ruta en la columna foto. Reemplaza el archivo anterior. */
    @Transactional
    public PerfilResponseDTO guardarFoto(MultipartFile archivo) {
        User user = usuario();
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Selecciona una foto");
        }
        if (archivo.getSize() > FOTO_MAX_BYTES) {
            throw new IllegalArgumentException("La foto no puede superar 2 MB");
        }
        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
        nombre = nombre.replace('\\', '/');
        int barra = nombre.lastIndexOf('/');
        if (barra >= 0) nombre = nombre.substring(barra + 1);
        int punto = nombre.lastIndexOf('.');
        String extension = punto > 0 ? nombre.substring(punto + 1).toLowerCase(Locale.ROOT) : "";
        if (!FOTO_EXTENSIONES.contains(extension)) {
            throw new IllegalArgumentException("La foto debe ser JPG, PNG o WEBP");
        }
        String tipo = archivo.getContentType() != null ? archivo.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!tipo.isBlank() && !tipo.startsWith("image/")) {
            throw new FileStorageException("El archivo no es una imagen");
        }
        String anterior = user.getFoto();
        String ruta = fileStorageService.guardarFijo(archivo, "usuarios/" + user.getId() + "/foto");
        user.setFoto(ruta);
        userRepository.save(user);
        fileStorageService.descartarVariantes(ruta);
        if (anterior != null && !anterior.equals(ruta) && anterior.startsWith("usuarios/") && !anterior.contains("..")) {
            fileStorageService.eliminar(anterior);
        }
        return respuesta(user, null, null, null);
    }

    @Transactional(readOnly = true)
    public Resource cargarFoto() {
        User user = usuario();
        String foto = user.getFoto();
        if (foto == null || foto.isBlank() || foto.contains("..") || !foto.startsWith("usuarios/")) {
            throw new ResourceNotFoundException("No hay una foto guardada en el servidor");
        }
        return fileStorageService.cargar(foto);
    }

    @Transactional
    public PerfilResponseDTO cambiarContrasena(PerfilContrasenaRequestDTO request) {
        User user = usuario();
        if (!request.nueva().equals(request.confirmacion())) {
            throw new IllegalArgumentException("La confirmación no coincide con la nueva contraseña.");
        }
        if (!passwordEncoder.matches(request.actual(), user.getPasswordHash())) {
            throw new IllegalArgumentException("La contraseña actual no coincide.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.nueva()));
        user.setSesionVersion(version(user) + 1);
        userRepository.save(user);
        return respuesta(user, jwtUtil.generateToken(user), null, null);
    }

    @Transactional
    public PerfilResponseDTO cambiarTotp(boolean activo) {
        User user = usuario();
        String secreto = null;
        String url = null;
        if (activo && (user.getTotpSecret() == null || user.getTotpSecret().isBlank())) {
            secreto = totpService.generateSecret();
            user.setTotpSecret(secreto);
            url = "otpauth://totp/POLICALUM:" + user.getEmail() + "?secret=" + secreto + "&issuer=POLICALUM&period=60";
        }
        user.setTotpEnabled(activo);
        userRepository.save(user);
        return respuesta(user, null, secreto, url);
    }

    @Transactional
    public PerfilResponseDTO cerrarOtrasSesiones() {
        User user = usuario();
        user.setSesionVersion(version(user) + 1);
        userRepository.save(user);
        return respuesta(user, jwtUtil.generateToken(user), null, null);
    }

    /** null conserva la foto. Vacío la quita. Un data URL ya no se guarda en la base. */
    private void aplicarFoto(User user, String foto) {
        if (foto == null) return;
        if (foto.isBlank()) {
            String anterior = user.getFoto();
            user.setFoto(null);
            if (anterior != null && anterior.startsWith("usuarios/") && !anterior.contains("..")) {
                fileStorageService.eliminar(anterior);
            }
            return;
        }
        throw new IllegalArgumentException("La foto se sube como archivo. El perfil solo guarda la ruta.");
    }

    private void aplicarSede(User user, java.util.UUID sedeId) {
        if (sedeId == null || sedeId.equals(user.getSede().getId())) return;
        if (!esAdmin()) throw new IllegalArgumentException("Solo un administrador puede cambiar la sede asignada.");
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new IllegalArgumentException("La sede seleccionada no existe."));
        user.setSede(sede);
    }

    private void aplicarLaboral(User user, BigDecimal sueldo, Integer diaCobro) {
        if (sueldo == null && diaCobro == null) return;
        if (!esAdmin()) throw new IllegalArgumentException("Solo un administrador puede editar el sueldo y el día de cobro.");
        RrhhLegajoDigital legajo = legajoRepository.findDetalleByUserId(user.getId()).orElse(null);
        if (legajo == null) {
            if (sueldo == null) throw new IllegalArgumentException("Registra el sueldo base para abrir el legajo.");
            legajo = RrhhLegajoDigital.builder()
                    .user(user)
                    .tipoContrato("PLANILLA")
                    .fechaIngreso(LocalDate.now())
                    .sueldoBase(sueldo)
                    .diaCobro(diaCobro)
                    .porcentajeComision(BigDecimal.ZERO)
                    .sistemaPension(SistemaPension.NINGUNO)
                    .activo(true)
                    .build();
        } else {
            if (sueldo != null) legajo.setSueldoBase(sueldo);
            if (diaCobro != null) legajo.setDiaCobro(diaCobro);
        }
        legajoRepository.save(legajo);
    }

    private PerfilResponseDTO respuesta(User user, String token, String secreto, String url) {
        RrhhLegajoDigital legajo = legajoRepository.findDetalleByUserId(user.getId()).orElse(null);
        return new PerfilResponseDTO(
                user.getId(),
                user.getNombre(),
                user.getApellido(),
                user.getNombreUsuario(),
                user.getEmail(),
                user.getCelular(),
                user.getDocumento(),
                user.getBiografia(),
                user.getFoto(),
                user.getSede() != null ? user.getSede().getId() : null,
                user.getSede() != null ? user.getSede().getNombre() : null,
                user.getRol() != null && user.getRol().getNombre() != null ? user.getRol().getNombre().name() : null,
                user.getActivo(),
                Boolean.TRUE.equals(user.getTotpEnabled()),
                user.getUltimoLogin(),
                user.getUltimoIp(),
                esAdmin(),
                legajo != null ? legajo.getId() : null,
                legajo != null ? legajo.getSueldoBase() : null,
                legajo != null ? legajo.getDiaCobro() : null,
                legajo != null ? legajo.getTipoContrato() : null,
                legajo != null ? legajo.getFechaIngreso() : null,
                legajo != null && legajo.getSistemaPension() != null ? legajo.getSistemaPension().name() : null,
                token,
                secreto,
                url
        );
    }

    private User usuario() {
        return userRepository.findPerfilByEmail(correoActual())
                .orElseThrow(() -> new IllegalStateException("Usuario autenticado no válido"));
    }

    private static String correoActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new IllegalStateException("Usuario autenticado no válido");
        }
        return auth.getName();
    }

    private static boolean esAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private static int version(User user) {
        return user.getSesionVersion() == null ? 0 : user.getSesionVersion();
    }

    private static String vacio(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }
}
