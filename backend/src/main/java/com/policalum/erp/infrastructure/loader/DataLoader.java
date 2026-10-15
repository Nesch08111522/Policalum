package com.policalum.erp.infrastructure.loader;

import com.policalum.erp.domain.enums.RolNombre;
import com.policalum.erp.domain.model.Rol;
import com.policalum.erp.domain.model.Sede;
import com.policalum.erp.domain.model.User;
import com.policalum.erp.infrastructure.repository.RolRepository;
import com.policalum.erp.infrastructure.repository.SedeRepository;
import com.policalum.erp.infrastructure.repository.UserRepository;
import com.policalum.erp.application.service.TotpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RolRepository rolRepository;
    private final SedeRepository sedeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totpService;  // ← INYECTAR TotpService

    // ID de la sede existente
    private static final UUID SEDE_ID = UUID.fromString("2c0d98ec-1680-4662-a9af-e66d23e44345");

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🔄 Iniciando carga de datos iniciales...");

        // 1. Crear roles si no existen
        crearRoles();

        // 2. Obtener la sede existente
        Sede sede = obtenerSedeExistente();

        // 3. Crear usuarios por defecto
        crearUsuarios(sede);

        log.info("✅ Carga de datos iniciales completada.");
    }

    private void crearRoles() {
        crearRolSiNoExiste(RolNombre.ROLE_ADMIN, crearPermisosAdmin());
        crearRolSiNoExiste(RolNombre.ROLE_MULTIFUNCIONAL, crearPermisosMultifuncional());
    }

    private void crearRolSiNoExiste(RolNombre nombre, Map<String, Object> permisosJson) {
        boolean existe = rolRepository.findAll().stream()
                .anyMatch(rol -> rol.getNombre() == nombre);

        if (!existe) {
            Rol rol = Rol.builder()
                    .nombre(nombre)
                    .permisosJson(permisosJson)
                    .build();
            rolRepository.save(rol);
            log.info("✅ Rol creado: {}", nombre);
        } else {
            log.debug("ℹ️ Rol ya existe: {}", nombre);
        }
    }

    private Map<String, Object> crearPermisosAdmin() {
        Map<String, Object> permisos = new HashMap<>();

        Map<String, Boolean> sedesPermisos = new HashMap<>();
        sedesPermisos.put("crear", true);
        permisos.put("sedes", sedesPermisos);

        Map<String, Boolean> finanzasPermisos = new HashMap<>();
        finanzasPermisos.put("ver_reportes", true);
        finanzasPermisos.put("aprobar_pagos", true);
        permisos.put("finanzas", finanzasPermisos);

        Map<String, Boolean> usuariosPermisos = new HashMap<>();
        usuariosPermisos.put("leer", true);
        usuariosPermisos.put("crear", true);
        usuariosPermisos.put("eliminar", false);
        permisos.put("usuarios", usuariosPermisos);

        return permisos;
    }

    private Map<String, Object> crearPermisosMultifuncional() {
        Map<String, Object> permisos = new HashMap<>();

        Map<String, Boolean> asistenciaPermisos = new HashMap<>();
        asistenciaPermisos.put("marcar", true);
        asistenciaPermisos.put("ver_mis_marcaciones", true);
        permisos.put("asistencia", asistenciaPermisos);

        return permisos;
    }

    private Sede obtenerSedeExistente() {
        Sede sede = sedeRepository.findById(SEDE_ID)
                .orElseThrow(() -> new RuntimeException(
                        "❌ Sede no encontrada con ID: " + SEDE_ID
                ));

        log.info("✅ Sede encontrada: {} - {}", sede.getId(), sede.getNombre());
        return sede;
    }

    private void crearUsuarios(Sede sede) {
        // Obtener roles
        Rol adminRol = rolRepository.findAll().stream()
                .filter(r -> r.getNombre() == RolNombre.ROLE_ADMIN)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));

        Rol multifuncionalRol = rolRepository.findAll().stream()
                .filter(r -> r.getNombre() == RolNombre.ROLE_MULTIFUNCIONAL)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rol MULTIFUNCIONAL no encontrado"));

        // 1. Crear usuario ADMIN
        crearUsuarioSiNoExiste(
                "admin@policalum.com",
                "Admin",
                "Sistema",
                "admin",
                "admin123",
                "+51 987654321",
                adminRol,
                sede,
                true
        );

        // 2. Crear usuario MULTIFUNCIONAL (empleado)
        crearUsuarioSiNoExiste(
                "empleado@policalum.com",
                "Empleado",
                "Demo",
                "empleado",
                "empleado123",
                "+51 987654322",
                multifuncionalRol,
                sede,
                true
        );
    }

    private void crearUsuarioSiNoExiste(
            String email,
            String nombre,
            String apellido,
            String nombreUsuario,
            String passwordRaw,
            String celular,
            Rol rol,
            Sede sede,
            boolean activo) {

        // Verificar si el usuario ya existe por email
        if (userRepository.findByEmail(email).isPresent()) {
            log.debug("ℹ️ Usuario ya existe: {}", email);
            return;
        }

        // Verificar si el usuario ya existe por nombre de usuario
        boolean existeNombreUsuario = userRepository.findAll().stream()
                .anyMatch(u -> nombreUsuario.equals(u.getNombreUsuario()));

        if (existeNombreUsuario) {
            log.debug("ℹ️ Usuario ya existe: {}", nombreUsuario);
            return;
        }

        // ✅ GENERAR SECRETO TOTP
        String totpSecret = totpService.generateSecret();
        log.info("🔑 Secreto TOTP generado para {}: {}", email, totpSecret);

        // Crear nuevo usuario con TOTP configurado
        User user = User.builder()
                .email(email)
                .nombre(nombre)
                .apellido(apellido)
                .nombreUsuario(nombreUsuario)
                .passwordHash(passwordEncoder.encode(passwordRaw))
                .celular(celular)
                .rol(rol)
                .sede(sede)
                .activo(activo)
                .totpEnabled(true)          // ✅ HABILITAR TOTP
                .totpSecret(totpSecret)     // ✅ ASIGNAR SECRETO
                .build();

        userRepository.save(user);
        log.info("✅ Usuario creado: {} - {} (Rol: {}) - TOTP configurado: ✅",
                email, nombreUsuario, rol.getNombre());
    }
}